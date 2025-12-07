package com.serverbackup.service;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.api.*;
import com.serverbackup.api.events.BackupCompleteEvent;
import com.serverbackup.api.events.BackupDeleteEvent;
import com.serverbackup.api.events.BackupFailEvent;
import com.serverbackup.api.events.BackupStartEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of BackupAPI
 * 
 * This class bridges the legacy BackupService with the new public API.
 */
public class BackupAPIImpl implements BackupAPI {
    
    private final ServerBackupPlugin plugin;
    private final BackupService backupService;
    private BackupResult lastBackup = null;
    
    public BackupAPIImpl(ServerBackupPlugin plugin, BackupService backupService) {
        this.plugin = plugin;
        this.backupService = backupService;
    }
    
    @NotNull
    @Override
    public CompletableFuture<BackupResult> createBackup(@NotNull BackupOptions options) {
        CompletableFuture<BackupResult> future = new CompletableFuture<>();
        
        // Check if events are enabled
        boolean eventsEnabled = plugin.getConfig().getBoolean("features.events.enabled", true);
        
        // Fire BackupStartEvent (if enabled and not cancelled)
        if (eventsEnabled && plugin.getConfig().getBoolean("features.events.fire-start-event", true)) {
            BackupStartEvent startEvent = new BackupStartEvent(options);
            Bukkit.getPluginManager().callEvent(startEvent);
            
            if (startEvent.isCancelled()) {
                String reason = startEvent.getCancelReason() != null ? 
                    startEvent.getCancelReason() : "Backup cancelled by another plugin";
                
                BackupResult result = BackupResult.builder()
                    .setSuccess(false)
                    .setType(options.getType())
                    .setError(new IllegalStateException(reason))
                    .build();
                    
                future.complete(result);
                plugin.getLogger().warning("Backup cancelled: " + reason);
                return future;
            }
        }
        
        // Execute backup
        if (options.isAsync()) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                executeBackup(options, future, eventsEnabled);
            });
        } else {
            executeBackup(options, future, eventsEnabled);
        }
        
        return future;
    }
    
    private void executeBackup(BackupOptions options, CompletableFuture<BackupResult> future, boolean eventsEnabled) {
        long startTime = System.currentTimeMillis();
        UUID sessionId = UUID.randomUUID();
        
        try {
            // Save worlds on main thread
            if (!options.getWorldNames().isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (String worldName : options.getWorldNames()) {
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            world.save();
                        }
                    }
                });
                // Wait a bit for save to complete
                Thread.sleep(1000);
            }
            
            // Create backup using legacy service
            String type = options.getType().getName();
            File backupFile = backupService.createBackupSync(options);
            
            if (backupFile == null || !backupFile.exists()) {
                throw new IllegalStateException("Backup file was not created");
            }
            
            // Build result
            BackupResult result = BackupResult.builder()
                .setSessionId(sessionId)
                .setSuccess(true)
                .setBackupFile(backupFile)
                .setType(options.getType())
                .setStartTime(startTime)
                .setEndTime(System.currentTimeMillis())
                .setFileSize(backupFile.length())
                .setMetadata(options.getMetadata())
                .build();
                
            lastBackup = result;
            future.complete(result);
            
            // Fire BackupCompleteEvent
            if (eventsEnabled && plugin.getConfig().getBoolean("features.events.fire-complete-event", true)) {
                Bukkit.getPluginManager().callEvent(new BackupCompleteEvent(result));
            }
            
        } catch (Exception e) {
            BackupResult result = BackupResult.builder()
                .setSessionId(sessionId)
                .setSuccess(false)
                .setType(options.getType())
                .setStartTime(startTime)
                .setEndTime(System.currentTimeMillis())
                .setError(e)
                .build();
                
            future.complete(result);
            
            // Fire BackupFailEvent
            if (eventsEnabled && plugin.getConfig().getBoolean("features.events.fire-fail-event", true)) {
                Bukkit.getPluginManager().callEvent(new BackupFailEvent(options, result));
            }
            
            plugin.getLogger().severe("Backup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @NotNull
    @Override
    public List<File> listBackups() {
        return backupService.listBackups();
    }
    
    @NotNull
    @Override
    public List<File> listBackups(@NotNull BackupFilter filter) {
        List<File> allBackups = listBackups();
        return allBackups.stream()
            .map(this::fileToBackupInfo)
            .filter(Objects::nonNull)
            .filter(filter::matches)
            .map(BackupInfo::getBackupFile)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteBackup(@NotNull String backupName) {
        File backupDir = getBackupDirectory();
        File backupFile = new File(backupDir, backupName);
        return deleteBackup(backupFile);
    }
    
    @Override
    public boolean deleteBackup(@NotNull File backupFile) {
        // Fire BackupDeleteEvent (if enabled)
        boolean eventsEnabled = plugin.getConfig().getBoolean("features.events.enabled", true);
        if (eventsEnabled && plugin.getConfig().getBoolean("features.events.fire-delete-event", true)) {
            BackupDeleteEvent deleteEvent = new BackupDeleteEvent(backupFile);
            Bukkit.getPluginManager().callEvent(deleteEvent);
            
            if (deleteEvent.isCancelled()) {
                String reason = deleteEvent.getCancelReason() != null ? 
                    deleteEvent.getCancelReason() : "Deletion cancelled by another plugin";
                plugin.getLogger().warning("Backup deletion cancelled: " + reason);
                return false;
            }
        }
        
        return backupService.deleteBackup(backupFile.getName());
    }
    
    @Nullable
    @Override
    public BackupInfo getBackupInfo(@NotNull String backupName) {
        File backupDir = getBackupDirectory();
        File backupFile = new File(backupDir, backupName);
        
        if (!backupFile.exists()) {
            return null;
        }
        
        return fileToBackupInfo(backupFile);
    }
    
    @Override
    public boolean isBackupInProgress() {
        return backupService.getSessionManager().isBackupRunning();
    }
    
    @Override
    public int getActiveBackupCount() {
        return backupService.getSessionManager().getActiveSessionCount();
    }
    
    @Nullable
    @Override
    public BackupResult getLastBackup() {
        return lastBackup;
    }
    
    @NotNull
    @Override
    public File getBackupDirectory() {
        String backupPath = plugin.getConfig().getString("backup.directory", "backups");
        return new File(plugin.getServer().getWorldContainer(), backupPath);
    }
    
    @Override
    public long getTotalBackupSize() {
        return listBackups().stream()
            .mapToLong(File::length)
            .sum();
    }
    
    @Override
    public int getBackupCount() {
        return listBackups().size();
    }
    
    @Override
    public int cleanOldBackups() {
        int maxBackups = plugin.getConfig().getInt("backup.max-backups", 10);
        if (maxBackups <= 0) {
            return 0;
        }
        
        List<File> backups = listBackups();
        int toDelete = backups.size() - maxBackups;
        
        if (toDelete <= 0) {
            return 0;
        }
        
        int deleted = 0;
        for (int i = maxBackups; i < backups.size(); i++) {
            if (deleteBackup(backups.get(i))) {
                deleted++;
            }
        }
        
        return deleted;
    }
    
    /**
     * Convert File to BackupInfo
     */
    @Nullable
    private BackupInfo fileToBackupInfo(File file) {
        if (!file.exists()) {
            return null;
        }
        
        // Parse backup type from filename or default to WORLD
        BackupType type = BackupType.WORLD;
        if (file.getName().contains("full")) {
            type = BackupType.FULL;
        }
        
        // For now, we don't have world list in filename
        // This could be enhanced later by storing metadata
        Set<String> worlds = new HashSet<>(plugin.getConfig().getStringList("backup.worlds"));
        if (worlds.isEmpty()) {
            for (World world : Bukkit.getWorlds()) {
                worlds.add(world.getName());
            }
        }
        
        return new BackupInfo(
            file,
            file.getName(),
            type,
            file.length(),
            Instant.ofEpochMilli(file.lastModified()),
            worlds,
            file.getName().endsWith(".zip")
        );
    }
}

package com.serverbackup.integrations.placeholderapi;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.api.BackupAPI;
import com.serverbackup.api.BackupResult;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * PlaceholderAPI expansion for ServerBackupPlugin
 * 
 * Available placeholders:
 * - %serverbackup_last_backup_time% - Last backup timestamp
 * - %serverbackup_last_backup_ago% - Time since last backup (human readable)
 * - %serverbackup_backup_count% - Total number of backups
 * - %serverbackup_backup_size% - Total size of all backups
 * - %serverbackup_backup_size_mb% - Total size in MB
 * - %serverbackup_is_running% - Is backup currently running (true/false)
 * - %serverbackup_is_running_colored% - Is backup running (✓/✗ colored)
 * - %serverbackup_next_backup_in% - Time until next auto backup
 * - %serverbackup_latest_backup_name% - Name of latest backup
 * - %serverbackup_latest_backup_size% - Size of latest backup
 * - %serverbackup_status% - Overall status indicator
 * 
 * Config:
 * integrations:
 *   placeholderapi:
 *     enabled: true
 */
public class ServerBackupExpansion extends PlaceholderExpansion {
    
    private final ServerBackupPlugin plugin;
    private final BackupAPI backupAPI;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public ServerBackupExpansion(ServerBackupPlugin plugin) {
        this.plugin = plugin;
        this.backupAPI = ServerBackupPlugin.getAPI();
    }
    
    @Override
    @NotNull
    public String getIdentifier() {
        return "serverbackup";
    }
    
    @Override
    @NotNull
    public String getAuthor() {
        return "ffurkandemir";
    }
    
    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true; // Keep expansion loaded across reloads
    }
    
    @Override
    public boolean canRegister() {
        return true;
    }
    
    @Override
    @Nullable
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        
        if (backupAPI == null) {
            return "API Disabled";
        }
        
        // %serverbackup_last_backup_time%
        if (identifier.equals("last_backup_time")) {
            BackupResult lastBackup = backupAPI.getLastBackup();
            if (lastBackup == null) {
                return "Never";
            }
            return dateFormat.format(new Date(lastBackup.getEndTime()));
        }
        
        // %serverbackup_last_backup_ago%
        if (identifier.equals("last_backup_ago")) {
            BackupResult lastBackup = backupAPI.getLastBackup();
            if (lastBackup == null) {
                return "Never";
            }
            long ago = System.currentTimeMillis() - lastBackup.getEndTime();
            return formatDuration(ago);
        }
        
        // %serverbackup_backup_count%
        if (identifier.equals("backup_count")) {
            return String.valueOf(backupAPI.getBackupCount());
        }
        
        // %serverbackup_backup_size%
        if (identifier.equals("backup_size")) {
            long totalSize = backupAPI.getTotalBackupSize();
            return formatFileSize(totalSize);
        }
        
        // %serverbackup_backup_size_mb%
        if (identifier.equals("backup_size_mb")) {
            long totalSize = backupAPI.getTotalBackupSize();
            return String.format("%.1f", totalSize / 1024.0 / 1024.0);
        }
        
        // %serverbackup_is_running%
        if (identifier.equals("is_running")) {
            return String.valueOf(backupAPI.isBackupInProgress());
        }
        
        // %serverbackup_is_running_colored%
        if (identifier.equals("is_running_colored")) {
            return backupAPI.isBackupInProgress() ? "§a✓ Running" : "§7✗ Idle";
        }
        
        // %serverbackup_next_backup_in%
        if (identifier.equals("next_backup_in")) {
            if (!plugin.getConfig().getBoolean("backup.auto-backup-enabled", false)) {
                return "Disabled";
            }
            
            // Calculate based on interval and last backup
            BackupResult lastBackup = backupAPI.getLastBackup();
            if (lastBackup == null) {
                return "Unknown";
            }
            
            int intervalMinutes = plugin.getConfig().getInt("backup.auto-backup-interval", 720);
            long intervalMillis = intervalMinutes * 60 * 1000L;
            long nextBackupTime = lastBackup.getEndTime() + intervalMillis;
            long timeUntil = nextBackupTime - System.currentTimeMillis();
            
            if (timeUntil <= 0) {
                return "Soon";
            }
            
            return formatDuration(timeUntil);
        }
        
        // %serverbackup_latest_backup_name%
        if (identifier.equals("latest_backup_name")) {
            List<File> backups = backupAPI.listBackups();
            if (backups.isEmpty()) {
                return "None";
            }
            return backups.get(0).getName();
        }
        
        // %serverbackup_latest_backup_size%
        if (identifier.equals("latest_backup_size")) {
            List<File> backups = backupAPI.listBackups();
            if (backups.isEmpty()) {
                return "0 B";
            }
            return formatFileSize(backups.get(0).length());
        }
        
        // %serverbackup_status%
        if (identifier.equals("status")) {
            if (backupAPI.isBackupInProgress()) {
                return "§e⏳ Backing up...";
            }
            
            BackupResult lastBackup = backupAPI.getLastBackup();
            if (lastBackup == null) {
                return "§7⚠ No backups";
            }
            
            if (!lastBackup.isSuccess()) {
                return "§c✗ Last failed";
            }
            
            long ago = System.currentTimeMillis() - lastBackup.getEndTime();
            if (ago < 3600000) { // Less than 1 hour
                return "§a✓ Recent";
            } else if (ago < 86400000) { // Less than 1 day
                return "§e✓ Today";
            } else {
                return "§6✓ Old";
            }
        }
        
        return null; // Unknown placeholder
    }
    
    /**
     * Format file size (bytes to human readable)
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Format duration (millis to human readable)
     */
    private String formatDuration(long millis) {
        if (millis < 0) {
            return "Now";
        }
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }
}

package com.serverbackup.service;

import com.serverbackup.ServerBackupPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.ChatColor;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupService {
    
    private final ServerBackupPlugin plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    public BackupService(ServerBackupPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void createBackup(org.bukkit.command.CommandSender sender) {
        String defaultType = plugin.getConfig().getString("backup.default-backup-type", "world");
        createBackup(sender, defaultType);
    }
    
    public void createBackup(org.bukkit.command.CommandSender sender, String backupType) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String type = backupType.equalsIgnoreCase("full") ? "full" : "world";
                String startMsg = getMessage("backup-started").replace("{type}", type);
                broadcast(startMsg, sender);
                
                // Save all worlds
                for (World world : plugin.getServer().getWorlds()) {
                    world.save();
                }
                
                // Create backup
                String timestamp = dateFormat.format(new Date());
                String filename = "backup-" + timestamp + ".zip";
                File backupDir = getBackupDirectory();
                File backupFile = new File(backupDir, filename);
                
                boolean compress = plugin.getConfig().getBoolean("backup.compress", true);
                
                if (compress) {
                    createZipBackup(backupFile, type);
                } else {
                    createFolderBackup(new File(backupDir, "backup-" + timestamp), type);
                }
                
                // Clean old backups
                cleanOldBackups();
                
                String message = getMessage("backup-completed")
                    .replace("{filename}", filename)
                    .replace("{type}", type);
                broadcast(message, sender);
                
            } catch (Exception e) {
                String message = getMessage("backup-failed").replace("{error}", e.getMessage());
                broadcast(message, sender);
                plugin.getLogger().severe("Backup failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void createZipBackup(File backupFile, String backupType) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            // Get worlds to backup
            List<String> worldNames = plugin.getConfig().getStringList("backup.worlds");
            if (worldNames.isEmpty()) {
                for (World world : plugin.getServer().getWorlds()) {
                    worldNames.add(world.getName());
                }
            }
            
            // Backup each world
            for (String worldName : worldNames) {
                File worldFolder = new File(plugin.getServer().getWorldContainer(), worldName);
                if (worldFolder.exists() && worldFolder.isDirectory()) {
                    addFolderToZip(worldFolder, worldFolder.getName(), zos);
                }
            }
            
            // Include plugins for full backup or if configured
            boolean includePlugins = backupType.equalsIgnoreCase("full") || 
                                   plugin.getConfig().getBoolean("backup.include-plugins", false);
            if (includePlugins) {
                File pluginsFolder = plugin.getDataFolder().getParentFile();
                addFolderToZip(pluginsFolder, "plugins", zos);
            }
        }
    }
    
    private void addFolderToZip(File folder, String parentPath, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                addFolderToZip(file, parentPath + "/" + file.getName(), zos);
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    ZipEntry zipEntry = new ZipEntry(parentPath + "/" + file.getName());
                    zos.putNextEntry(zipEntry);
                    
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    
                    zos.closeEntry();
                }
            }
        }
    }
    
    private void createFolderBackup(File backupFolder, String backupType) throws IOException {
        backupFolder.mkdirs();
        
        List<String> worldNames = plugin.getConfig().getStringList("backup.worlds");
        if (worldNames.isEmpty()) {
            for (World world : plugin.getServer().getWorlds()) {
                worldNames.add(world.getName());
            }
        }
        
        for (String worldName : worldNames) {
            File worldFolder = new File(plugin.getServer().getWorldContainer(), worldName);
            if (worldFolder.exists() && worldFolder.isDirectory()) {
                copyFolder(worldFolder.toPath(), new File(backupFolder, worldName).toPath());
            }
        }
        
        // Include plugins for full backup
        boolean includePlugins = backupType.equalsIgnoreCase("full") || 
                               plugin.getConfig().getBoolean("backup.include-plugins", false);
        if (includePlugins) {
            File pluginsFolder = plugin.getDataFolder().getParentFile();
            copyFolder(pluginsFolder.toPath(), new File(backupFolder, "plugins").toPath());
        }
    }
    
    private void copyFolder(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to copy: " + sourcePath);
            }
        });
    }
    
    public List<File> listBackups() {
        File backupDir = getBackupDirectory();
        File[] files = backupDir.listFiles((dir, name) -> 
            name.startsWith("backup-") && (name.endsWith(".zip") || new File(dir, name).isDirectory())
        );
        
        if (files == null) return new ArrayList<>();
        
        List<File> backups = Arrays.asList(files);
        backups.sort((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        return backups;
    }
    
    public boolean deleteBackup(String backupName) {
        File backupDir = getBackupDirectory();
        File backupFile = new File(backupDir, backupName);
        
        if (!backupFile.exists()) return false;
        
        if (backupFile.isDirectory()) {
            return deleteDirectory(backupFile);
        } else {
            return backupFile.delete();
        }
    }
    
    private boolean deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return directory.delete();
    }
    
    private void cleanOldBackups() {
        int maxBackups = plugin.getConfig().getInt("backup.max-backups", 10);
        if (maxBackups <= 0) return;
        
        List<File> backups = listBackups();
        if (backups.size() > maxBackups) {
            for (int i = maxBackups; i < backups.size(); i++) {
                File oldBackup = backups.get(i);
                if (deleteBackup(oldBackup.getName())) {
                    plugin.getLogger().info("Deleted old backup: " + oldBackup.getName());
                }
            }
        }
    }
    
    private File getBackupDirectory() {
        String backupPath = plugin.getConfig().getString("backup.directory", "backups");
        File backupDir = new File(plugin.getServer().getWorldContainer(), backupPath);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        return backupDir;
    }
    
    private String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    private void broadcast(String message, org.bukkit.command.CommandSender sender) {
        boolean broadcast = plugin.getConfig().getBoolean("backup.broadcast-messages", true);
        
        if (broadcast) {
            Bukkit.broadcastMessage(message);
        } else if (sender != null) {
            sender.sendMessage(message);
        } else {
            plugin.getLogger().info(ChatColor.stripColor(message));
        }
    }
    
    public String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}

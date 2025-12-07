package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BackupListCommand implements CommandExecutor {
    
    private final ServerBackupPlugin plugin;
    private final BackupService backupService;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public BackupListCommand(ServerBackupPlugin plugin, BackupService backupService) {
        this.plugin = plugin;
        this.backupService = backupService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("serverbackup.list")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        List<File> backups = backupService.listBackups();
        
        if (backups.isEmpty()) {
            sender.sendMessage(getMessage("no-backups"));
            return true;
        }
        
        sender.sendMessage(getMessage("list-header"));
        
        for (File backup : backups) {
            long size = backup.isDirectory() ? getFolderSize(backup) : backup.length();
            String sizeStr = backupService.formatFileSize(size);
            String dateStr = dateFormat.format(new Date(backup.lastModified()));
            
            String message = getMessage("list-entry")
                .replace("{filename}", backup.getName())
                .replace("{size}", sizeStr)
                .replace("{date}", dateStr);
            
            sender.sendMessage(message);
        }
        
        return true;
    }
    
    private long getFolderSize(File folder) {
        long size = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += getFolderSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }
    
    private String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}

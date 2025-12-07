package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;

/**
 * Base class for all backup commands
 * Provides common functionality and reduces code duplication
 */
public abstract class BaseCommand implements CommandExecutor {
    
    protected final ServerBackupPlugin plugin;
    protected final BackupService backupService;
    
    protected BaseCommand(ServerBackupPlugin plugin, BackupService backupService) {
        this.plugin = plugin;
        this.backupService = backupService;
    }
    
    /**
     * Get a localized message from config.yml
     * @param key Message key from messages section
     * @return Formatted message with color codes translated
     */
    protected String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Send a colored message to sender
     * @param sender Command sender
     * @param color Color code
     * @param message Message text
     */
    protected void sendColoredMessage(org.bukkit.command.CommandSender sender, ChatColor color, String message) {
        sender.sendMessage(color + message);
    }
    
    /**
     * Validate if a backup name is safe (prevent path traversal attacks)
     * @param backupName The backup filename to validate
     * @return true if safe, false otherwise
     */
    protected boolean isValidBackupName(String backupName) {
        if (backupName == null || backupName.trim().isEmpty()) {
            return false;
        }
        
        // Prevent path traversal attacks
        if (backupName.contains("..") || backupName.contains("/") || backupName.contains("\\")) {
            return false;
        }
        
        // Must start with "backup-"
        if (!backupName.startsWith("backup-")) {
            return false;
        }
        
        return true;
    }
}

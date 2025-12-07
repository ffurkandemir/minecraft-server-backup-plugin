package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BackupCommand implements CommandExecutor {
    
    private final ServerBackupPlugin plugin;
    private final BackupService backupService;
    
    public BackupCommand(ServerBackupPlugin plugin, BackupService backupService) {
        this.plugin = plugin;
        this.backupService = backupService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("serverbackup.backup")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        // No args or "now" - use default backup type
        if (args.length == 0 || args[0].equalsIgnoreCase("now")) {
            backupService.createBackup(sender);
            return true;
        }
        
        // Backup with specified type: /backup world or /backup full
        if (args[0].equalsIgnoreCase("world")) {
            backupService.createBackup(sender, "world");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("full")) {
            backupService.createBackup(sender, "full");
            return true;
        }
        
        // Toggle auto backup
        if (args[0].equalsIgnoreCase("auto")) {
            boolean currentState = plugin.getConfig().getBoolean("backup.auto-backup-enabled");
            plugin.getConfig().set("backup.auto-backup-enabled", !currentState);
            plugin.saveConfig();
            plugin.restartAutoBackup();
            
            String status = !currentState ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
            sender.sendMessage(ChatColor.GOLD + "Auto-backup has been " + status);
            if (!currentState) {
                int interval = plugin.getConfig().getInt("backup.auto-backup-interval", 720);
                sender.sendMessage(ChatColor.YELLOW + "Auto-backup will run every " + interval + " minutes (twice daily)");
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("cancel")) {
            sender.sendMessage(ChatColor.YELLOW + "Note: Backups run asynchronously and cannot be cancelled once started.");
            return true;
        }
        
        sender.sendMessage(ChatColor.RED + "Usage: /backup [world|full|now|auto|cancel]");
        sender.sendMessage(ChatColor.YELLOW + "  world - Backup worlds only (default)");
        sender.sendMessage(ChatColor.YELLOW + "  full  - Backup worlds and plugins");
        sender.sendMessage(ChatColor.YELLOW + "  auto  - Toggle automatic backups");
        return true;
    }
    
    private String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}

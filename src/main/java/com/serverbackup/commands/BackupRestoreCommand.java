package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BackupRestoreCommand implements CommandExecutor {
    
    private final ServerBackupPlugin plugin;
    private final BackupService backupService;
    
    public BackupRestoreCommand(ServerBackupPlugin plugin, BackupService backupService) {
        this.plugin = plugin;
        this.backupService = backupService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("serverbackup.restore")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /backuprestore <backup-name>");
            return true;
        }
        
        String backupName = args[0];
        
        sender.sendMessage(ChatColor.GOLD + "Backup restoration is a manual process:");
        sender.sendMessage(ChatColor.YELLOW + "1. Stop the server");
        sender.sendMessage(ChatColor.YELLOW + "2. Extract the backup file: " + backupName);
        sender.sendMessage(ChatColor.YELLOW + "3. Replace the world folders with the backed up ones");
        sender.sendMessage(ChatColor.YELLOW + "4. Restart the server");
        sender.sendMessage(ChatColor.RED + "WARNING: This will overwrite current world data!");
        
        return true;
    }
    
    private String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}

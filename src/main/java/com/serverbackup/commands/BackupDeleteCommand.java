package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BackupDeleteCommand implements CommandExecutor {
    
    private final ServerBackupPlugin plugin;
    private final BackupService backupService;
    
    public BackupDeleteCommand(ServerBackupPlugin plugin, BackupService backupService) {
        this.plugin = plugin;
        this.backupService = backupService;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("serverbackup.delete")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /backupdelete <backup-name>");
            return true;
        }
        
        String backupName = args[0];
        
        if (backupService.deleteBackup(backupName)) {
            String message = getMessage("backup-deleted").replace("{filename}", backupName);
            sender.sendMessage(message);
        } else {
            sender.sendMessage(getMessage("invalid-backup"));
        }
        
        return true;
    }
    
    private String getMessage(String key) {
        String message = plugin.getConfig().getString("messages." + key, key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}

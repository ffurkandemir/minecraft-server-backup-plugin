package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class BackupDeleteCommand extends BaseCommand {
    
    public BackupDeleteCommand(ServerBackupPlugin plugin, BackupService backupService) {
        super(plugin, backupService);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("serverbackup.delete")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendColoredMessage(sender, ChatColor.RED, "Usage: /backupdelete <backup-name>");
            return true;
        }
        
        String backupName = args[0];
        
        // Security: Validate backup name to prevent path traversal
        if (!isValidBackupName(backupName)) {
            sendColoredMessage(sender, ChatColor.RED, "Invalid backup name! Backup names must start with 'backup-' and contain no path separators.");
            return true;
        }
        
        if (backupService.deleteBackup(backupName)) {
            String message = getMessage("backup-deleted").replace("{filename}", backupName);
            sender.sendMessage(message);
        } else {
            sender.sendMessage(getMessage("invalid-backup"));
        }
        
        return true;
    }
}

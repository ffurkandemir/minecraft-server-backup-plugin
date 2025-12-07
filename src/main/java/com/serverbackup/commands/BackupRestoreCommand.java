package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class BackupRestoreCommand extends BaseCommand {
    
    public BackupRestoreCommand(ServerBackupPlugin plugin, BackupService backupService) {
        super(plugin, backupService);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("serverbackup.restore")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            sendColoredMessage(sender, ChatColor.RED, "Usage: /backuprestore <backup-name>");
            return true;
        }
        
        String backupName = args[0];
        
        // Security: Validate backup name to prevent path traversal
        if (!isValidBackupName(backupName)) {
            sendColoredMessage(sender, ChatColor.RED, "Invalid backup name! Backup names must start with 'backup-' and contain no path separators.");
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "Backup restoration is a manual process:");
        sender.sendMessage(ChatColor.YELLOW + "1. Stop the server");
        sender.sendMessage(ChatColor.YELLOW + "2. Extract the backup file: " + backupName);
        sender.sendMessage(ChatColor.YELLOW + "3. Replace the world folders with the backed up ones");
        sender.sendMessage(ChatColor.YELLOW + "4. Restart the server");
        sendColoredMessage(sender, ChatColor.RED, "WARNING: This will overwrite current world data!");
        
        return true;
    }
}

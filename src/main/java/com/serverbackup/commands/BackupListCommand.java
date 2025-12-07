package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BackupListCommand extends BaseCommand {
    
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public BackupListCommand(ServerBackupPlugin plugin, BackupService backupService) {
        super(plugin, backupService);
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
        
        // Calculate sizes asynchronously to avoid blocking
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            for (File backup : backups) {
                long size = backup.isDirectory() ? getFolderSize(backup) : backup.length();
                String sizeStr = backupService.formatFileSize(size);
                String dateStr = dateFormat.format(new Date(backup.lastModified()));
                
                String message = getMessage("list-entry")
                    .replace("{filename}", backup.getName())
                    .replace("{size}", sizeStr)
                    .replace("{date}", dateStr);
                
                // Send message on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage(message));
            }
        });
        
        return true;
    }
    
    /**
     * Calculate folder size recursively
     * WARNING: This method can be slow for large folders. Always call from async thread.
     */
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
}

package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;

/**
 * Shows total size and statistics of all backups
 */
public class BackupSizeCommand extends BaseCommand {
    
    public BackupSizeCommand(ServerBackupPlugin plugin, BackupService backupService) {
        super(plugin, backupService);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("serverbackup.list")) {
            sender.sendMessage(getMessage("no-permission"));
            return true;
        }
        
        sender.sendMessage(ChatColor.GOLD + "╔═══════════════════════════════════════╗");
        sender.sendMessage(ChatColor.GOLD + "║     " + ChatColor.YELLOW + "Calculating Backup Size..." + ChatColor.GOLD + "     ║");
        sender.sendMessage(ChatColor.GOLD + "╚═══════════════════════════════════════╝");
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<File> backups = backupService.listBackups();
            
            if (backups.isEmpty()) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage(ChatColor.YELLOW + "No backups found."));
                return;
            }
            
            long totalSize = 0;
            long largestSize = 0;
            long smallestSize = Long.MAX_VALUE;
            File largestBackup = null;
            File smallestBackup = null;
            
            for (File backup : backups) {
                long size = backup.isDirectory() ? getFolderSize(backup) : backup.length();
                totalSize += size;
                
                if (size > largestSize) {
                    largestSize = size;
                    largestBackup = backup;
                }
                
                if (size < smallestSize) {
                    smallestSize = size;
                    smallestBackup = backup;
                }
            }
            
            long avgSize = totalSize / backups.size();
            
            File finalLargestBackup = largestBackup;
            File finalSmallestBackup = smallestBackup;
            long finalTotalSize = totalSize;
            long finalLargestSize = largestSize;
            long finalSmallestSize = smallestSize;
            long finalAvgSize = avgSize;
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage("");
                sender.sendMessage(ChatColor.AQUA + "Total Backups: " + ChatColor.WHITE + backups.size());
                sender.sendMessage(ChatColor.AQUA + "Total Size: " + ChatColor.WHITE + backupService.formatFileSize(finalTotalSize));
                sender.sendMessage(ChatColor.AQUA + "Average Size: " + ChatColor.WHITE + backupService.formatFileSize(finalAvgSize));
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GREEN + "Largest: " + ChatColor.WHITE + 
                    finalLargestBackup.getName() + ChatColor.GRAY + " (" + 
                    backupService.formatFileSize(finalLargestSize) + ")");
                sender.sendMessage(ChatColor.YELLOW + "Smallest: " + ChatColor.WHITE + 
                    finalSmallestBackup.getName() + ChatColor.GRAY + " (" + 
                    backupService.formatFileSize(finalSmallestSize) + ")");
                
                // Show disk space
                File backupDir = backupService.getBackupDirectory();
                long freeSpace = backupDir.getFreeSpace();
                long totalSpace = backupDir.getTotalSpace();
                long usedSpace = totalSpace - freeSpace;
                double usedPercent = (usedSpace * 100.0) / totalSpace;
                
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GOLD + "Disk Usage:");
                sender.sendMessage(ChatColor.GRAY + "  Free: " + ChatColor.WHITE + 
                    backupService.formatFileSize(freeSpace));
                sender.sendMessage(ChatColor.GRAY + "  Used: " + ChatColor.WHITE + 
                    backupService.formatFileSize(usedSpace) + ChatColor.GRAY + 
                    String.format(" (%.1f%%)", usedPercent));
            });
        });
        
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
}

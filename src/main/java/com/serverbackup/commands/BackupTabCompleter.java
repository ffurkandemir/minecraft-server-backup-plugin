package com.serverbackup.commands;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.service.BackupService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides tab completion for backup commands
 */
public class BackupTabCompleter implements TabCompleter {
    
    private final BackupService backupService;
    
    public BackupTabCompleter(BackupService backupService) {
        this.backupService = backupService;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("backup")) {
            if (args.length == 1) {
                completions = Arrays.asList("world", "full", "now", "auto", "info", "status");
            }
        } else if (command.getName().equalsIgnoreCase("backuplist")) {
            if (args.length == 1) {
                completions = Arrays.asList("all", "recent");
            }
        } else if (command.getName().equalsIgnoreCase("backuprestore") || 
                   command.getName().equalsIgnoreCase("backupdelete")) {
            if (args.length == 1) {
                // Suggest available backup names
                List<File> backups = backupService.listBackups();
                completions = backups.stream()
                    .map(File::getName)
                    .collect(Collectors.toList());
            }
        }
        
        // Filter completions based on what user has typed
        if (args.length > 0) {
            String input = args[args.length - 1].toLowerCase();
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        }
        
        return completions;
    }
}

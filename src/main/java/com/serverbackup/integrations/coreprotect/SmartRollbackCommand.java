package com.serverbackup.integrations.coreprotect;

import com.serverbackup.ServerBackupPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Smart rollback command using CoreProtect integration
 * 
 * Usage:
 *   /smartrollback <time> [user] [radius]
 * 
 * Examples:
 *   /smartrollback 1h              - Rollback last hour (all users, global)
 *   /smartrollback 30m Griefer123  - Rollback Griefer123's actions in last 30min
 *   /smartrollback 2h Griefer123 100 - Within 100 block radius
 */
public class SmartRollbackCommand implements CommandExecutor {
    
    private final ServerBackupPlugin plugin;
    private final CoreProtectIntegration integration;
    
    public SmartRollbackCommand(ServerBackupPlugin plugin, CoreProtectIntegration integration) {
        this.plugin = plugin;
        this.integration = integration;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, @NotNull String[] args) {
        
        if (!sender.hasPermission("serverbackup.smartrollback")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        if (!integration.isEnabled()) {
            sender.sendMessage(ChatColor.RED + "CoreProtect integration is not enabled!");
            sender.sendMessage(ChatColor.YELLOW + "Enable it in config.yml: integrations.coreprotect.enabled");
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /smartrollback <time> [user] [radius]");
            sender.sendMessage(ChatColor.GRAY + "Examples:");
            sender.sendMessage(ChatColor.GRAY + "  /smartrollback 1h");
            sender.sendMessage(ChatColor.GRAY + "  /smartrollback 30m Griefer123");
            sender.sendMessage(ChatColor.GRAY + "  /smartrollback 2h Griefer123 100");
            return true;
        }
        
        // Parse time
        String timeStr = args[0];
        int timeSeconds;
        try {
            timeSeconds = parseTime(timeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid time format: " + timeStr);
            sender.sendMessage(ChatColor.YELLOW + "Examples: 30m, 1h, 2h30m, 1d");
            return true;
        }
        
        // Parse username (optional)
        String username = args.length > 1 ? args[1] : null;
        
        // Parse radius (optional)
        int radius = 0;
        Location location = null;
        
        if (args.length > 2) {
            try {
                radius = Integer.parseInt(args[2]);
                if (sender instanceof Player) {
                    location = ((Player) sender).getLocation();
                } else {
                    sender.sendMessage(ChatColor.RED + "Radius requires you to be a player!");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid radius: " + args[2]);
                return true;
            }
        }
        
        // Confirm action
        sender.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════════");
        sender.sendMessage(ChatColor.GOLD + "Smart Rollback Parameters:");
        sender.sendMessage(ChatColor.GRAY + "  Time: " + ChatColor.WHITE + timeStr + ChatColor.GRAY + " (" + timeSeconds + "s)");
        sender.sendMessage(ChatColor.GRAY + "  User: " + ChatColor.WHITE + (username != null ? username : "all"));
        sender.sendMessage(ChatColor.GRAY + "  Radius: " + ChatColor.WHITE + (radius > 0 ? radius + " blocks" : "global"));
        sender.sendMessage(ChatColor.YELLOW + "═══════════════════════════════════════");
        
        // Perform smart rollback
        integration.performSmartRollback(sender, timeSeconds, username, radius, location)
            .exceptionally(error -> {
                sender.sendMessage(ChatColor.RED + "Error: " + error.getMessage());
                return null;
            });
        
        return true;
    }
    
    /**
     * Parse time string to seconds
     * Formats: 30s, 5m, 2h, 1d, 2h30m, etc.
     */
    private int parseTime(String timeStr) throws IllegalArgumentException {
        timeStr = timeStr.toLowerCase().trim();
        int total = 0;
        int current = 0;
        
        for (char c : timeStr.toCharArray()) {
            if (Character.isDigit(c)) {
                current = current * 10 + (c - '0');
            } else {
                if (current == 0) {
                    throw new IllegalArgumentException("Invalid time format");
                }
                
                switch (c) {
                    case 's': total += current; break;
                    case 'm': total += current * 60; break;
                    case 'h': total += current * 3600; break;
                    case 'd': total += current * 86400; break;
                    case 'w': total += current * 604800; break;
                    default: throw new IllegalArgumentException("Unknown time unit: " + c);
                }
                current = 0;
            }
        }
        
        if (current > 0) {
            // No unit specified, assume seconds
            total += current;
        }
        
        if (total <= 0) {
            throw new IllegalArgumentException("Time must be positive");
        }
        
        return total;
    }
}

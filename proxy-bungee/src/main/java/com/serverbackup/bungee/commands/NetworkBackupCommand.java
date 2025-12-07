package com.serverbackup.bungee.commands;

import com.serverbackup.bungee.ServerBackupBungee;
import com.serverbackup.bungee.coordinator.BackupCoordinator;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

/**
 * Network backup command for BungeeCord proxy
 * Usage: /networkbackup [server|all] [world|full]
 */
public class NetworkBackupCommand extends Command {
    
    private final ServerBackupBungee plugin;
    private final BackupCoordinator coordinator;
    
    public NetworkBackupCommand(ServerBackupBungee plugin, BackupCoordinator coordinator) {
        super("networkbackup", "serverbackup.network", "netbackup", "nbackup");
        this.plugin = plugin;
        this.coordinator = coordinator;
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("serverbackup.network")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return;
        }
        
        // Default values
        String backupType = "full";
        boolean sequential = true;
        
        // Parse arguments
        if (args.length == 0) {
            // /networkbackup - backup all servers
            coordinator.startNetworkBackup(sender, sequential, backupType);
            return;
        }
        
        String target = args[0].toLowerCase();
        
        // Parse backup type if provided
        if (args.length >= 2) {
            String typeArg = args[1].toLowerCase();
            if (typeArg.equals("world") || typeArg.equals("full")) {
                backupType = typeArg;
            }
        }
        
        // Handle different targets
        switch (target) {
            case "all":
                coordinator.startNetworkBackup(sender, sequential, backupType);
                break;
            
            case "parallel":
                coordinator.startNetworkBackup(sender, false, backupType);
                break;
            
            case "help":
                sendHelp(sender);
                break;
            
            default:
                // Assume it's a server name
                coordinator.startServerBackup(sender, target, backupType);
                break;
        }
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
        sender.sendMessage(ChatColor.GOLD + "Network Backup Command Help");
        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
        sender.sendMessage(ChatColor.YELLOW + "/networkbackup" + ChatColor.GRAY + " - Backup all servers (sequential)");
        sender.sendMessage(ChatColor.YELLOW + "/networkbackup all" + ChatColor.GRAY + " - Backup all servers");
        sender.sendMessage(ChatColor.YELLOW + "/networkbackup parallel" + ChatColor.GRAY + " - Backup all (parallel)");
        sender.sendMessage(ChatColor.YELLOW + "/networkbackup <server>" + ChatColor.GRAY + " - Backup specific server");
        sender.sendMessage(ChatColor.YELLOW + "/networkbackup <target> world" + ChatColor.GRAY + " - World backup only");
        sender.sendMessage(ChatColor.YELLOW + "/networkbackup <target> full" + ChatColor.GRAY + " - Full server backup");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GRAY + "Examples:");
        sender.sendMessage(ChatColor.WHITE + "  /networkbackup" + ChatColor.GRAY + " - Backup all servers one by one");
        sender.sendMessage(ChatColor.WHITE + "  /networkbackup lobby" + ChatColor.GRAY + " - Backup only lobby server");
        sender.sendMessage(ChatColor.WHITE + "  /networkbackup parallel full" + ChatColor.GRAY + " - Parallel full backup");
        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
    }
}

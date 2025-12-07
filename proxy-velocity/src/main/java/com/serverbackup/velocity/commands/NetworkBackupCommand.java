package com.serverbackup.velocity.commands;

import com.serverbackup.velocity.ServerBackupVelocity;
import com.serverbackup.velocity.coordinator.BackupCoordinator;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Network backup command for Velocity proxy
 * Usage: /networkbackup [server|all] [world|full]
 */
public class NetworkBackupCommand implements SimpleCommand {
    
    private final ServerBackupVelocity plugin;
    private final BackupCoordinator coordinator;
    
    public NetworkBackupCommand(ServerBackupVelocity plugin, BackupCoordinator coordinator) {
        this.plugin = plugin;
        this.coordinator = coordinator;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        // Check permission
        if (!source.hasPermission("serverbackup.network")) {
            source.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return;
        }
        
        // Default values
        String backupType = "full";
        boolean sequential = true;
        
        // Parse arguments
        if (args.length == 0) {
            // /networkbackup - backup all servers
            coordinator.startNetworkBackup(source, sequential, backupType);
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
                coordinator.startNetworkBackup(source, sequential, backupType);
                break;
            
            case "parallel":
                coordinator.startNetworkBackup(source, false, backupType);
                break;
            
            case "help":
                sendHelp(source);
                break;
            
            default:
                // Assume it's a server name
                coordinator.startServerBackup(source, target, backupType);
                break;
        }
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("serverbackup.network");
    }
    
    private void sendHelp(CommandSource source) {
        source.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.GOLD));
        source.sendMessage(Component.text("Network Backup Command Help", NamedTextColor.GOLD));
        source.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.GOLD));
        source.sendMessage(Component.text("/networkbackup", NamedTextColor.YELLOW)
            .append(Component.text(" - Backup all servers (sequential)", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/networkbackup all", NamedTextColor.YELLOW)
            .append(Component.text(" - Backup all servers", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/networkbackup parallel", NamedTextColor.YELLOW)
            .append(Component.text(" - Backup all (parallel)", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/networkbackup <server>", NamedTextColor.YELLOW)
            .append(Component.text(" - Backup specific server", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/networkbackup <target> world", NamedTextColor.YELLOW)
            .append(Component.text(" - World backup only", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("/networkbackup <target> full", NamedTextColor.YELLOW)
            .append(Component.text(" - Full server backup", NamedTextColor.GRAY)));
        source.sendMessage(Component.empty());
        source.sendMessage(Component.text("Examples:", NamedTextColor.GRAY));
        source.sendMessage(Component.text("  /networkbackup", NamedTextColor.WHITE)
            .append(Component.text(" - Backup all servers one by one", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /networkbackup lobby", NamedTextColor.WHITE)
            .append(Component.text(" - Backup only lobby server", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("  /networkbackup parallel full", NamedTextColor.WHITE)
            .append(Component.text(" - Parallel full backup", NamedTextColor.GRAY)));
        source.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.GOLD));
    }
}

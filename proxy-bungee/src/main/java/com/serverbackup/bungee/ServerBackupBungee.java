package com.serverbackup.bungee;

import com.serverbackup.bungee.commands.NetworkBackupCommand;
import com.serverbackup.bungee.coordinator.BackupCoordinator;
import net.md_5.bungee.api.plugin.Plugin;

public class ServerBackupBungee extends Plugin {
    
    private BackupCoordinator coordinator;
    
    @Override
    public void onEnable() {
        // Create config directory
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Initialize coordinator
        coordinator = new BackupCoordinator(this);
        coordinator.initialize();
        
        // Register commands
        getProxy().getPluginManager().registerCommand(this, new NetworkBackupCommand(this, coordinator));
        
        getLogger().info("═══════════════════════════════════════════");
        getLogger().info("ServerBackup BungeeCord Coordinator v" + getDescription().getVersion());
        getLogger().info("Network backup orchestration enabled");
        getLogger().info("Command: /networkbackup [server|all]");
        getLogger().info("═══════════════════════════════════════════");
    }
    
    @Override
    public void onDisable() {
        if (coordinator != null) {
            coordinator.shutdown();
        }
        getLogger().info("ServerBackup BungeeCord coordinator disabled");
    }
    
    public BackupCoordinator getCoordinator() {
        return coordinator;
    }
}

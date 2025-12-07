package com.serverbackup.velocity;

import com.google.inject.Inject;
import com.serverbackup.velocity.commands.NetworkBackupCommand;
import com.serverbackup.velocity.coordinator.BackupCoordinator;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "serverbackup-velocity",
    name = "ServerBackup-Velocity",
    version = "2.0.0",
    description = "Network backup coordinator for Velocity",
    authors = {"ServerBackup Team"}
)
public class ServerBackupVelocity {
    
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private BackupCoordinator coordinator;
    
    @Inject
    public ServerBackupVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }
    
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Initialize coordinator
        coordinator = new BackupCoordinator(this);
        coordinator.initialize();
        
        // Register commands
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("networkbackup")
                .aliases("netbackup", "nbackup")
                .build(),
            new NetworkBackupCommand(this, coordinator)
        );
        
        logger.info("═══════════════════════════════════════════");
        logger.info("ServerBackup Velocity Coordinator v2.0.0");
        logger.info("Network backup orchestration enabled");
        logger.info("Command: /networkbackup [server|all]");
        logger.info("═══════════════════════════════════════════");
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (coordinator != null) {
            coordinator.shutdown();
        }
        logger.info("ServerBackup Velocity coordinator disabled");
    }
    
    public ProxyServer getServer() {
        return server;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public BackupCoordinator getCoordinator() {
        return coordinator;
    }
}

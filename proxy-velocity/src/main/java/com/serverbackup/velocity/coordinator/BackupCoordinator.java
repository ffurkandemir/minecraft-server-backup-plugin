package com.serverbackup.velocity.coordinator;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.serverbackup.velocity.ServerBackupVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.*;

/**
 * Coordinates backup operations across multiple backend servers (Velocity)
 */
public class BackupCoordinator {
    
    private final ServerBackupVelocity plugin;
    private final Map<String, BackupSession> activeSessions;
    private final ScheduledExecutorService scheduler;
    
    private static final MinecraftChannelIdentifier CHANNEL_REQUEST = 
        MinecraftChannelIdentifier.create("serverbackup", "request");
    private static final MinecraftChannelIdentifier CHANNEL_RESPONSE = 
        MinecraftChannelIdentifier.create("serverbackup", "response");
    
    public BackupCoordinator(ServerBackupVelocity plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    public void initialize() {
        // Register plugin channels
        plugin.getServer().getChannelRegistrar().register(CHANNEL_REQUEST, CHANNEL_RESPONSE);
        
        // Register listener
        plugin.getServer().getEventManager().register(plugin, this);
        
        plugin.getLogger().info("Backup coordinator initialized");
        plugin.getLogger().info("  Listening on: serverbackup:response");
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    /**
     * Start network backup for all servers
     */
    public void startNetworkBackup(CommandSource source, boolean sequential, String backupType) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        List<String> serverNames = new ArrayList<>();
        plugin.getServer().getAllServers().forEach(s -> serverNames.add(s.getServerInfo().getName()));
        
        if (serverNames.isEmpty()) {
            source.sendMessage(Component.text("No backend servers found!", NamedTextColor.RED));
            return;
        }
        
        BackupSession session = new BackupSession(sessionId, serverNames, source);
        activeSessions.put(sessionId, session);
        
        source.sendMessage(Component.text("╔════════════════════════════════════════╗", NamedTextColor.GOLD));
        source.sendMessage(Component.text("║    NETWORK BACKUP INITIATED            ║", NamedTextColor.GOLD));
        source.sendMessage(Component.text("╚════════════════════════════════════════╝", NamedTextColor.GOLD));
        source.sendMessage(Component.text("Session ID: ", NamedTextColor.YELLOW)
            .append(Component.text(sessionId, NamedTextColor.WHITE)));
        source.sendMessage(Component.text("Servers: ", NamedTextColor.YELLOW)
            .append(Component.text(serverNames.size(), NamedTextColor.WHITE)));
        source.sendMessage(Component.text("Mode: ", NamedTextColor.YELLOW)
            .append(Component.text(sequential ? "Sequential" : "Parallel", NamedTextColor.WHITE)));
        source.sendMessage(Component.empty());
        
        if (sequential) {
            executeSequentialBackup(session, serverNames, backupType, 0);
        } else {
            executeParallelBackup(session, serverNames, backupType);
        }
        
        // Timeout after 10 minutes
        scheduler.schedule(() -> {
            if (activeSessions.containsKey(sessionId)) {
                session.timeout();
                activeSessions.remove(sessionId);
            }
        }, 10, TimeUnit.MINUTES);
    }
    
    /**
     * Start backup for specific server
     */
    public void startServerBackup(CommandSource source, String serverName, String backupType) {
        Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(serverName);
        if (serverOpt.isEmpty()) {
            source.sendMessage(Component.text("Server not found: " + serverName, NamedTextColor.RED));
            return;
        }
        
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        BackupSession session = new BackupSession(sessionId, Collections.singletonList(serverName), source);
        activeSessions.put(sessionId, session);
        
        source.sendMessage(Component.text("Starting backup for server: ", NamedTextColor.YELLOW)
            .append(Component.text(serverName, NamedTextColor.WHITE)));
        source.sendMessage(Component.text("Session: " + sessionId, NamedTextColor.GRAY));
        
        sendBackupRequest(serverOpt.get(), sessionId, backupType);
        
        // Timeout after 5 minutes
        scheduler.schedule(() -> {
            if (activeSessions.containsKey(sessionId)) {
                session.timeout();
                activeSessions.remove(sessionId);
            }
        }, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Execute backups sequentially
     */
    private void executeSequentialBackup(BackupSession session, List<String> servers, String backupType, int index) {
        if (index >= servers.size()) {
            session.complete();
            activeSessions.remove(session.getSessionId());
            return;
        }
        
        String serverName = servers.get(index);
        Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(serverName);
        
        if (serverOpt.isEmpty()) {
            session.recordResult(serverName, false, "Server not found", 0, 0);
            executeSequentialBackup(session, servers, backupType, index + 1);
            return;
        }
        
        session.getSource().sendMessage(Component.text("→ " + serverName, NamedTextColor.AQUA)
            .append(Component.text(" - Starting...", NamedTextColor.GRAY)));
        sendBackupRequest(serverOpt.get(), session.getSessionId(), backupType);
        
        // Continue to next server after delay
        scheduler.schedule(() -> {
            if (activeSessions.containsKey(session.getSessionId())) {
                executeSequentialBackup(session, servers, backupType, index + 1);
            }
        }, 30, TimeUnit.SECONDS);
    }
    
    /**
     * Execute backups in parallel
     */
    private void executeParallelBackup(BackupSession session, List<String> servers, String backupType) {
        for (String serverName : servers) {
            Optional<RegisteredServer> serverOpt = plugin.getServer().getServer(serverName);
            if (serverOpt.isPresent()) {
                session.getSource().sendMessage(Component.text("→ " + serverName, NamedTextColor.AQUA)
                    .append(Component.text(" - Starting...", NamedTextColor.GRAY)));
                sendBackupRequest(serverOpt.get(), session.getSessionId(), backupType);
            } else {
                session.recordResult(serverName, false, "Server not found", 0, 0);
            }
        }
        
        // Auto-complete after timeout
        scheduler.schedule(() -> {
            if (activeSessions.containsKey(session.getSessionId())) {
                session.complete();
                activeSessions.remove(session.getSessionId());
            }
        }, 60, TimeUnit.SECONDS);
    }
    
    /**
     * Send backup request to backend server
     */
    private void sendBackupRequest(RegisteredServer server, String sessionId, String backupType) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("BackupRequest");
        out.writeUTF(sessionId);
        out.writeUTF(backupType);
        
        server.sendPluginMessage(CHANNEL_REQUEST, out.toByteArray());
    }
    
    /**
     * Handle backup responses from backend servers
     */
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL_RESPONSE)) {
            return;
        }
        
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }
        
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String subChannel = in.readUTF();
        
        if (subChannel.equals("BackupResponse")) {
            String sessionId = in.readUTF();
            String serverName = in.readUTF();
            boolean success = in.readBoolean();
            String message = in.readUTF();
            long duration = in.readLong();
            long size = in.readLong();
            
            BackupSession session = activeSessions.get(sessionId);
            if (session != null) {
                session.recordResult(serverName, success, message, duration, size);
                
                // Display result
                CommandSource source = session.getSource();
                if (success) {
                    if (message.equals("STARTED")) {
                        return; // Don't show started messages
                    }
                    long sizeMB = size / 1024 / 1024;
                    source.sendMessage(Component.text("✓ " + serverName, NamedTextColor.GREEN)
                        .append(Component.text(" - Completed (" + duration + "ms, " + sizeMB + "MB)", NamedTextColor.GRAY)));
                } else {
                    source.sendMessage(Component.text("✗ " + serverName, NamedTextColor.RED)
                        .append(Component.text(" - " + message, NamedTextColor.GRAY)));
                }
                
                // Check if complete
                if (session.isComplete()) {
                    session.complete();
                    activeSessions.remove(sessionId);
                }
            }
        }
    }
    
    /**
     * Backup session tracking
     */
    private class BackupSession {
        private final String sessionId;
        private final List<String> servers;
        private final CommandSource source;
        private final Map<String, BackupResult> results;
        private final long startTime;
        
        public BackupSession(String sessionId, List<String> servers, CommandSource source) {
            this.sessionId = sessionId;
            this.servers = servers;
            this.source = source;
            this.results = new ConcurrentHashMap<>();
            this.startTime = System.currentTimeMillis();
        }
        
        public void recordResult(String server, boolean success, String message, long duration, long size) {
            results.put(server, new BackupResult(success, message, duration, size));
        }
        
        public boolean isComplete() {
            return results.size() >= servers.size();
        }
        
        public void complete() {
            long totalDuration = System.currentTimeMillis() - startTime;
            int successCount = (int) results.values().stream().filter(r -> r.success).count();
            int failCount = results.size() - successCount;
            
            source.sendMessage(Component.empty());
            source.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.GOLD));
            source.sendMessage(Component.text("Network Backup Summary", NamedTextColor.GOLD));
            source.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.GOLD));
            source.sendMessage(Component.text("Session: ", NamedTextColor.YELLOW)
                .append(Component.text(sessionId, NamedTextColor.WHITE)));
            source.sendMessage(Component.text("Total Time: ", NamedTextColor.YELLOW)
                .append(Component.text((totalDuration / 1000) + "s", NamedTextColor.WHITE)));
            source.sendMessage(Component.text("Success: " + successCount, NamedTextColor.GREEN)
                .append(Component.text(" / ", NamedTextColor.GRAY))
                .append(Component.text("Failed: " + failCount, NamedTextColor.RED)));
            
            if (failCount > 0) {
                source.sendMessage(Component.empty());
                source.sendMessage(Component.text("Failed servers:", NamedTextColor.RED));
                results.forEach((server, result) -> {
                    if (!result.success) {
                        source.sendMessage(Component.text("  - " + server + ": " + result.message, NamedTextColor.RED));
                    }
                });
            }
            
            source.sendMessage(Component.text("═══════════════════════════════════════════", NamedTextColor.GOLD));
        }
        
        public void timeout() {
            source.sendMessage(Component.text("⚠ Backup session timed out!", NamedTextColor.RED));
            source.sendMessage(Component.text("Completed: " + results.size() + "/" + servers.size(), NamedTextColor.YELLOW));
            complete();
        }
        
        public String getSessionId() { return sessionId; }
        public CommandSource getSource() { return source; }
    }
    
    private record BackupResult(boolean success, String message, long duration, long size) {}
}

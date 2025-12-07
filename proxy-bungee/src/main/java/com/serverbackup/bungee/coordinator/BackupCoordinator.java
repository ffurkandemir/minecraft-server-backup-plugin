package com.serverbackup.bungee.coordinator;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.serverbackup.bungee.ServerBackupBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;
import java.util.concurrent.*;

/**
 * Coordinates backup operations across multiple backend servers
 */
public class BackupCoordinator implements Listener {
    
    private final ServerBackupBungee plugin;
    private final Map<String, BackupSession> activeSessions;
    private final ScheduledExecutorService scheduler;
    
    private static final String CHANNEL_REQUEST = "serverbackup:request";
    private static final String CHANNEL_RESPONSE = "serverbackup:response";
    
    public BackupCoordinator(ServerBackupBungee plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    public void initialize() {
        // Register plugin channels
        plugin.getProxy().registerChannel(CHANNEL_REQUEST);
        plugin.getProxy().registerChannel(CHANNEL_RESPONSE);
        
        // Register listener for responses
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
        
        plugin.getLogger().info("Backup coordinator initialized");
        plugin.getLogger().info("  Listening on: " + CHANNEL_RESPONSE);
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
     * Start network backup for all servers (sequential or parallel)
     */
    public void startNetworkBackup(CommandSender sender, boolean sequential, String backupType) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        List<String> serverNames = new ArrayList<>(plugin.getProxy().getServers().keySet());
        
        if (serverNames.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No backend servers found!");
            return;
        }
        
        BackupSession session = new BackupSession(sessionId, serverNames, sender);
        activeSessions.put(sessionId, session);
        
        sender.sendMessage(ChatColor.GOLD + "╔════════════════════════════════════════╗");
        sender.sendMessage(ChatColor.GOLD + "║    NETWORK BACKUP INITIATED            ║");
        sender.sendMessage(ChatColor.GOLD + "╚════════════════════════════════════════╝");
        sender.sendMessage(ChatColor.YELLOW + "Session ID: " + ChatColor.WHITE + sessionId);
        sender.sendMessage(ChatColor.YELLOW + "Servers: " + ChatColor.WHITE + serverNames.size());
        sender.sendMessage(ChatColor.YELLOW + "Mode: " + ChatColor.WHITE + (sequential ? "Sequential" : "Parallel"));
        sender.sendMessage("");
        
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
    public void startServerBackup(CommandSender sender, String serverName, String backupType) {
        ServerInfo server = plugin.getProxy().getServerInfo(serverName);
        if (server == null) {
            sender.sendMessage(ChatColor.RED + "Server not found: " + serverName);
            return;
        }
        
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        BackupSession session = new BackupSession(sessionId, Collections.singletonList(serverName), sender);
        activeSessions.put(sessionId, session);
        
        sender.sendMessage(ChatColor.YELLOW + "Starting backup for server: " + ChatColor.WHITE + serverName);
        sender.sendMessage(ChatColor.GRAY + "Session: " + sessionId);
        
        sendBackupRequest(server, sessionId, backupType);
        
        // Timeout after 5 minutes
        scheduler.schedule(() -> {
            if (activeSessions.containsKey(sessionId)) {
                session.timeout();
                activeSessions.remove(sessionId);
            }
        }, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Execute backups one server at a time
     */
    private void executeSequentialBackup(BackupSession session, List<String> servers, String backupType, int index) {
        if (index >= servers.size()) {
            session.complete();
            activeSessions.remove(session.getSessionId());
            return;
        }
        
        String serverName = servers.get(index);
        ServerInfo server = plugin.getProxy().getServerInfo(serverName);
        
        if (server == null) {
            session.recordResult(serverName, false, "Server not found", 0, 0);
            executeSequentialBackup(session, servers, backupType, index + 1);
            return;
        }
        
        session.getSender().sendMessage(ChatColor.AQUA + "→ " + serverName + ChatColor.GRAY + " - Starting...");
        sendBackupRequest(server, session.getSessionId(), backupType);
        
        // Wait for response or timeout, then continue to next server
        scheduler.schedule(() -> {
            if (activeSessions.containsKey(session.getSessionId())) {
                executeSequentialBackup(session, servers, backupType, index + 1);
            }
        }, 30, TimeUnit.SECONDS); // 30 seconds per server
    }
    
    /**
     * Execute backups on all servers simultaneously
     */
    private void executeParallelBackup(BackupSession session, List<String> servers, String backupType) {
        for (String serverName : servers) {
            ServerInfo server = plugin.getProxy().getServerInfo(serverName);
            if (server != null) {
                session.getSender().sendMessage(ChatColor.AQUA + "→ " + serverName + ChatColor.GRAY + " - Starting...");
                sendBackupRequest(server, session.getSessionId(), backupType);
            } else {
                session.recordResult(serverName, false, "Server not found", 0, 0);
            }
        }
        
        // Auto-complete after all responses or timeout
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
    private void sendBackupRequest(ServerInfo server, String sessionId, String backupType) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("BackupRequest");
        out.writeUTF(sessionId);
        out.writeUTF(backupType);
        
        server.sendData(CHANNEL_REQUEST, out.toByteArray());
    }
    
    /**
     * Handle backup responses from backend servers
     */
    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getTag().equals(CHANNEL_RESPONSE)) {
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
                
                // Display result immediately
                CommandSender sender = session.getSender();
                if (success) {
                    if (message.equals("STARTED")) {
                        // Don't show "started" messages
                        return;
                    }
                    long sizeMB = size / 1024 / 1024;
                    sender.sendMessage(ChatColor.GREEN + "✓ " + serverName + 
                        ChatColor.GRAY + " - Completed (" + duration + "ms, " + sizeMB + "MB)");
                } else {
                    sender.sendMessage(ChatColor.RED + "✗ " + serverName + 
                        ChatColor.GRAY + " - " + message);
                }
                
                // Check if all servers completed
                if (session.isComplete()) {
                    session.complete();
                    activeSessions.remove(sessionId);
                }
            }
        }
    }
    
    /**
     * Represents an active backup session
     */
    private static class BackupSession {
        private final String sessionId;
        private final List<String> servers;
        private final CommandSender sender;
        private final Map<String, BackupResult> results;
        private final long startTime;
        
        public BackupSession(String sessionId, List<String> servers, CommandSender sender) {
            this.sessionId = sessionId;
            this.servers = servers;
            this.sender = sender;
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
            
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
            sender.sendMessage(ChatColor.GOLD + "Network Backup Summary");
            sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
            sender.sendMessage(ChatColor.YELLOW + "Session: " + ChatColor.WHITE + sessionId);
            sender.sendMessage(ChatColor.YELLOW + "Total Time: " + ChatColor.WHITE + (totalDuration / 1000) + "s");
            sender.sendMessage(ChatColor.GREEN + "Success: " + successCount + ChatColor.GRAY + " / " + 
                ChatColor.RED + "Failed: " + failCount);
            
            if (failCount > 0) {
                sender.sendMessage("");
                sender.sendMessage(ChatColor.RED + "Failed servers:");
                results.forEach((server, result) -> {
                    if (!result.success) {
                        sender.sendMessage(ChatColor.RED + "  - " + server + ": " + result.message);
                    }
                });
            }
            
            sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════════════════");
        }
        
        public void timeout() {
            sender.sendMessage(ChatColor.RED + "⚠ Backup session timed out!");
            sender.sendMessage(ChatColor.YELLOW + "Completed: " + results.size() + "/" + servers.size());
            complete();
        }
        
        public String getSessionId() { return sessionId; }
        public CommandSender getSender() { return sender; }
    }
    
    private record BackupResult(boolean success, String message, long duration, long size) {}
}

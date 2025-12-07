package com.serverbackup.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.api.BackupResult;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.concurrent.CompletableFuture;

/**
 * Listens for network backup requests from BungeeCord/Velocity proxy
 */
public class NetworkBackupListener implements PluginMessageListener {
    
    private final ServerBackupPlugin plugin;
    private static final String CHANNEL_REQUEST = "serverbackup:request";
    private static final String CHANNEL_RESPONSE = "serverbackup:response";
    
    public NetworkBackupListener(ServerBackupPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Register incoming and outgoing plugin message channels
     */
    public void register() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_REQUEST, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL_RESPONSE);
        plugin.getLogger().info("Network backup listener registered (BungeeCord/Velocity)");
        plugin.getLogger().info("  Listening on channel: " + CHANNEL_REQUEST);
    }
    
    /**
     * Unregister plugin message channels
     */
    public void unregister() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_REQUEST);
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, CHANNEL_RESPONSE);
    }
    
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL_REQUEST)) {
            return;
        }
        
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        
        // Handle backup request
        if (subChannel.equals("BackupRequest")) {
            String requestId = in.readUTF();
            String backupType = in.readUTF(); // "world", "full", or "auto"
            
            plugin.getLogger().info("Received network backup request: " + requestId + " (type: " + backupType + ")");
            
            // Execute backup asynchronously
            executeNetworkBackup(player, requestId, backupType);
        }
        // Handle status check
        else if (subChannel.equals("StatusRequest")) {
            String requestId = in.readUTF();
            sendStatusResponse(player, requestId);
        }
    }
    
    /**
     * Execute backup and send result back to proxy
     */
    private void executeNetworkBackup(Player player, String requestId, String backupType) {
        String serverName = plugin.getServer().getServerName();
        
        // Check if backup API is available
        if (plugin.getAPI() == null) {
            sendBackupResponse(player, requestId, false, "Public API disabled on this server", 0, 0);
            return;
        }
        
        // Send "started" status
        sendBackupResponse(player, requestId, true, "STARTED", 0, 0);
        
        // Execute backup via API
        CompletableFuture<BackupResult> future;
        if (backupType.equalsIgnoreCase("world")) {
            future = plugin.getAPI().createBackup(
                plugin.getAPI().newBackupOptions()
                    .type(com.serverbackup.api.BackupType.WORLD)
                    .async(true)
                    .build()
            );
        } else {
            future = plugin.getAPI().createBackup(
                plugin.getAPI().newBackupOptions()
                    .type(com.serverbackup.api.BackupType.FULL)
                    .async(true)
                    .build()
            );
        }
        
        // Handle result
        future.whenComplete((result, error) -> {
            if (error != null) {
                plugin.getLogger().warning("Network backup failed: " + error.getMessage());
                sendBackupResponse(player, requestId, false, "ERROR: " + error.getMessage(), 0, 0);
            } else if (result.isSuccess()) {
                long duration = result.getDuration();
                long size = result.getFile() != null ? result.getFile().length() : 0;
                plugin.getLogger().info("Network backup completed: " + requestId + " (" + duration + "ms, " + (size/1024/1024) + "MB)");
                sendBackupResponse(player, requestId, true, "SUCCESS", duration, size);
            } else {
                plugin.getLogger().warning("Network backup failed: " + result.getError());
                sendBackupResponse(player, requestId, false, "FAILED: " + result.getError(), result.getDuration(), 0);
            }
        });
    }
    
    /**
     * Send backup result back to proxy
     */
    private void sendBackupResponse(Player player, String requestId, boolean success, String message, long duration, long size) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("BackupResponse");
        out.writeUTF(requestId);
        out.writeUTF(plugin.getServer().getServerName());
        out.writeBoolean(success);
        out.writeUTF(message);
        out.writeLong(duration);
        out.writeLong(size);
        
        // Send to any online player (BungeeCord/Velocity requirement)
        if (player != null && player.isOnline()) {
            player.sendPluginMessage(plugin, CHANNEL_RESPONSE, out.toByteArray());
        } else {
            // Find any online player
            Player anyPlayer = plugin.getServer().getOnlinePlayers().stream().findFirst().orElse(null);
            if (anyPlayer != null) {
                anyPlayer.sendPluginMessage(plugin, CHANNEL_RESPONSE, out.toByteArray());
            } else {
                plugin.getLogger().warning("Cannot send backup response: No online players!");
            }
        }
    }
    
    /**
     * Send server status (backup count, last backup time, etc.)
     */
    private void sendStatusResponse(Player player, String requestId) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("StatusResponse");
        out.writeUTF(requestId);
        out.writeUTF(plugin.getServer().getServerName());
        out.writeBoolean(plugin.getAPI() != null); // API available
        out.writeBoolean(plugin.getBackupService().isBackupInProgress()); // Currently running
        out.writeInt(plugin.getAPI() != null ? plugin.getAPI().listBackups(null).join().size() : 0); // Backup count
        
        if (player != null && player.isOnline()) {
            player.sendPluginMessage(plugin, CHANNEL_RESPONSE, out.toByteArray());
        }
    }
}

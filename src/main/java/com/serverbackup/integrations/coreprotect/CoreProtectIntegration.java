package com.serverbackup.integrations.coreprotect;

import com.serverbackup.ServerBackupPlugin;
import com.serverbackup.api.BackupAPI;
import com.serverbackup.api.BackupOptions;
import com.serverbackup.api.BackupType;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * CoreProtect integration for smart rollback system
 * 
 * Features:
 * - Detect large rollbacks (threshold-based)
 * - Auto-create backup before large rollbacks
 * - Suggest full restore for massive griefs
 * - Track rollback history
 * 
 * Config:
 * integrations:
 *   coreprotect:
 *     enabled: true
 *     auto-restore-threshold: 10000  # blocks
 *     backup-before-rollback: true
 */
public class CoreProtectIntegration {
    
    private final ServerBackupPlugin plugin;
    private final BackupAPI backupAPI;
    private CoreProtectAPI coreProtectAPI;
    private boolean enabled;
    private int autoRestoreThreshold;
    private boolean backupBeforeRollback;
    
    public CoreProtectIntegration(ServerBackupPlugin plugin) {
        this.plugin = plugin;
        this.backupAPI = ServerBackupPlugin.getAPI();
        this.enabled = plugin.getConfig().getBoolean("integrations.coreprotect.enabled", false);
        this.autoRestoreThreshold = plugin.getConfig().getInt("integrations.coreprotect.auto-restore-threshold", 10000);
        this.backupBeforeRollback = plugin.getConfig().getBoolean("integrations.coreprotect.backup-before-rollback", true);
    }
    
    /**
     * Initialize CoreProtect API
     * @return true if successfully initialized
     */
    public boolean initialize() {
        if (!enabled) {
            plugin.getLogger().info("CoreProtect integration is disabled");
            return false;
        }
        
        Plugin coreProtectPlugin = Bukkit.getPluginManager().getPlugin("CoreProtect");
        if (coreProtectPlugin == null || !(coreProtectPlugin instanceof CoreProtect)) {
            plugin.getLogger().warning("CoreProtect plugin not found! Integration disabled.");
            enabled = false;
            return false;
        }
        
        CoreProtectAPI api = ((CoreProtect) coreProtectPlugin).getAPI();
        if (!api.isEnabled()) {
            plugin.getLogger().warning("CoreProtect API is not enabled! Integration disabled.");
            enabled = false;
            return false;
        }
        
        // Check API version
        if (api.APIVersion() < 9) {
            plugin.getLogger().warning("CoreProtect API version 9+ required! Current: " + api.APIVersion());
            enabled = false;
            return false;
        }
        
        this.coreProtectAPI = api;
        plugin.getLogger().info("CoreProtect integration enabled!");
        plugin.getLogger().info("  Auto-restore threshold: " + autoRestoreThreshold + " blocks");
        plugin.getLogger().info("  Backup before rollback: " + backupBeforeRollback);
        
        return true;
    }
    
    /**
     * Check if integration is enabled and initialized
     */
    public boolean isEnabled() {
        return enabled && coreProtectAPI != null;
    }
    
    /**
     * Perform smart rollback with backup support
     * 
     * @param sender Command sender
     * @param time Time in seconds to rollback
     * @param username Target username (null for all)
     * @param radius Radius around location (0 for global)
     * @param location Center location (null for global)
     * @return CompletableFuture with rollback result
     */
    public CompletableFuture<RollbackResult> performSmartRollback(
            CommandSender sender,
            int time,
            String username,
            int radius,
            Location location) {
        
        CompletableFuture<RollbackResult> future = new CompletableFuture<>();
        
        if (!isEnabled()) {
            future.completeExceptionally(new IllegalStateException("CoreProtect integration not enabled"));
            return future;
        }
        
        // Step 1: Analyze the rollback size (async)
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Get lookup data to estimate size
                // CoreProtect API: performLookup(time, users, blocks, excludeBlocks, actions, excludeActions, radius, location)
                List<String[]> lookupData = coreProtectAPI.performLookup(
                    time,
                    username != null ? List.of(username) : null,
                    null, // blocks
                    null, // excludeBlocks
                    null, // actions
                    null, // excludeActions
                    radius,
                    location
                );
                
                int estimatedBlocks = lookupData != null ? lookupData.size() : 0;
                
                sender.sendMessage("§e[SmartRollback] §7Analyzing grief...");
                sender.sendMessage("§7Estimated affected blocks: §e" + estimatedBlocks);
                
                // Step 2: Decide strategy based on size
                if (estimatedBlocks >= autoRestoreThreshold) {
                    // MASSIVE GRIEF - Suggest full restore
                    handleMassiveGrief(sender, estimatedBlocks, future);
                } else if (estimatedBlocks >= 1000 && backupBeforeRollback) {
                    // LARGE GRIEF - Backup first, then rollback
                    handleLargeGrief(sender, time, username, radius, location, estimatedBlocks, future);
                } else {
                    // SMALL GRIEF - Direct rollback
                    handleSmallGrief(sender, time, username, radius, location, estimatedBlocks, future);
                }
                
            } catch (Exception e) {
                sender.sendMessage("§c[SmartRollback] Error: " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    /**
     * Handle massive grief (suggest full restore)
     */
    private void handleMassiveGrief(CommandSender sender, int blocks, CompletableFuture<RollbackResult> future) {
        sender.sendMessage("§c╔════════════════════════════════════════╗");
        sender.sendMessage("§c║     ⚠ MASSIVE GRIEF DETECTED ⚠       ║");
        sender.sendMessage("§c╚════════════════════════════════════════╝");
        sender.sendMessage("");
        sender.sendMessage("§7Affected blocks: §c" + blocks + " §7(Threshold: §e" + autoRestoreThreshold + "§7)");
        sender.sendMessage("");
        sender.sendMessage("§eRecommendation: §6Full server restore from backup");
        sender.sendMessage("§7CoreProtect rollback may take too long and cause lag.");
        sender.sendMessage("");
        sender.sendMessage("§aOptions:");
        sender.sendMessage("§7  1. §e/backuplist §7- View available backups");
        sender.sendMessage("§7  2. §e/backuprestore <name> §7- Restore from backup");
        sender.sendMessage("§7  3. §c/co rollback §7- Force CoreProtect rollback (not recommended)");
        sender.sendMessage("");
        
        RollbackResult result = new RollbackResult(
            RollbackStrategy.FULL_RESTORE,
            blocks,
            0,
            "Massive grief detected - full restore recommended"
        );
        future.complete(result);
    }
    
    /**
     * Handle large grief (backup first, then rollback)
     */
    private void handleLargeGrief(CommandSender sender, int time, String username, 
                                  int radius, Location location, int blocks,
                                  CompletableFuture<RollbackResult> future) {
        
        sender.sendMessage("§e[SmartRollback] §7Large grief detected. Creating safety backup first...");
        
        // Create backup before rollback
        BackupOptions options = BackupOptions.builder()
            .setType(BackupType.WORLD)
            .setAsync(true)
            .addMetadata("reason", "pre_rollback")
            .addMetadata("blocks_affected", blocks)
            .addMetadata("rollback_user", username != null ? username : "all")
            .build();
        
        backupAPI.createBackup(options).thenAccept(backupResult -> {
            if (!backupResult.isSuccess()) {
                sender.sendMessage("§c[SmartRollback] Backup failed! Aborting rollback for safety.");
                sender.sendMessage("§cError: " + backupResult.getErrorMessage());
                future.complete(new RollbackResult(
                    RollbackStrategy.BACKUP_FAILED,
                    blocks,
                    0,
                    "Backup failed: " + backupResult.getErrorMessage()
                ));
                return;
            }
            
            sender.sendMessage("§a[SmartRollback] ✓ Safety backup created: " + backupResult.getBackupFile().getName());
            sender.sendMessage("§e[SmartRollback] Performing rollback...");
            
            // Perform actual rollback on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // CoreProtect API performRollback returns void in API 22.2
                coreProtectAPI.performRollback(
                    time,
                    username != null ? List.of(username) : null,
                    null, // blocks
                    null, // excludeBlocks
                    null, // actions
                    null, // excludeActions
                    radius,
                    location
                );
                
                // Assume success if no exception thrown
                sender.sendMessage("§a[SmartRollback] ✓ Rollback completed successfully!");
                sender.sendMessage("§7Safety backup: §e" + backupResult.getBackupFile().getName());
                
                future.complete(new RollbackResult(
                    RollbackStrategy.BACKUP_THEN_ROLLBACK,
                    blocks,
                    blocks,
                    "Success - backup: " + backupResult.getBackupFile().getName()
                ));
            });
        });
    }
    
    /**
     * Handle small grief (direct rollback)
     */
    private void handleSmallGrief(CommandSender sender, int time, String username,
                                  int radius, Location location, int blocks,
                                  CompletableFuture<RollbackResult> future) {
        
        sender.sendMessage("§a[SmartRollback] §7Performing direct rollback...");
        
        // Perform rollback on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // CoreProtect API performRollback returns void in API 22.2
            coreProtectAPI.performRollback(
                time,
                username != null ? List.of(username) : null,
                null,
                null,
                null,
                null,
                radius,
                location
            );
            
            // Assume success if no exception thrown
            sender.sendMessage("§a[SmartRollback] ✓ Rollback completed!");
            future.complete(new RollbackResult(
                RollbackStrategy.DIRECT_ROLLBACK,
                blocks,
                blocks,
                "Success"
            ));
        });
    }
    
    /**
     * Get CoreProtect API instance
     */
    public CoreProtectAPI getAPI() {
        return coreProtectAPI;
    }
    
    /**
     * Rollback result data
     */
    public static class RollbackResult {
        private final RollbackStrategy strategy;
        private final int blocksAnalyzed;
        private final int blocksRolledBack;
        private final String message;
        
        public RollbackResult(RollbackStrategy strategy, int blocksAnalyzed, 
                            int blocksRolledBack, String message) {
            this.strategy = strategy;
            this.blocksAnalyzed = blocksAnalyzed;
            this.blocksRolledBack = blocksRolledBack;
            this.message = message;
        }
        
        public RollbackStrategy getStrategy() { return strategy; }
        public int getBlocksAnalyzed() { return blocksAnalyzed; }
        public int getBlocksRolledBack() { return blocksRolledBack; }
        public String getMessage() { return message; }
    }
    
    /**
     * Rollback strategies
     */
    public enum RollbackStrategy {
        DIRECT_ROLLBACK,           // Small grief - direct CO rollback
        BACKUP_THEN_ROLLBACK,      // Large grief - backup first
        FULL_RESTORE,              // Massive grief - suggest restore
        BACKUP_FAILED,             // Backup failed
        ROLLBACK_FAILED            // Rollback failed
    }
}

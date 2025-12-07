package com.serverbackup.integrations.luckperms;

import com.serverbackup.ServerBackupPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LuckPerms integration for advanced permissions and rate limiting
 * 
 * Features:
 * - Group-based rate limiting (backups per hour)
 * - Track backup usage per player
 * - Permission-based quotas
 * 
 * Config:
 * integrations:
 *   luckperms:
 *     enabled: true
 *     rate-limits:
 *       admin: 0      # unlimited
 *       mod: 5        # 5 backups/hour
 *       default: 1    # 1 backup/hour
 */
public class LuckPermsIntegration {
    
    private final ServerBackupPlugin plugin;
    private LuckPerms luckPerms;
    private boolean enabled;
    
    // Track backup usage: UUID -> [timestamp1, timestamp2, ...]
    private final Map<UUID, java.util.List<Long>> backupUsage = new ConcurrentHashMap<>();
    
    public LuckPermsIntegration(ServerBackupPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("integrations.luckperms.enabled", false);
    }
    
    /**
     * Initialize LuckPerms API
     */
    public boolean initialize() {
        if (!enabled) {
            plugin.getLogger().info("LuckPerms integration is disabled");
            return false;
        }
        
        try {
            luckPerms = LuckPermsProvider.get();
            plugin.getLogger().info("LuckPerms integration enabled!");
            plugin.getLogger().info("  Rate limiting active");
            
            // Log configured limits
            Map<String, Object> limits = plugin.getConfig().getConfigurationSection("integrations.luckperms.rate-limits").getValues(false);
            for (Map.Entry<String, Object> entry : limits.entrySet()) {
                int limit = (int) entry.getValue();
                String status = limit == 0 ? "unlimited" : limit + " backups/hour";
                plugin.getLogger().info("    " + entry.getKey() + ": " + status);
            }
            
            return true;
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("LuckPerms not found! Integration disabled.");
            enabled = false;
            return false;
        }
    }
    
    /**
     * Check if integration is enabled
     */
    public boolean isEnabled() {
        return enabled && luckPerms != null;
    }
    
    /**
     * Check if player can create a backup (rate limit check)
     * 
     * @param player Player to check
     * @return RateLimitResult with status and message
     */
    public RateLimitResult checkRateLimit(Player player) {
        if (!isEnabled()) {
            return RateLimitResult.allowed();
        }
        
        // Admins bypass rate limits
        if (player.hasPermission("serverbackup.bypass.ratelimit")) {
            return RateLimitResult.allowed();
        }
        
        // Get player's primary group
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return RateLimitResult.allowed(); // Fallback if user not loaded
        }
        
        String primaryGroup = user.getPrimaryGroup();
        
        // Get rate limit for this group
        int limit = plugin.getConfig().getInt("integrations.luckperms.rate-limits." + primaryGroup, 
                   plugin.getConfig().getInt("integrations.luckperms.rate-limits.default", 1));
        
        // 0 = unlimited
        if (limit == 0) {
            return RateLimitResult.allowed();
        }
        
        // Check usage in last hour
        UUID playerId = player.getUniqueId();
        long oneHourAgo = System.currentTimeMillis() - 3600000; // 1 hour in ms
        
        backupUsage.putIfAbsent(playerId, new java.util.ArrayList<>());
        java.util.List<Long> usage = backupUsage.get(playerId);
        
        // Clean old entries
        usage.removeIf(timestamp -> timestamp < oneHourAgo);
        
        // Check if over limit
        if (usage.size() >= limit) {
            long oldestBackup = usage.isEmpty() ? System.currentTimeMillis() : usage.get(0);
            long resetTime = oldestBackup + 3600000;
            long minutesUntilReset = (resetTime - System.currentTimeMillis()) / 60000;
            
            return RateLimitResult.denied(
                "Rate limit exceeded! You can create " + limit + " backup(s) per hour.\n" +
                "Your limit resets in " + minutesUntilReset + " minute(s).\n" +
                "Group: " + primaryGroup
            );
        }
        
        return RateLimitResult.allowed();
    }
    
    /**
     * Record a backup creation for rate limiting
     * 
     * @param player Player who created the backup
     */
    public void recordBackup(Player player) {
        if (!isEnabled()) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        backupUsage.putIfAbsent(playerId, new java.util.ArrayList<>());
        backupUsage.get(playerId).add(System.currentTimeMillis());
    }
    
    /**
     * Get player's remaining backup quota for this hour
     * 
     * @param player Player to check
     * @return Remaining backups, or -1 for unlimited
     */
    public int getRemainingQuota(Player player) {
        if (!isEnabled()) {
            return -1;
        }
        
        if (player.hasPermission("serverbackup.bypass.ratelimit")) {
            return -1; // Unlimited
        }
        
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return -1;
        }
        
        String primaryGroup = user.getPrimaryGroup();
        int limit = plugin.getConfig().getInt("integrations.luckperms.rate-limits." + primaryGroup,
                   plugin.getConfig().getInt("integrations.luckperms.rate-limits.default", 1));
        
        if (limit == 0) {
            return -1; // Unlimited
        }
        
        UUID playerId = player.getUniqueId();
        long oneHourAgo = System.currentTimeMillis() - 3600000;
        
        backupUsage.putIfAbsent(playerId, new java.util.ArrayList<>());
        java.util.List<Long> usage = backupUsage.get(playerId);
        usage.removeIf(timestamp -> timestamp < oneHourAgo);
        
        return Math.max(0, limit - usage.size());
    }
    
    /**
     * Get player's primary group name
     * 
     * @param player Player
     * @return Group name or "default"
     */
    public String getPlayerGroup(Player player) {
        if (!isEnabled()) {
            return "default";
        }
        
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        return user != null ? user.getPrimaryGroup() : "default";
    }
    
    /**
     * Rate limit result
     */
    public static class RateLimitResult {
        private final boolean allowed;
        private final String message;
        
        private RateLimitResult(boolean allowed, String message) {
            this.allowed = allowed;
            this.message = message;
        }
        
        public boolean isAllowed() {
            return allowed;
        }
        
        public String getMessage() {
            return message;
        }
        
        public static RateLimitResult allowed() {
            return new RateLimitResult(true, null);
        }
        
        public static RateLimitResult denied(String message) {
            return new RateLimitResult(false, message);
        }
    }
}

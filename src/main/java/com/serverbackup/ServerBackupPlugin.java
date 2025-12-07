package com.serverbackup;

import com.serverbackup.api.BackupAPI;
import com.serverbackup.commands.BackupCommand;
import com.serverbackup.commands.BackupDeleteCommand;
import com.serverbackup.commands.BackupListCommand;
import com.serverbackup.commands.BackupRestoreCommand;
import com.serverbackup.integrations.coreprotect.CoreProtectIntegration;
import com.serverbackup.integrations.coreprotect.SmartRollbackCommand;
import com.serverbackup.integrations.luckperms.LuckPermsIntegration;
import com.serverbackup.integrations.placeholderapi.ServerBackupExpansion;
import com.serverbackup.service.BackupService;
import com.serverbackup.service.BackupAPIImpl;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ServerBackupPlugin extends JavaPlugin {
    
    private static ServerBackupPlugin instance;
    private BackupService backupService;
    private BackupAPIImpl backupAPI;
    private CoreProtectIntegration coreProtectIntegration;
    private LuckPermsIntegration luckPermsIntegration;
    private ServerBackupExpansion placeholderExpansion;
    private int autoBackupTaskId = -1;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize backup directory
        String backupPath = getConfig().getString("backup.directory", "backups");
        File backupDir = new File(getServer().getWorldContainer(), backupPath);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        // Initialize backup service
        backupService = new BackupService(this);
        
        // Initialize public API (if enabled)
        if (getConfig().getBoolean("features.public-api.enabled", true)) {
            backupAPI = new BackupAPIImpl(this, backupService);
            getLogger().info("Public API enabled - other plugins can use BackupAPI");
        } else {
            getLogger().info("Public API disabled");
        }
        
        // Initialize integrations
        initializeIntegrations();
        
        // Register commands
        registerCommands();
        
        // Start auto backup if enabled (disabled by default)
        if (getConfig().getBoolean("backup.auto-backup-enabled", false)) {
            startAutoBackup();
        } else {
            getLogger().info("Auto-backup is disabled. Use /backup command to create backups manually.");
        }
        
        // Log feature status
        logFeatureStatus();
        
        getLogger().info("ServerBackupPlugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Cancel auto backup task
        if (autoBackupTaskId != -1) {
            getServer().getScheduler().cancelTask(autoBackupTaskId);
        }
        
        // Unregister PlaceholderAPI expansion
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        
        getLogger().info("ServerBackupPlugin has been disabled!");
    }
    
    private void registerCommands() {
        BackupTabCompleter tabCompleter = new com.serverbackup.commands.BackupTabCompleter(backupService);
        
        getCommand("backup").setExecutor(new BackupCommand(this, backupService));
        getCommand("backup").setTabCompleter(tabCompleter);
        
        getCommand("backuplist").setExecutor(new BackupListCommand(this, backupService));
        
        getCommand("backuprestore").setExecutor(new BackupRestoreCommand(this, backupService));
        getCommand("backuprestore").setTabCompleter(tabCompleter);
        
        getCommand("backupdelete").setExecutor(new BackupDeleteCommand(this, backupService));
        getCommand("backupdelete").setTabCompleter(tabCompleter);
        
        // Register integration commands
        if (coreProtectIntegration != null && coreProtectIntegration.isEnabled()) {
            getCommand("smartrollback").setExecutor(new SmartRollbackCommand(this, coreProtectIntegration));
        }
    }
    
    private void initializeIntegrations() {
        // CoreProtect Integration
        coreProtectIntegration = new CoreProtectIntegration(this);
        if (getConfig().getBoolean("integrations.coreprotect.enabled", false)) {
            coreProtectIntegration.initialize();
        }
        
        // LuckPerms Integration
        luckPermsIntegration = new LuckPermsIntegration(this);
        if (getConfig().getBoolean("integrations.luckperms.enabled", false)) {
            luckPermsIntegration.initialize();
            if (luckPermsIntegration.isEnabled()) {
                getLogger().info("LuckPerms rate limiting enabled!");
                getLogger().info("  Group-based backup quotas active");
            }
        }
        
        // PlaceholderAPI Integration
        if (getConfig().getBoolean("integrations.placeholderapi.enabled", false)) {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                placeholderExpansion = new ServerBackupExpansion(this);
                if (placeholderExpansion.register()) {
                    getLogger().info("PlaceholderAPI expansion registered!");
                    getLogger().info("  Available placeholders: %serverbackup_*");
                } else {
                    getLogger().warning("Failed to register PlaceholderAPI expansion");
                }
            } else {
                getLogger().warning("PlaceholderAPI not found! Expansion disabled.");
            }
        }
    }
    
    private void startAutoBackup() {
        int interval = getConfig().getInt("backup.auto-backup-interval", 720);
        long intervalTicks = interval * 60L * 20L; // Convert minutes to ticks
        
        autoBackupTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            getLogger().info("Starting automatic backup...");
            backupService.createBackup(null);
        }, intervalTicks, intervalTicks);
        
        int timesPerDay = 1440 / interval; // 1440 minutes in a day
        getLogger().info("Auto-backup scheduled every " + interval + " minutes (" + timesPerDay + "x daily)");
    }
    
    public void restartAutoBackup() {
        if (autoBackupTaskId != -1) {
            getServer().getScheduler().cancelTask(autoBackupTaskId);
            autoBackupTaskId = -1;
        }
        if (getConfig().getBoolean("backup.auto-backup-enabled", false)) {
            startAutoBackup();
        }
    }
    
    public BackupService getBackupService() {
        return backupService;
    }
    
    /**
     * Get the public API instance
     * @return BackupAPI or null if disabled in config
     */
    public static BackupAPI getAPI() {
        return instance != null ? instance.backupAPI : null;
    }
    
    /**
     * Get plugin instance
     */
    public static ServerBackupPlugin getInstance() {
        return instance;
    }
    
    /**
     * Log which features are enabled
     */
    private void logFeatureStatus() {
        getLogger().info("═══════════════════════════════════════════");
        getLogger().info("Feature Status:");
        getLogger().info("  Public API: " + (getConfig().getBoolean("features.public-api.enabled", true) ? "✓" : "✗"));
        getLogger().info("  Events: " + (getConfig().getBoolean("features.events.enabled", true) ? "✓" : "✗"));
        getLogger().info("  HTTP API: " + (getConfig().getBoolean("features.http-api.enabled", false) ? "✓" : "✗"));
        getLogger().info("Integrations:");
        String cpStatus = (coreProtectIntegration != null && coreProtectIntegration.isEnabled()) ? "✓ Active" : 
                         getConfig().getBoolean("integrations.coreprotect.enabled", false) ? "✗ Failed" : "✗ Disabled";
        getLogger().info("  CoreProtect: " + cpStatus);
        getLogger().info("  WorldGuard: " + (getConfig().getBoolean("integrations.worldguard.enabled", false) ? "✓" : "✗"));
        getLogger().info("  LuckPerms: " + (getConfig().getBoolean("integrations.luckperms.enabled", false) ? "✓" : "✗"));
        getLogger().info("  PlaceholderAPI: " + (getConfig().getBoolean("integrations.placeholderapi.enabled", false) ? "✓" : "✗"));
        getLogger().info("  Network Mode: " + (getConfig().getBoolean("integrations.network.enabled", false) ? "✓" : "✗"));
        getLogger().info("═══════════════════════════════════════════");
    }
    
    public CoreProtectIntegration getCoreProtectIntegration() {
        return coreProtectIntegration;
    }
    
    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }
}

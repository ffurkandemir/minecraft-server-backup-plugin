package com.serverbackup;

import com.serverbackup.commands.BackupCommand;
import com.serverbackup.commands.BackupDeleteCommand;
import com.serverbackup.commands.BackupListCommand;
import com.serverbackup.commands.BackupRestoreCommand;
import com.serverbackup.service.BackupService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ServerBackupPlugin extends JavaPlugin {
    
    private BackupService backupService;
    private int autoBackupTaskId = -1;
    
    @Override
    public void onEnable() {
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
        
        // Register commands
        registerCommands();
        
        // Start auto backup if enabled (disabled by default)
        if (getConfig().getBoolean("backup.auto-backup-enabled", false)) {
            startAutoBackup();
        } else {
            getLogger().info("Auto-backup is disabled. Use /backup command to create backups manually.");
        }
        
        getLogger().info("ServerBackupPlugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Cancel auto backup task
        if (autoBackupTaskId != -1) {
            getServer().getScheduler().cancelTask(autoBackupTaskId);
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
}

# ServerBackupPlugin API - Example Integration Plugin

This is a minimal example showing how to use the ServerBackupPlugin API in your own plugin.

## Plugin Files

### plugin.yml
```yaml
name: BackupAPIExample
version: 1.0.0
main: com.example.backupexample.BackupExamplePlugin
api-version: '1.21'
depend: [ServerBackupPlugin]  # Required dependency
author: YourName
description: Example plugin demonstrating ServerBackupPlugin API usage

commands:
  custombackup:
    description: Create a custom backup with API
    usage: /custombackup [world]
```

### BackupExamplePlugin.java
```java
package com.example.backupexample;

import com.serverbackup.api.*;
import com.serverbackup.api.events.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class BackupExamplePlugin extends JavaPlugin implements Listener {
    
    private BackupAPI backupAPI;
    
    @Override
    public void onEnable() {
        // Get the API instance
        backupAPI = com.serverbackup.ServerBackupPlugin.getAPI();
        
        if (backupAPI == null) {
            getLogger().severe("ServerBackupPlugin API is not available!");
            getLogger().severe("Make sure ServerBackupPlugin is installed and features.public-api.enabled is true");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("BackupAPIExample enabled!");
        getLogger().info("Total backups: " + backupAPI.getBackupCount());
        getLogger().info("Backup directory: " + backupAPI.getBackupDirectory());
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("custombackup")) {
            // Example: Create backup with custom options
            BackupOptions.Builder builder = BackupOptions.builder()
                .setType(BackupType.WORLD)
                .setCompression(true)
                .setAsync(true)
                .setNotifyPlayers(false)  // Silent backup
                .addMetadata("triggered_by", sender.getName())
                .addMetadata("reason", "custom_command");
            
            // Add specific world if provided
            if (args.length > 0) {
                builder.addWorld(args[0]);
                sender.sendMessage(ChatColor.GREEN + "Creating backup for world: " + args[0]);
            } else {
                sender.sendMessage(ChatColor.GREEN + "Creating backup for all worlds...");
            }
            
            BackupOptions options = builder.build();
            
            // Create backup asynchronously
            backupAPI.createBackup(options).thenAccept(result -> {
                if (result.isSuccess()) {
                    String message = ChatColor.GREEN + "✓ Backup completed in " + 
                        result.getDuration() + "ms";
                    sender.sendMessage(message);
                    sender.sendMessage(ChatColor.GRAY + "File: " + result.getBackupFile().getName());
                    sender.sendMessage(ChatColor.GRAY + "Size: " + 
                        formatSize(result.getFileSize()));
                } else {
                    sender.sendMessage(ChatColor.RED + "✗ Backup failed: " + 
                        result.getErrorMessage());
                }
            });
            
            return true;
        }
        return false;
    }
    
    // ═══════════════════════════════════════════════════════════════════
    //                        EVENT LISTENERS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Listen to backup start events
     * You can cancel backups based on custom logic
     */
    @EventHandler
    public void onBackupStart(BackupStartEvent event) {
        getLogger().info("Backup starting: " + event.getOptions().getType());
        
        // Example: Don't allow full backups during peak hours
        if (event.getOptions().getType() == BackupType.FULL) {
            int hour = java.time.LocalTime.now().getHour();
            if (hour >= 18 && hour <= 22) {  // 6 PM - 10 PM
                event.setCancelReason("Full backups are disabled during peak hours (6PM-10PM)");
                getLogger().warning("Blocked full backup during peak hours");
            }
        }
        
        // Example: Rate limiting per player
        Object triggeredBy = event.getOptions().getMetadata().get("triggered_by");
        if (triggeredBy instanceof String) {
            String playerName = (String) triggeredBy;
            // Implement your rate limiting logic here
            getLogger().info("Backup triggered by: " + playerName);
        }
    }
    
    /**
     * Listen to backup completion
     * Good for triggering follow-up actions
     */
    @EventHandler
    public void onBackupComplete(BackupCompleteEvent event) {
        BackupResult result = event.getResult();
        
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("Backup completed!");
        getLogger().info("File: " + result.getBackupFile().getName());
        getLogger().info("Duration: " + result.getDuration() + "ms");
        getLogger().info("Size: " + formatSize(result.getFileSize()));
        getLogger().info("═══════════════════════════════════════");
        
        // Example: Upload to cloud storage
        // uploadToCloud(result.getBackupFile());
        
        // Example: Send Discord webhook
        // sendDiscordNotification("Backup completed: " + result.getBackupFile().getName());
        
        // Example: Clean old backups
        cleanOldBackups();
    }
    
    /**
     * Listen to backup failures
     * Good for alerts and recovery
     */
    @EventHandler
    public void onBackupFail(BackupFailEvent event) {
        Throwable error = event.getError();
        
        getLogger().severe("═══════════════════════════════════════");
        getLogger().severe("Backup FAILED!");
        getLogger().severe("Error: " + error.getMessage());
        getLogger().severe("Retry count: " + event.getRetryCount());
        getLogger().severe("═══════════════════════════════════════");
        
        // Example: Send alert to admins
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.hasPermission("backup.admin.alerts")) {
                player.sendMessage(ChatColor.RED + "⚠ Backup failed: " + error.getMessage());
            }
        }
        
        // Example: Retry backup after 5 minutes
        if (event.getRetryCount() < 3) {
            getServer().getScheduler().runTaskLater(this, () -> {
                getLogger().info("Retrying backup...");
                backupAPI.createBackup(event.getOptions());
            }, 20 * 60 * 5);  // 5 minutes
        }
    }
    
    /**
     * Listen to backup deletions
     * You can prevent deletion of important backups
     */
    @EventHandler
    public void onBackupDelete(BackupDeleteEvent event) {
        String fileName = event.getBackupFile().getName();
        
        // Example: Protect recent backups
        long age = System.currentTimeMillis() - event.getBackupFile().lastModified();
        long oneHour = 3600000;
        
        if (age < oneHour) {
            event.setCancelReason("Cannot delete backups less than 1 hour old");
            getLogger().warning("Blocked deletion of recent backup: " + fileName);
            return;
        }
        
        // Example: Protect backups with specific names
        if (fileName.contains("important") || fileName.contains("manual")) {
            event.setCancelReason("This backup is marked as important");
            getLogger().warning("Blocked deletion of important backup: " + fileName);
            return;
        }
        
        getLogger().info("Backup deleted: " + fileName);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    //                        UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Example: Clean old backups using API
     */
    private void cleanOldBackups() {
        // Keep only last 5 backups
        while (backupAPI.getBackupCount() > 5) {
            java.util.List<java.io.File> backups = backupAPI.listBackups();
            if (!backups.isEmpty()) {
                java.io.File oldest = backups.get(backups.size() - 1);
                backupAPI.deleteBackup(oldest);
                getLogger().info("Cleaned old backup: " + oldest.getName());
            } else {
                break;
            }
        }
    }
    
    /**
     * Format file size
     */
    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
```

## Advanced Examples

### Example 1: Filter Backups by Date
```java
// Get backups from last 7 days
BackupFilter filter = BackupFilter.builder()
    .setAfter(Instant.now().minus(7, ChronoUnit.DAYS))
    .setType(BackupType.WORLD)
    .build();

List<File> recentBackups = backupAPI.listBackups(filter);
```

### Example 2: Custom Backup Metadata
```java
BackupOptions options = BackupOptions.builder()
    .setType(BackupType.WORLD)
    .addWorld("world")
    .addMetadata("version", "1.21.1")
    .addMetadata("players", Bukkit.getOnlinePlayers().size())
    .addMetadata("reason", "pre-update")
    .setCustomName("before-update-1.21.1")
    .build();

backupAPI.createBackup(options);
```

### Example 3: Check Backup Status
```java
if (backupAPI.isBackupInProgress()) {
    sender.sendMessage("A backup is already running!");
    sender.sendMessage("Active backups: " + backupAPI.getActiveBackupCount());
} else {
    backupAPI.createBackup();
}
```

### Example 4: Get Backup Info
```java
BackupInfo info = backupAPI.getBackupInfo("backup-2025-12-07_12-30-00.zip");
if (info != null) {
    sender.sendMessage("Backup: " + info.getName());
    sender.sendMessage("Type: " + info.getType());
    sender.sendMessage("Size: " + info.getFormattedSize());
    sender.sendMessage("Created: " + info.getCreatedAt());
    sender.sendMessage("Worlds: " + String.join(", ", info.getWorldsIncluded()));
}
```

## Building

```bash
mvn clean package
```

## Dependencies (pom.xml)

```xml
<dependencies>
    <dependency>
        <groupId>org.spigotmc</groupId>
        <artifactId>spigot-api</artifactId>
        <version>1.21.1-R0.1-SNAPSHOT</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- ServerBackupPlugin API -->
    <dependency>
        <groupId>com.serverbackup</groupId>
        <artifactId>server-backup-plugin</artifactId>
        <version>1.0.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## Notes

- Always check if `BackupAPI.getAPI()` returns null before using it
- Make sure ServerBackupPlugin is listed in your `plugin.yml` as a dependency
- The API is fully thread-safe and can be used from any thread
- All backup operations return `CompletableFuture` for async handling
- Events are fired asynchronously (except delete event which is sync)

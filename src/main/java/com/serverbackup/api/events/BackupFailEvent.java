package com.serverbackup.api.events;

import com.serverbackup.api.BackupOptions;
import com.serverbackup.api.BackupResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a backup operation fails
 * 
 * This event is fired asynchronously when an error occurs during backup.
 * 
 * Example usage:
 * <pre>
 * {@literal @}EventHandler
 * public void onBackupFail(BackupFailEvent event) {
 *     Throwable error = event.getResult().getError();
 *     
 *     getLogger().severe("Backup failed: " + error.getMessage());
 *     
 *     // Example: Send alert to Discord
 *     discordWebhook.sendAlert("Backup failed: " + error.getMessage());
 *     
 *     // Example: Retry backup
 *     if (event.getRetryCount() < 3) {
 *         Bukkit.getScheduler().runTaskLater(this, () -> {
 *             BackupAPI.createBackup(event.getOptions());
 *         }, 20 * 60); // retry after 1 minute
 *     }
 * }
 * </pre>
 */
public class BackupFailEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final BackupOptions options;
    private final BackupResult result;
    private final int retryCount;
    
    public BackupFailEvent(@NotNull BackupOptions options, @NotNull BackupResult result) {
        this(options, result, 0);
    }
    
    public BackupFailEvent(@NotNull BackupOptions options, @NotNull BackupResult result, int retryCount) {
        super(true); // async event
        this.options = options;
        this.result = result;
        this.retryCount = retryCount;
    }
    
    /**
     * Get the backup options that were used
     */
    @NotNull
    public BackupOptions getOptions() {
        return options;
    }
    
    /**
     * Get the backup result (contains error information)
     */
    @NotNull
    public BackupResult getResult() {
        return result;
    }
    
    /**
     * Get the error that caused the failure
     */
    @NotNull
    public Throwable getError() {
        return result.getError();
    }
    
    /**
     * Get the number of times this backup has been retried
     */
    public int getRetryCount() {
        return retryCount;
    }
    
    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

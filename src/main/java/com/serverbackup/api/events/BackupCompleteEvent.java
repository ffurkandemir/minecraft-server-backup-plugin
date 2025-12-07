package com.serverbackup.api.events;

import com.serverbackup.api.BackupResult;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a backup operation completes successfully
 * 
 * This event is fired asynchronously after the backup file is created.
 * 
 * Example usage:
 * <pre>
 * {@literal @}EventHandler
 * public void onBackupComplete(BackupCompleteEvent event) {
 *     BackupResult result = event.getResult();
 *     
 *     getLogger().info("Backup completed in " + result.getDuration() + "ms");
 *     getLogger().info("File: " + result.getBackupFile().getName());
 *     getLogger().info("Size: " + result.getFileSize() + " bytes");
 *     
 *     // Example: Upload to cloud storage
 *     cloudStorage.upload(result.getBackupFile());
 * }
 * </pre>
 */
public class BackupCompleteEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final BackupResult result;
    
    public BackupCompleteEvent(@NotNull BackupResult result) {
        super(true); // async event
        this.result = result;
    }
    
    /**
     * Get the backup result containing all information about the completed backup
     */
    @NotNull
    public BackupResult getResult() {
        return result;
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

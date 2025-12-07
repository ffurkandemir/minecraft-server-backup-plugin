package com.serverbackup.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Called when a backup is about to be deleted
 * 
 * This event is cancellable. If cancelled, the backup will not be deleted.
 * 
 * Example usage:
 * <pre>
 * {@literal @}EventHandler
 * public void onBackupDelete(BackupDeleteEvent event) {
 *     File backup = event.getBackupFile();
 *     
 *     // Prevent deletion of backups less than 1 hour old
 *     long age = System.currentTimeMillis() - backup.lastModified();
 *     if (age < 3600000) {
 *         event.setCancelled(true);
 *         event.setCancelReason("Cannot delete backups less than 1 hour old");
 *     }
 * }
 * </pre>
 */
public class BackupDeleteEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final File backupFile;
    private boolean cancelled = false;
    private String cancelReason = null;
    
    public BackupDeleteEvent(@NotNull File backupFile) {
        this.backupFile = backupFile;
    }
    
    /**
     * Get the backup file that will be deleted
     */
    @NotNull
    public File getBackupFile() {
        return backupFile;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    /**
     * Set cancellation reason
     */
    public void setCancelReason(@NotNull String reason) {
        this.cancelReason = reason;
        this.cancelled = true;
    }
    
    /**
     * Get the reason why deletion was cancelled
     */
    public String getCancelReason() {
        return cancelReason;
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

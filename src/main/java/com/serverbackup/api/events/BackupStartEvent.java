package com.serverbackup.api.events;

import com.serverbackup.api.BackupOptions;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a backup operation starts
 * 
 * This event is cancellable. If cancelled, the backup will not be created.
 * 
 * Example usage:
 * <pre>
 * {@literal @}EventHandler
 * public void onBackupStart(BackupStartEvent event) {
 *     if (event.getOptions().getType() == BackupType.FULL) {
 *         // Don't allow full backups during peak hours
 *         if (isPeakTime()) {
 *             event.setCancelled(true);
 *             event.setCancelReason("Full backups are disabled during peak hours");
 *         }
 *     }
 * }
 * </pre>
 */
public class BackupStartEvent extends Event implements Cancellable {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final BackupOptions options;
    private boolean cancelled = false;
    private String cancelReason = null;
    
    public BackupStartEvent(@NotNull BackupOptions options) {
        super(true); // async event
        this.options = options;
    }
    
    /**
     * Get the backup options for this operation
     */
    @NotNull
    public BackupOptions getOptions() {
        return options;
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
     * Set cancellation reason (will be logged)
     */
    public void setCancelReason(@NotNull String reason) {
        this.cancelReason = reason;
        this.cancelled = true;
    }
    
    /**
     * Get the reason why backup was cancelled
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

package com.serverbackup.api;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main API interface for ServerBackupPlugin
 * 
 * This is the primary entry point for other plugins to interact with the backup system.
 * All methods are thread-safe and can be called from any thread.
 * 
 * Example usage:
 * <pre>
 * BackupAPI api = ServerBackupPlugin.getAPI();
 * 
 * BackupOptions options = BackupOptions.builder()
 *     .setType(BackupType.WORLD)
 *     .addWorld("world")
 *     .setAsync(true)
 *     .build();
 * 
 * api.createBackup(options).thenAccept(result -> {
 *     if (result.isSuccess()) {
 *         System.out.println("Backup created: " + result.getBackupFile().getName());
 *     }
 * });
 * </pre>
 * 
 * @since 1.0.0
 * @author ServerBackupPlugin Team
 */
public interface BackupAPI {
    
    /**
     * Create a new backup with the specified options
     * 
     * @param options Backup configuration options
     * @return CompletableFuture that completes when backup is done
     * @throws IllegalStateException if another backup is already in progress
     */
    @NotNull
    CompletableFuture<BackupResult> createBackup(@NotNull BackupOptions options);
    
    /**
     * Create a backup with default options (all worlds, async)
     * 
     * @return CompletableFuture that completes when backup is done
     */
    @NotNull
    default CompletableFuture<BackupResult> createBackup() {
        return createBackup(BackupOptions.builder().build());
    }
    
    /**
     * List all available backups
     * 
     * @return List of backup files, sorted by date (newest first)
     */
    @NotNull
    List<File> listBackups();
    
    /**
     * List backups matching a filter
     * 
     * @param filter Filter criteria
     * @return Filtered list of backup files
     */
    @NotNull
    List<File> listBackups(@NotNull BackupFilter filter);
    
    /**
     * Delete a backup by filename
     * 
     * @param backupName Name of the backup file (e.g., "backup-2025-12-07_12-30-00.zip")
     * @return true if backup was deleted successfully
     */
    boolean deleteBackup(@NotNull String backupName);
    
    /**
     * Delete a backup by file reference
     * 
     * @param backupFile The backup file to delete
     * @return true if backup was deleted successfully
     */
    boolean deleteBackup(@NotNull File backupFile);
    
    /**
     * Get information about a specific backup
     * 
     * @param backupName Name of the backup file
     * @return BackupInfo or null if not found
     */
    @Nullable
    BackupInfo getBackupInfo(@NotNull String backupName);
    
    /**
     * Check if a backup operation is currently in progress
     * 
     * @return true if backup is running
     */
    boolean isBackupInProgress();
    
    /**
     * Get the number of active backup sessions
     * 
     * @return Number of backups currently running
     */
    int getActiveBackupCount();
    
    /**
     * Get the last successful backup result
     * 
     * @return Last backup result or null if no backup has been created yet
     */
    @Nullable
    BackupResult getLastBackup();
    
    /**
     * Get the backup directory where all backups are stored
     * 
     * @return Backup directory
     */
    @NotNull
    File getBackupDirectory();
    
    /**
     * Calculate the total size of all backups
     * 
     * @return Total size in bytes
     */
    long getTotalBackupSize();
    
    /**
     * Get the number of available backups
     * 
     * @return Backup count
     */
    int getBackupCount();
    
    /**
     * Clean old backups based on configured retention policy
     * 
     * @return Number of backups deleted
     */
    int cleanOldBackups();
}

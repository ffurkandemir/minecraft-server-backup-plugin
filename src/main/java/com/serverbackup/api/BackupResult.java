package com.serverbackup.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.UUID;

/**
 * Result of a backup operation
 * 
 * Contains information about the backup execution including success status,
 * file location, duration, and any error messages.
 */
public class BackupResult {
    
    private final UUID sessionId;
    private final boolean success;
    private final File backupFile;
    private final BackupType type;
    private final long startTime;
    private final long endTime;
    private final long fileSize;
    private final Throwable error;
    private final Map<String, Object> metadata;
    
    private BackupResult(Builder builder) {
        this.sessionId = builder.sessionId;
        this.success = builder.success;
        this.backupFile = builder.backupFile;
        this.type = builder.type;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
        this.fileSize = builder.fileSize;
        this.error = builder.error;
        this.metadata = builder.metadata;
    }
    
    /**
     * Get the unique session ID for this backup
     */
    @NotNull
    public UUID getSessionId() {
        return sessionId;
    }
    
    /**
     * Check if backup completed successfully
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Get the backup file (may be null if backup failed)
     */
    @Nullable
    public File getBackupFile() {
        return backupFile;
    }
    
    /**
     * Get the backup type
     */
    @NotNull
    public BackupType getType() {
        return type;
    }
    
    /**
     * Get backup start time (epoch millis)
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Get backup end time (epoch millis)
     */
    public long getEndTime() {
        return endTime;
    }
    
    /**
     * Get backup duration in milliseconds
     */
    public long getDuration() {
        return endTime - startTime;
    }
    
    /**
     * Get backup file size in bytes (0 if failed)
     */
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     * Get error if backup failed
     */
    @Nullable
    public Throwable getError() {
        return error;
    }
    
    /**
     * Get error message if backup failed
     */
    @Nullable
    public String getErrorMessage() {
        return error != null ? error.getMessage() : null;
    }
    
    /**
     * Get custom metadata
     */
    @NotNull
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    @NotNull
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private UUID sessionId = UUID.randomUUID();
        private boolean success = false;
        private File backupFile = null;
        private BackupType type = BackupType.WORLD;
        private long startTime = System.currentTimeMillis();
        private long endTime = System.currentTimeMillis();
        private long fileSize = 0;
        private Throwable error = null;
        private Map<String, Object> metadata = Map.of();
        
        public Builder setSessionId(@NotNull UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public Builder setSuccess(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder setBackupFile(@Nullable File backupFile) {
            this.backupFile = backupFile;
            return this;
        }
        
        public Builder setType(@NotNull BackupType type) {
            this.type = type;
            return this;
        }
        
        public Builder setStartTime(long startTime) {
            this.startTime = startTime;
            return this;
        }
        
        public Builder setEndTime(long endTime) {
            this.endTime = endTime;
            return this;
        }
        
        public Builder setFileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }
        
        public Builder setError(@Nullable Throwable error) {
            this.error = error;
            return this;
        }
        
        public Builder setMetadata(@NotNull Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        @NotNull
        public BackupResult build() {
            return new BackupResult(this);
        }
    }
}

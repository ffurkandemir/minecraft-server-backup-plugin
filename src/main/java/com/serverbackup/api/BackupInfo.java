package com.serverbackup.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Instant;
import java.util.Set;

/**
 * Information about an existing backup
 */
public class BackupInfo {
    
    private final File backupFile;
    private final String name;
    private final BackupType type;
    private final long size;
    private final Instant createdAt;
    private final Set<String> worldsIncluded;
    private final boolean compressed;
    
    public BackupInfo(@NotNull File backupFile, 
                      @NotNull String name,
                      @NotNull BackupType type,
                      long size,
                      @NotNull Instant createdAt,
                      @NotNull Set<String> worldsIncluded,
                      boolean compressed) {
        this.backupFile = backupFile;
        this.name = name;
        this.type = type;
        this.size = size;
        this.createdAt = createdAt;
        this.worldsIncluded = worldsIncluded;
        this.compressed = compressed;
    }
    
    @NotNull
    public File getBackupFile() {
        return backupFile;
    }
    
    @NotNull
    public String getName() {
        return name;
    }
    
    @NotNull
    public BackupType getType() {
        return type;
    }
    
    public long getSize() {
        return size;
    }
    
    @NotNull
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    @NotNull
    public Set<String> getWorldsIncluded() {
        return worldsIncluded;
    }
    
    public boolean isCompressed() {
        return compressed;
    }
    
    public String getFormattedSize() {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}

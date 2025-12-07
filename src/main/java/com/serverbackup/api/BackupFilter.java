package com.serverbackup.api;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.function.Predicate;

/**
 * Filter for listing backups
 * 
 * Example usage:
 * <pre>
 * BackupFilter filter = BackupFilter.builder()
 *     .setType(BackupType.WORLD)
 *     .setMinSize(1024 * 1024 * 10) // 10 MB
 *     .setAfter(Instant.now().minus(7, ChronoUnit.DAYS))
 *     .build();
 * </pre>
 */
public class BackupFilter {
    
    private final BackupType type;
    private final Long minSize;
    private final Long maxSize;
    private final Instant after;
    private final Instant before;
    private final String nameContains;
    private final Predicate<BackupInfo> customPredicate;
    
    private BackupFilter(Builder builder) {
        this.type = builder.type;
        this.minSize = builder.minSize;
        this.maxSize = builder.maxSize;
        this.after = builder.after;
        this.before = builder.before;
        this.nameContains = builder.nameContains;
        this.customPredicate = builder.customPredicate;
    }
    
    /**
     * Test if a backup matches this filter
     */
    public boolean matches(@NotNull BackupInfo info) {
        if (type != null && info.getType() != type) {
            return false;
        }
        if (minSize != null && info.getSize() < minSize) {
            return false;
        }
        if (maxSize != null && info.getSize() > maxSize) {
            return false;
        }
        if (after != null && info.getCreatedAt().isBefore(after)) {
            return false;
        }
        if (before != null && info.getCreatedAt().isAfter(before)) {
            return false;
        }
        if (nameContains != null && !info.getName().contains(nameContains)) {
            return false;
        }
        if (customPredicate != null && !customPredicate.test(info)) {
            return false;
        }
        return true;
    }
    
    @NotNull
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private BackupType type = null;
        private Long minSize = null;
        private Long maxSize = null;
        private Instant after = null;
        private Instant before = null;
        private String nameContains = null;
        private Predicate<BackupInfo> customPredicate = null;
        
        public Builder setType(BackupType type) {
            this.type = type;
            return this;
        }
        
        public Builder setMinSize(long minSize) {
            this.minSize = minSize;
            return this;
        }
        
        public Builder setMaxSize(long maxSize) {
            this.maxSize = maxSize;
            return this;
        }
        
        public Builder setAfter(Instant after) {
            this.after = after;
            return this;
        }
        
        public Builder setBefore(Instant before) {
            this.before = before;
            return this;
        }
        
        public Builder setNameContains(String nameContains) {
            this.nameContains = nameContains;
            return this;
        }
        
        public Builder setCustomPredicate(Predicate<BackupInfo> predicate) {
            this.customPredicate = predicate;
            return this;
        }
        
        @NotNull
        public BackupFilter build() {
            return new BackupFilter(this);
        }
    }
}

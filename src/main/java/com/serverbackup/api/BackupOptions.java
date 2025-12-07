package com.serverbackup.api;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Configuration options for creating a backup
 * 
 * Use the builder pattern to construct options:
 * <pre>
 * BackupOptions options = BackupOptions.builder()
 *     .setType(BackupType.WORLD)
 *     .addWorld("world")
 *     .addWorld("world_nether")
 *     .setCompression(true)
 *     .setAsync(true)
 *     .setNotifyPlayers(false)
 *     .build();
 * </pre>
 */
public class BackupOptions {
    
    private final BackupType type;
    private final Set<String> worldNames;
    private final boolean includePlugins;
    private final boolean compression;
    private final boolean async;
    private final boolean notifyPlayers;
    private final String customName;
    private final Map<String, Object> metadata;
    
    private BackupOptions(Builder builder) {
        this.type = builder.type;
        this.worldNames = Collections.unmodifiableSet(builder.worldNames);
        this.includePlugins = builder.includePlugins;
        this.compression = builder.compression;
        this.async = builder.async;
        this.notifyPlayers = builder.notifyPlayers;
        this.customName = builder.customName;
        this.metadata = Collections.unmodifiableMap(builder.metadata);
    }
    
    public BackupType getType() { return type; }
    public Set<String> getWorldNames() { return worldNames; }
    public boolean includePlugins() { return includePlugins; }
    public boolean isCompression() { return compression; }
    public boolean isAsync() { return async; }
    public boolean isNotifyPlayers() { return notifyPlayers; }
    public String getCustomName() { return customName; }
    public Map<String, Object> getMetadata() { return metadata; }
    
    @NotNull
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private BackupType type = BackupType.WORLD;
        private Set<String> worldNames = new HashSet<>();
        private boolean includePlugins = false;
        private boolean compression = true;
        private boolean async = true;
        private boolean notifyPlayers = true;
        private String customName = null;
        private Map<String, Object> metadata = new HashMap<>();
        
        /**
         * Set the backup type
         * @param type Backup type (WORLD or FULL)
         */
        public Builder setType(@NotNull BackupType type) {
            this.type = type;
            if (type == BackupType.FULL) {
                this.includePlugins = true;
            }
            return this;
        }
        
        /**
         * Add a world to backup
         * @param worldName World name
         */
        public Builder addWorld(@NotNull String worldName) {
            this.worldNames.add(worldName);
            return this;
        }
        
        /**
         * Add a world to backup
         * @param world World instance
         */
        public Builder addWorld(@NotNull World world) {
            this.worldNames.add(world.getName());
            return this;
        }
        
        /**
         * Set multiple worlds to backup
         * @param worldNames Collection of world names
         */
        public Builder setWorlds(@NotNull Collection<String> worldNames) {
            this.worldNames.clear();
            this.worldNames.addAll(worldNames);
            return this;
        }
        
        /**
         * Include plugins folder in backup
         * @param includePlugins true to include plugins
         */
        public Builder setIncludePlugins(boolean includePlugins) {
            this.includePlugins = includePlugins;
            return this;
        }
        
        /**
         * Enable/disable compression (ZIP format)
         * @param compression true to compress backup
         */
        public Builder setCompression(boolean compression) {
            this.compression = compression;
            return this;
        }
        
        /**
         * Run backup asynchronously (recommended)
         * @param async true for async execution
         */
        public Builder setAsync(boolean async) {
            this.async = async;
            return this;
        }
        
        /**
         * Notify all players when backup starts/completes
         * @param notifyPlayers true to notify players
         */
        public Builder setNotifyPlayers(boolean notifyPlayers) {
            this.notifyPlayers = notifyPlayers;
            return this;
        }
        
        /**
         * Set a custom name for the backup (optional)
         * If not set, timestamp-based name will be used
         * @param customName Custom backup name (without extension)
         */
        public Builder setCustomName(String customName) {
            this.customName = customName;
            return this;
        }
        
        /**
         * Add custom metadata to the backup
         * @param key Metadata key
         * @param value Metadata value
         */
        public Builder addMetadata(@NotNull String key, @NotNull Object value) {
            this.metadata.put(key, value);
            return this;
        }
        
        /**
         * Build the BackupOptions instance
         * @return Immutable BackupOptions
         */
        @NotNull
        public BackupOptions build() {
            return new BackupOptions(this);
        }
    }
}

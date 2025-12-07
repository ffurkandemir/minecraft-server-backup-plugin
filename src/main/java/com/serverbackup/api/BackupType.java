package com.serverbackup.api;

/**
 * Types of backups supported by the plugin
 */
public enum BackupType {
    
    /**
     * Backup only world folders (default)
     * Includes: world, world_nether, world_the_end, or custom worlds
     */
    WORLD("world"),
    
    /**
     * Full backup including worlds and plugins folder
     * WARNING: May contain sensitive data (configs, databases)
     */
    FULL("full");
    
    private final String name;
    
    BackupType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Parse backup type from string
     * @param name Type name ("world" or "full")
     * @return BackupType or WORLD as default
     */
    public static BackupType fromString(String name) {
        for (BackupType type : values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return WORLD;
    }
}

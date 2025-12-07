package com.serverbackup.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active backup sessions to prevent conflicts and provide progress information
 */
public class BackupSessionManager {
    
    private final Map<UUID, BackupSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * Start a new backup session
     * @param type Backup type (world/full)
     * @return Session UUID
     */
    public UUID startSession(String type) {
        UUID sessionId = UUID.randomUUID();
        BackupSession session = new BackupSession(sessionId, type, System.currentTimeMillis());
        activeSessions.put(sessionId, session);
        return sessionId;
    }
    
    /**
     * End a backup session
     * @param sessionId Session UUID
     * @param success Whether backup completed successfully
     */
    public void endSession(UUID sessionId, boolean success) {
        BackupSession session = activeSessions.get(sessionId);
        if (session != null) {
            session.setCompleted(true);
            session.setSuccess(success);
            session.setEndTime(System.currentTimeMillis());
            // Keep session for 5 minutes for status queries
            scheduleSessionCleanup(sessionId);
        }
    }
    
    /**
     * Check if any backup is currently running
     * @return true if a backup is in progress
     */
    public boolean isBackupRunning() {
        return activeSessions.values().stream()
            .anyMatch(session -> !session.isCompleted());
    }
    
    /**
     * Get active backup session count
     * @return Number of active backups
     */
    public int getActiveSessionCount() {
        return (int) activeSessions.values().stream()
            .filter(session -> !session.isCompleted())
            .count();
    }
    
    /**
     * Get session information
     * @param sessionId Session UUID
     * @return BackupSession or null if not found
     */
    public BackupSession getSession(UUID sessionId) {
        return activeSessions.get(sessionId);
    }
    
    private void scheduleSessionCleanup(UUID sessionId) {
        // Remove session after 5 minutes
        new Thread(() -> {
            try {
                Thread.sleep(300000); // 5 minutes
                activeSessions.remove(sessionId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Represents a backup session
     */
    public static class BackupSession {
        private final UUID id;
        private final String type;
        private final long startTime;
        private long endTime;
        private boolean completed;
        private boolean success;
        
        public BackupSession(UUID id, String type, long startTime) {
            this.id = id;
            this.type = type;
            this.startTime = startTime;
            this.completed = false;
            this.success = false;
        }
        
        public UUID getId() { return id; }
        public String getType() { return type; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public boolean isCompleted() { return completed; }
        public boolean isSuccess() { return success; }
        
        public void setEndTime(long endTime) { this.endTime = endTime; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public long getDuration() {
            return completed ? (endTime - startTime) : (System.currentTimeMillis() - startTime);
        }
    }
}

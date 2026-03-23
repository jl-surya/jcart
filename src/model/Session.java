package model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Session represents a user session in the system.
 * 
 * Includes:
 * - Session identification (ID, token)
 * - User association (user type, user ID)
 * - Expiration management with auto-renewal
 * - Dirty tracking for sync operations
 * - Session validation and remaining time calculation
 */
public class Session implements Serializable {
    
    private static final long SESSION_DURATION_HOURS = 24;
    
    private UUID sessionId;
    private String userType;
    private String userId;
    private String sessionToken;
    private Timestamp expiresAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp lastSyncAt;
    private long lastAccessedTime;
    private boolean isValid;
    private boolean dirty;

    /**
     * Constructs a new session with default values and auto-generated token.
     */
    public Session() {
        this.createdAt = Timestamp.from(Instant.now());
        this.updatedAt = Timestamp.from(Instant.now());
        this.lastAccessedTime = System.currentTimeMillis();
        this.isValid = true;
        this.dirty = true;
        this.sessionToken = UUID.randomUUID().toString();
        updateExpiry();
    }
    
    /**
     * Updates session expiry to current time + session duration.
     */
    public void updateExpiry() {
        this.expiresAt = Timestamp.from(Instant.now().plus(SESSION_DURATION_HOURS, ChronoUnit.HOURS));
        this.updatedAt = Timestamp.from(Instant.now());
        this.lastAccessedTime = System.currentTimeMillis();
        this.isValid = true;
        this.dirty = true;
    }
    
    /**
     * Touches the session to renew its expiry.
     */
    public void touch() {
        this.updateExpiry();
    }
    
    /**
     * Checks if session has expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(Timestamp.from(Instant.now()));
    }
    
    /**
     * Checks if session needs synchronization with database.
     *
     * @return true if dirty and last sync was more than 1 minute ago
     */
    public boolean needsSync() {
        if (!dirty) return false;
        if (lastSyncAt == null) return true;
        long minutesSinceLastSync = (System.currentTimeMillis() - lastSyncAt.getTime()) / (1000 * 60);
        return minutesSinceLastSync >= 1;
    }
    
    /**
     * Marks session as synced with database.
     */
    public void markSynced() {
        this.dirty = false;
        this.lastSyncAt = Timestamp.from(Instant.now());
        this.updatedAt = Timestamp.from(Instant.now());
    }
    
    /**
     * Gets remaining time in milliseconds before session expires.
     *
     * @return remaining time in milliseconds
     */
    public long getRemainingTimeMillis() {
        if (expiresAt == null) return 0;
        long remaining = expiresAt.getTime() - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }
    
    /**
     * Gets remaining time in hours before session expires.
     *
     * @return remaining time in hours
     */
    public long getRemainingTimeHours() {
        return getRemainingTimeMillis() / (1000 * 60 * 60);
    }

    public UUID getSessionId() { return sessionId; }
    public void setSessionId(UUID sessionId) { this.sessionId = sessionId; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { 
        this.expiresAt = expiresAt;
        this.updatedAt = Timestamp.from(Instant.now());
        this.dirty = true;
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Timestamp getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Timestamp lastSyncAt) { this.lastSyncAt = lastSyncAt; }

    public long getLastAccessedTime() { return lastAccessedTime; }
    public void setLastAccessedTime(long lastAccessedTime) { this.lastAccessedTime = lastAccessedTime; }

    public boolean isValid() { return isValid; }
    public void setValid(boolean valid) { isValid = valid; }

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
}
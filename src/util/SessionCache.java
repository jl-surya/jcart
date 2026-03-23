package util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import model.Session;

/**
 * SessionCache provides in-memory session management with automatic persistence.
 * 
 * Includes:
 * - Concurrent session storage with token mapping
 * - Automatic expiration cleanup
 * - Dirty session tracking for persistence
 * - Background sync with database
 * - Session validation and retrieval
 */
public class SessionCache {
    
    private static final Map<UUID, Session> sessionCache = new ConcurrentHashMap<>();
    private static final Map<String, UUID> tokenToIdCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private static SessionPersister persister;
    private static final AtomicInteger totalSyncs = new AtomicInteger(0);
    
    static {
        persister = new SessionPersister();
        
        // Schedule expired session cleanup every 5 minutes
        executor.scheduleAtFixedRate(SessionCache::cleanExpiredSessions, 5, 5, TimeUnit.MINUTES);
        
        // Schedule dirty session persistence every 10 minutes
        executor.scheduleAtFixedRate(() -> {
            try {
                persister.persistDirtySessions(sessionCache);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.MINUTES);
    }
    
    /**
     * Adds or updates a session in the cache.
     *
     * @param session the session to cache
     */
    public static void put(Session session) {
        if (session != null && session.getSessionId() != null) {
            session.setDirty(true);
            sessionCache.put(session.getSessionId(), session);
            tokenToIdCache.put(session.getSessionToken(), session.getSessionId());
        }
    }
    
    /**
     * Retrieves session by token.
     *
     * @param sessionToken the session token
     * @return Session if found and valid, null otherwise
     */
    public static Session getByToken(String sessionToken) {
        UUID sessionId = tokenToIdCache.get(sessionToken);
        if (sessionId == null) {
            return null;
        }
        return getInternal(sessionId);
    }
    
    /**
     * Internal method to retrieve session by ID with expiration check.
     *
     * @param sessionId the session ID
     * @return Session if valid, null if expired or not found
     */
    private static Session getInternal(UUID sessionId) {
        Session session = sessionCache.get(sessionId);
        
        if (session != null) {
            if (session.isExpired()) {
                removeInternal(sessionId);
                return null;
            }
            session.touch();
            session.setValid(true);
            return session;
        }
        return null;
    }
    
    /**
     * Removes session by token.
     *
     * @param sessionToken the session token
     */
    public static void removeByToken(String sessionToken) {
        UUID sessionId = tokenToIdCache.remove(sessionToken);
        if (sessionId != null) {
            removeInternal(sessionId);
            CompletableFuture.runAsync(() -> {
                try {
                    persister.deleteSession(sessionId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    /**
     * Internal method to remove session by ID.
     *
     * @param sessionId the session ID
     */
    private static void removeInternal(UUID sessionId) {
        Session session = sessionCache.remove(sessionId);
        if (session != null) {
            tokenToIdCache.remove(session.getSessionToken());
        }
    }
    
    /**
     * Updates an existing session in cache.
     *
     * @param session the session to update
     */
    public static void update(Session session) {
        if (session != null && session.getSessionId() != null) {
            session.setDirty(true);
            sessionCache.put(session.getSessionId(), session);
            tokenToIdCache.put(session.getSessionToken(), session.getSessionId());
        }
    }
    
    /**
     * Checks if session token is valid.
     *
     * @param sessionToken the session token
     * @return true if session exists and not expired
     */
    public static boolean isValid(String sessionToken) {
        UUID sessionId = tokenToIdCache.get(sessionToken);
        if (sessionId == null) return false;
        Session session = sessionCache.get(sessionId);
        return session != null && !session.isExpired();
    }
    
    /**
     * Gets user ID from session token.
     *
     * @param sessionToken the session token
     * @return user ID if found, null otherwise
     */
    public static String getUserId(String sessionToken) {
        UUID sessionId = tokenToIdCache.get(sessionToken);
        if (sessionId == null) return null;
        Session session = sessionCache.get(sessionId);
        return session != null ? session.getUserId() : null;
    }
    
    /**
     * Gets user type from session token.
     *
     * @param sessionToken the session token
     * @return user type if found, null otherwise
     */
    public static String getUserType(String sessionToken) {
        UUID sessionId = tokenToIdCache.get(sessionToken);
        if (sessionId == null) return null;
        Session session = sessionCache.get(sessionId);
        return session != null ? session.getUserType() : null;
    }
    
    /**
     * Gets all cached sessions.
     *
     * @return map of all sessions
     */
    static Map<UUID, Session> getAllSessions() {
        return new ConcurrentHashMap<>(sessionCache);
    }
    
    /**
     * Gets current cache size.
     *
     * @return number of sessions in cache
     */
    public static int getCacheSize() {
        return sessionCache.size();
    }
    
    /**
     * Gets count of dirty sessions pending sync.
     *
     * @return number of dirty sessions
     */
    public static int getDirtyCount() {
        return (int) sessionCache.values().stream()
                .filter(Session::isDirty)
                .count();
    }
    
    /**
     * Gets total number of sync operations performed.
     *
     * @return total sync count
     */
    public static int getTotalSyncs() {
        return totalSyncs.get();
    }
    
    /**
     * Increments sync counter.
     */
    static void incrementSyncCount() {
        totalSyncs.incrementAndGet();
    }
    
    /**
     * Removes all expired sessions from cache.
     */
    private static void cleanExpiredSessions() {
        sessionCache.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            if (session.isExpired()) {
                tokenToIdCache.remove(session.getSessionToken());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Forces immediate sync of dirty sessions to database.
     */
    public static void forceSync() {
        try {
            persister.persistDirtySessions(sessionCache);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
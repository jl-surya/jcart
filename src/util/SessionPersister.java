package util;

import dao.SessionDAO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import model.Session;

/**
 * SessionPersister handles database persistence for sessions.
 * 
 * Includes:
 * - Batch updates for dirty sessions
 * - Individual session deletion
 * - Sync time tracking for each session
 */
public class SessionPersister {
    
    private final SessionDAO sessionDAO = new SessionDAO();
    
    /**
     * Persists all dirty sessions that need synchronization.
     *
     * @param sessionCache the current session cache
     * @throws Exception if database operation fails
     */
    public void persistDirtySessions(Map<UUID, Session> sessionCache) throws Exception {
        List<Session> dirtySessions = new ArrayList<>();
        
        for (Session session : sessionCache.values()) {
            if (session.needsSync()) {
                dirtySessions.add(session);
            }
        }
        
        if (dirtySessions.isEmpty()) {
            return;
        }
        
        sessionDAO.batchUpdate(dirtySessions);
        
        for (Session session : dirtySessions) {
            session.markSynced();
            sessionDAO.updateSyncTime(session.getSessionId(), session.getLastSyncAt());
        }
        
        SessionCache.incrementSyncCount();
    }
    
    /**
     * Deletes a session from database.
     *
     * @param sessionId the session ID to delete
     * @throws Exception if database operation fails
     */
    public void deleteSession(UUID sessionId) throws Exception {
        sessionDAO.delete(sessionId);
    }
}
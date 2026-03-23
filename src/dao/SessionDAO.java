package dao;

import java.sql.*;
import java.util.List;
import java.util.UUID;
import model.Session;
import util.DBUtil;

/**
 * SessionDAO handles database operations for user sessions.
 * 
 * Includes:
 * - Session creation and management
 * - Batch updates for session expiration
 * - Token-based session retrieval
 * - Session cleanup by user or token
 */
public class SessionDAO {
    
    /**
     * Creates a new session record.
     *
     * @param session the session object to insert
     * @throws Exception if database operation fails
     */
    public void create(Session session) throws Exception {
        String sql = "INSERT INTO sessions (session_id, user_type, user_id, session_token, expires_at, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, session.getSessionId());
            ps.setString(2, session.getUserType());
            ps.setString(3, session.getUserId());
            ps.setString(4, session.getSessionToken());
            ps.setTimestamp(5, session.getExpiresAt());
            ps.setTimestamp(6, session.getCreatedAt());
            ps.setTimestamp(7, session.getUpdatedAt());
            ps.executeUpdate();
        }
    }
    
    /**
     * Updates existing session.
     *
     * @param session the session object with updated values
     * @throws Exception if database operation fails
     */
    public void update(Session session) throws Exception {
        String sql = "UPDATE sessions SET expires_at = ?, updated_at = ? WHERE session_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, session.getExpiresAt());
            ps.setTimestamp(2, session.getUpdatedAt());
            ps.setObject(3, session.getSessionId());
            ps.executeUpdate();
        }
    }
    
    /**
     * Batch updates multiple sessions.
     *
     * @param sessions list of sessions to update
     * @throws Exception if database operation fails
     */
    public void batchUpdate(List<Session> sessions) throws Exception {
        if (sessions.isEmpty()) return;
        
        String sql = "UPDATE sessions SET expires_at = ?, updated_at = ? WHERE session_id = ?";
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Session session : sessions) {
                    ps.setTimestamp(1, session.getExpiresAt());
                    ps.setTimestamp(2, session.getUpdatedAt());
                    ps.setObject(3, session.getSessionId());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    /**
     * Updates last sync time for a session.
     *
     * @param sessionId the session ID
     * @param lastSyncAt the last synchronization timestamp
     * @throws Exception if database operation fails
     */
    public void updateSyncTime(UUID sessionId, Timestamp lastSyncAt) throws Exception {
        String sql = "UPDATE sessions SET last_sync_at = ?, updated_at = ? WHERE session_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, lastSyncAt);
            ps.setTimestamp(2, Timestamp.from(java.time.Instant.now()));
            ps.setObject(3, sessionId);
            ps.executeUpdate();
        }
    }
    
    /**
     * Deletes session by ID.
     *
     * @param sessionId the session ID
     * @throws Exception if database operation fails
     */
    public void delete(UUID sessionId) throws Exception {
        String sql = "DELETE FROM sessions WHERE session_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes all sessions for a specific user.
     *
     * @param userId the user ID
     * @param userType the user type (e.g., CUSTOMER, ADMIN)
     * @throws Exception if database operation fails
     */
    public void deleteAllForUser(String userId, String userType) throws Exception {
        String sql = "DELETE FROM sessions WHERE user_id = ? AND user_type = ?";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, userType);
            ps.executeUpdate();
        }
    }
    
    /**
     * Deletes session by token.
     *
     * @param sessionToken the session token
     * @throws Exception if database operation fails
     */
    public void deleteByToken(String sessionToken) throws Exception {
        String sql = "DELETE FROM sessions WHERE session_token = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionToken);
            ps.executeUpdate();
        }
    }
    
    /**
     * Retrieves session by ID.
     *
     * @param sessionId the session ID
     * @return Session object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Session getById(UUID sessionId) throws Exception {
        String sql = "SELECT session_id, user_type, user_id, session_token, expires_at, created_at, updated_at, last_sync_at " +
                     "FROM sessions WHERE session_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, sessionId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Retrieves session by token.
     *
     * @param sessionToken the session token
     * @return Session object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Session getByToken(String sessionToken) throws Exception {
        String sql = "SELECT session_id, user_type, user_id, session_token, expires_at, created_at, updated_at, last_sync_at " +
                     "FROM sessions WHERE session_token = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionToken);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Maps ResultSet row to Session object.
     *
     * @param rs the ResultSet
     * @return mapped Session object
     * @throws Exception if mapping fails
     */
    private Session mapRow(ResultSet rs) throws Exception {
        Session session = new Session();
        session.setSessionId((UUID) rs.getObject("session_id"));
        session.setUserType(rs.getString("user_type"));
        session.setUserId(rs.getString("user_id"));
        session.setSessionToken(rs.getString("session_token"));
        session.setExpiresAt(rs.getTimestamp("expires_at"));
        session.setCreatedAt(rs.getTimestamp("created_at"));
        session.setUpdatedAt(rs.getTimestamp("updated_at"));
        session.setLastSyncAt(rs.getTimestamp("last_sync_at"));
        return session;
    }
}
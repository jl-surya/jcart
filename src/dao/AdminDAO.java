package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Admin;
import util.DBUtil;

/**
 * AdminDAO handles database operations for admin entities.
 * 
 * Includes:
 * - CRUD operations for admin management
 * - Search by ID, username, email
 * - Paginated admin listing
 * - Profile and password updates
 * - Account activation/deactivation
 * - Super admin protection
 */
public class AdminDAO {
    
    /**
     * Inserts a new admin into database with auto-generated admin_id.
     *
     * @param admin the admin object to insert
     * @throws Exception if database operation fails
     */
    public void insert(Admin admin) throws Exception {
        String sql = "INSERT INTO admins (username, email, password, phone, role, permissions, is_active, is_super_admin) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING admin_id";
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, admin.getUsername());
                ps.setString(2, admin.getEmail());
                ps.setString(3, admin.getPassword());
                ps.setString(4, admin.getPhone());
                ps.setString(5, admin.getRole());
                ps.setArray(6, conn.createArrayOf("text", admin.getPermissions()));
                ps.setBoolean(7, admin.isActive());
                ps.setBoolean(8, admin.isSuperAdmin());
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        admin.setAdminId(rs.getString("admin_id"));
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    /**
     * Retrieves admin by admin ID.
     *
     * @param adminId the admin ID to search
     * @return Admin object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Admin getById(String adminId) throws Exception {
        String sql = "SELECT admin_id, username, email, password, phone, role, permissions, is_active, is_super_admin, created_at, updated_at " +
                     "FROM admins WHERE admin_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, adminId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Retrieves admin by username.
     *
     * @param username the username to search
     * @return Admin object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Admin getByUsername(String username) throws Exception {
        String sql = "SELECT admin_id, username, email, password, phone, role, permissions, is_active, is_super_admin, created_at, updated_at " +
                     "FROM admins WHERE username = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Retrieves admin by email.
     *
     * @param email the email to search
     * @return Admin object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Admin getByEmail(String email) throws Exception {
        String sql = "SELECT admin_id, username, email, password, phone, role, permissions, is_active, is_super_admin, created_at, updated_at " +
                     "FROM admins WHERE email = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves admins with pagination and filters.
     * Supports search by username/email, role filter, and status filter.
     *
     * @param page the page number (1-indexed)
     * @param size the number of records per page
     * @param search search term for username or email (optional)
     * @param role filter by role (optional)
     * @param status filter by status: "active", "inactive", or null for all
     * @return list of admins matching filters
     * @throws Exception if database operation fails
     */
    public List<Admin> getAll(int page, int size, String search, String role, String status, String sortBy, String sortDir) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT admin_id, username, email, password, phone, role, permissions, is_active, is_super_admin, created_at, updated_at ");
        sql.append("FROM admins WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(username) LIKE ? OR LOWER(email) LIKE ? OR LOWER(phone) LIKE ?) ");
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (role != null && !role.trim().isEmpty()) {
            sql.append("AND role = ? ");
            params.add(role.trim());
        }
        
        if (status != null && !status.trim().isEmpty()) {
            if ("active".equalsIgnoreCase(status)) {
                sql.append("AND is_active = TRUE ");
            } else if ("inactive".equalsIgnoreCase(status)) {
                sql.append("AND is_active = FALSE ");
            }
        }
        
        String validatedSortBy = validateSortColumn(sortBy);
        String validatedSortDir = (sortDir != null && sortDir.equalsIgnoreCase("asc")) ? "ASC" : "DESC";
        
        sql.append("ORDER BY ");
        if (validatedSortBy != null && !validatedSortBy.isEmpty()) {
            sql.append(validatedSortBy).append(" ").append(validatedSortDir).append(", ");
        }
        sql.append("is_super_admin DESC, created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);
        
        List<Admin> admins = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    admins.add(mapRow(rs));
                }
            }
        }
        return admins;
    }
    
    /**
     * Gets total count of admins matching filters.
     *
     * @param search search term for username or email (optional)
     * @param role filter by role (optional)
     * @param status filter by status: "active", "inactive", or null for all
     * @return count of admins matching filters
     * @throws Exception if database operation fails
     */
    public int getAllCount(String search, String role, String status) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM admins WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(username) LIKE ? OR LOWER(email) LIKE ? OR LOWER(phone) LIKE ?) ");
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (role != null && !role.trim().isEmpty()) {
            sql.append("AND role = ? ");
            params.add(role.trim());
        }
        
        if (status != null && !status.trim().isEmpty()) {
            if ("active".equalsIgnoreCase(status)) {
                sql.append("AND is_active = TRUE ");
            } else if ("inactive".equalsIgnoreCase(status)) {
                sql.append("AND is_active = FALSE ");
            }
        }
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    /**
     * Gets stats (active count, inactive count) for search results.
     * Consolidated method following product management pattern.
     *
     * @param search search term (optional)
     * @param role filter by role (optional)
     * @param status status filter (optional - "active", "inactive", or null for all)
     * @return map with activeCount and inactiveCount
     * @throws Exception if database operation fails
     */
    public Map<String, Integer> getStats(String search, String role, String status) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("SUM(CASE WHEN is_active = TRUE THEN 1 ELSE 0 END) as active_count, ");
        sql.append("SUM(CASE WHEN is_active = FALSE THEN 1 ELSE 0 END) as inactive_count ");
        sql.append("FROM admins WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(username) LIKE ? OR LOWER(email) LIKE ? OR LOWER(phone) LIKE ?) ");
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (role != null && !role.trim().isEmpty()) {
            sql.append("AND role = ? ");
            params.add(role.trim());
        }
        
        if (status != null && !status.trim().isEmpty()) {
            if ("active".equalsIgnoreCase(status.trim())) {
                sql.append("AND is_active = TRUE ");
            } else if ("inactive".equalsIgnoreCase(status.trim())) {
                sql.append("AND is_active = FALSE ");
            }
        }
        
        Map<String, Integer> stats = new HashMap<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                }
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("activeCount", rs.getInt("active_count"));
                    stats.put("inactiveCount", rs.getInt("inactive_count"));
                }
            }
        }
        
        return stats;
    }
    
    /**
     * Updates admin information. Super admin accounts cannot be updated via this method.
     *
     * @param admin the admin object with updated details
     * @throws Exception if database operation fails or trying to update super admin
     */
    public void update(Admin admin) throws Exception {
        String sql = "UPDATE admins SET username = ?, email = ?, phone = ?, role = ?, permissions = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE admin_id = ? AND is_super_admin = FALSE";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, admin.getUsername());
            ps.setString(2, admin.getEmail());
            ps.setString(3, admin.getPhone());
            ps.setString(4, admin.getRole());
            ps.setArray(5, conn.createArrayOf("text", admin.getPermissions()));
            ps.setBoolean(6, admin.isActive());
            ps.setString(7, admin.getAdminId());
            
            int updated = ps.executeUpdate();
            if (updated == 0 && admin.isSuperAdmin()) {
                throw new IllegalArgumentException("Cannot update SUPER_ADMIN account");
            }
        }
    }
    
    /**
     * Updates admin profile information (username, email, phone).
     *
     * @param adminId the admin ID
     * @param username the new username
     * @param email the new email
     * @param phone the new phone number
     * @throws Exception if database operation fails
     */
    public void updateProfile(String adminId, String username, String email, String phone) throws Exception {
        String sql = "UPDATE admins SET username = ?, email = ?, phone = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE admin_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, adminId);
            ps.executeUpdate();
        }
    }
    
    /**
     * Updates admin password.
     *
     * @param adminId the admin ID
     * @param newPassword the new hashed password
     * @throws Exception if database operation fails
     */
    public void updatePassword(String adminId, String newPassword) throws Exception {
        String sql = "UPDATE admins SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE admin_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, adminId);
            ps.executeUpdate();
        }
    }
        
    /**
     * Deactivates admin account. Super admin accounts cannot be deactivated.
     *
     * @param adminId the admin ID
     * @throws Exception if database operation fails or trying to deactivate super admin
     */
    public void deactivate(String adminId) throws Exception {
        String sql = "UPDATE admins SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE admin_id = ? AND is_super_admin = FALSE";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, adminId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                Admin admin = getById(adminId);
                if (admin != null && admin.isSuperAdmin()) {
                    throw new IllegalArgumentException("Cannot deactivate SUPER_ADMIN account");
                }
            }
        }
    }

    /**
     * Checks if username already exists.
     *
     * @param username the username to check
     * @return true if username exists, false otherwise
     * @throws Exception if database operation fails
     */
    public boolean usernameExists(String username) throws Exception {
        String sql = "SELECT COUNT(*) FROM admins WHERE username = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
    
    /**
     * Checks if email already exists.
     *
     * @param email the email to check
     * @return true if email exists, false otherwise
     * @throws Exception if database operation fails
     */
    public boolean emailExists(String email) throws Exception {
        String sql = "SELECT COUNT(*) FROM admins WHERE email = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * Validates and returns safe sort column name for SQL.
     *
     * @param sortBy column name to validate
     * @return validated column name or null if invalid
     */
    private String validateSortColumn(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return null;
        }
        
        switch (sortBy.toLowerCase()) {
            case "username":
                return "username";
            case "email":
                return "email";
            case "role":
                return "role";
            case "created_at":
            case "createdat":
                return "created_at";
            default:
                return null;
        }
    }
    
    /**
     * Maps ResultSet row to Admin object.
     *
     * @param rs the ResultSet
     * @return mapped Admin object
     * @throws Exception if mapping fails
     */
    private Admin mapRow(ResultSet rs) throws Exception {
        Admin admin = new Admin();
        admin.setAdminId(rs.getString("admin_id"));
        admin.setUsername(rs.getString("username"));
        admin.setEmail(rs.getString("email"));
        admin.setPassword(rs.getString("password"));
        admin.setPhone(rs.getString("phone"));
        admin.setRole(rs.getString("role"));
        Array permissionsArray = rs.getArray("permissions");
        if (permissionsArray != null) {
            admin.setPermissions((String[]) permissionsArray.getArray());
        }
        admin.setActive(rs.getBoolean("is_active"));
        admin.setSuperAdmin(rs.getBoolean("is_super_admin"));
        admin.setCreatedAt(rs.getTimestamp("created_at"));
        admin.setUpdatedAt(rs.getTimestamp("updated_at"));
        return admin;
    }
}
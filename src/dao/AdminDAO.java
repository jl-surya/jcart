package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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
     * Retrieves all admins with pagination.
     * Super admins appear first in results.
     *
     * @param page the page number (1-indexed)
     * @param size the number of records per page
     * @return list of admins
     * @throws Exception if database operation fails
     */
    public List<Admin> getAll(int page, int size) throws Exception {
        String sql = "SELECT admin_id, username, email, password, phone, role, permissions, is_active, is_super_admin, created_at, updated_at " +
                     "FROM admins ORDER BY is_super_admin DESC, created_at DESC LIMIT ? OFFSET ?";
        List<Admin> admins = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    admins.add(mapRow(rs));
                }
            }
        }
        return admins;
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
     * Activates admin account.
     *
     * @param adminId the admin ID
     * @throws Exception if database operation fails
     */
    public void activate(String adminId) throws Exception {
        String sql = "UPDATE admins SET is_active = TRUE, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE admin_id = ? AND is_super_admin = FALSE";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, adminId);
            ps.executeUpdate();
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
     * Gets total count of all admins.
     *
     * @return total admin count
     * @throws Exception if database operation fails
     */
    public int getTotalCount() throws Exception {
        String sql = "SELECT COUNT(*) FROM admins";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
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
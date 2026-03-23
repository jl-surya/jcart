package model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Admin represents an administrator account in the system.
 * 
 * Includes:
 * - Personal information (username, email, phone)
 * - Authentication credentials (password)
 * - Role and permissions for access control
 * - Account status (active/inactive, super admin flag)
 * - Timestamps for tracking creation and updates
 * - Permission checking utility method
 */
public class Admin implements Serializable {
    
    private String adminId;
    private String username;
    private String email;
    private String password;
    private String phone;
    private String role;
    private String[] permissions;
    private boolean isActive;
    private boolean isSuperAdmin;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String[] getPermissions() { return permissions; }
    public void setPermissions(String[] permissions) { this.permissions = permissions; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isSuperAdmin() { return isSuperAdmin; }
    public void setSuperAdmin(boolean superAdmin) { isSuperAdmin = superAdmin; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * Checks if admin has a specific permission.
     * Super admins have all permissions automatically.
     *
     * @param permission the permission to check
     * @return true if admin has the permission, false otherwise
     */
    public boolean hasPermission(String permission) {
        if (isSuperAdmin) return true;
        if (permissions == null) return false;
        for (String p : permissions) {
            if (p.equals(permission) || p.equals("*")) {
                return true;
            }
        }
        return false;
    }
}
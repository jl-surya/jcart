package dto;

/**
 * AdminUpdateRequest holds profile update details for admin accounts.
 * 
 * Includes:
 * - Profile information (username, email, phone)
 * - Role and permissions for access control
 * - Account status (isActive) for activation/deactivation
 */
public class AdminUpdateRequest {
    private String username;
    private String email;
    private String phone;
    private String role;
    private String[] permissions;
    private Boolean isActive;
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String[] getPermissions() { return permissions; }
    public void setPermissions(String[] permissions) { this.permissions = permissions; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
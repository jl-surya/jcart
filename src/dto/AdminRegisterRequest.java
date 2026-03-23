package dto;

/**
 * AdminRegisterRequest holds registration details for new admin accounts.
 * 
 * Includes:
 * - User credentials (username, email, password)
 * - Password confirmation for validation
 * - Contact information (phone)
 * - Role assignment
 * - Permission array for access control
 */
public class AdminRegisterRequest {
    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private String phone;
    private String role;
    private String[] permissions;
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String[] getPermissions() { return permissions; }
    public void setPermissions(String[] permissions) { this.permissions = permissions; }
}
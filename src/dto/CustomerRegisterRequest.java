package dto;

/**
 * CustomerRegisterRequest holds registration details for new customers.
 * 
 * Includes:
 * - User credentials (username, email, password)
 * - Password confirmation for validation
 * - Contact information (phone)
 */
public class CustomerRegisterRequest {
    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private String phone;
    
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
}
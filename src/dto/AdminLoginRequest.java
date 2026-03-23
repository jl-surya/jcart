package dto;

/**
 * AdminLoginRequest holds login credentials for admin authentication.
 * 
 * Includes:
 * - Username or email field for flexible login
 * - Password for verification
 */
public class AdminLoginRequest {
    private String usernameOrEmail;
    private String password;
    
    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
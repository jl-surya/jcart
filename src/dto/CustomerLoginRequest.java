package dto;

/**
 * CustomerLoginRequest holds login credentials.
 * 
 * Includes:
 * - Username or email field for flexible authentication
 * - Password for verification
 */
public class CustomerLoginRequest {
    private String usernameOrEmail;
    private String password;
    
    public String getUsernameOrEmail() { return usernameOrEmail; }
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
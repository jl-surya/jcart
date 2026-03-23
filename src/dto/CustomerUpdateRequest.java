package dto;

/**
 * CustomerUpdateRequest holds profile update details.
 * 
 * Includes:
 * - Username for display name update
 * - Email for contact and login
 * - Phone for contact information
 */
public class CustomerUpdateRequest {
    private String username;
    private String email;
    private String phone;
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
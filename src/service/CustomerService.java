package service;

import dao.CustomerDAO;
import dao.SessionDAO;
import java.util.List;
import java.util.UUID;
import model.Customer;
import model.Session;
import util.PasswordUtil;
import util.SessionCache;

/**
 * CustomerService handles business logic for customer operations.
 * 
 * Includes:
 * - Customer registration with validation
 * - Login authentication with session creation
 * - Profile management (view, update)
 * - Password change functionality
 * - Account deactivation
 * - Session validation and caching
 * - Admin operations for customer management
 */
public class CustomerService {
    
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final SessionDAO sessionDAO = new SessionDAO();
     
    /**
     * Registers a new customer account.
     *
     * @param username the username
     * @param email the email address
     * @param password the password (min 6 characters)
     * @param phone the phone number (optional)
     * @return the created Customer object
     * @throws Exception if validation fails or customer already exists
     */
    public Customer register(String username, String email, String password, String phone) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        if (customerDAO.usernameExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (customerDAO.emailExists(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        Customer customer = new Customer();
        customer.setUsername(username.trim());
        customer.setEmail(email.trim().toLowerCase());
        customer.setPassword(PasswordUtil.hashPassword(password));
        customer.setPhone(phone != null ? phone.trim() : null);
        customer.setActive(true);
        
        customerDAO.insert(customer);
        return customer;
    }
    
    /**
     * Authenticates customer and creates a new session.
     *
     * @param usernameOrEmail username or email for login
     * @param password the password
     * @return the created Session object
     * @throws Exception if authentication fails
     */
    public Session login(String usernameOrEmail, String password) throws Exception {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Username/email is required");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        Customer customer = null;
        if (usernameOrEmail.contains("@")) {
            customer = customerDAO.getByEmail(usernameOrEmail.trim().toLowerCase());
        } else {
            customer = customerDAO.getByUsername(usernameOrEmail.trim());
        }
        
        if (customer == null) {
            throw new IllegalArgumentException("Invalid username/email or password");
        }
        if (!customer.isActive()) {
            throw new IllegalArgumentException("Account is deactivated. Please contact support.");
        }
        if (!PasswordUtil.verifyPassword(password, customer.getPassword())) {
            throw new IllegalArgumentException("Invalid username/email or password");
        }
        
        Session session = new Session();
        session.setSessionId(UUID.randomUUID());
        session.setUserType("CUSTOMER");
        session.setUserId(customer.getCustomerId());
        
        sessionDAO.create(session);
        SessionCache.put(session);
        session.markSynced();
        sessionDAO.updateSyncTime(session.getSessionId(), session.getLastSyncAt());
        
        return session;
    }
    
    /**
     * Logs out customer by removing session.
     *
     * @param sessionToken the session token to invalidate
     * @throws Exception if database operation fails
     */
    public void logout(String sessionToken) throws Exception {
        if (sessionToken != null) {
            SessionCache.removeByToken(sessionToken);
            sessionDAO.deleteByToken(sessionToken);
        }
    }
    
    /**
     * Validates session token and returns cached session.
     *
     * @param sessionToken the session token
     * @return Session object if valid, null otherwise
     */
    public Session validateSession(String sessionToken) {
        if (sessionToken == null) return null;
        return SessionCache.getByToken(sessionToken);
    }
    
    /**
     * Retrieves current customer from session.
     *
     * @param sessionToken the session token
     * @return Customer object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Customer getCurrentCustomer(String sessionToken) throws Exception {
        Session session = validateSession(sessionToken);
        if (session != null && "CUSTOMER".equals(session.getUserType())) {
            return customerDAO.getById(session.getUserId());
        }
        return null;
    }
    
    /**
     * Updates customer profile information.
     *
     * @param sessionToken the session token
     * @param username the new username
     * @param email the new email
     * @param phone the new phone number
     * @throws Exception if validation fails or update fails
     */
    public void updateProfile(String sessionToken, String username, String email, String phone) throws Exception {
        Customer customer = getCurrentCustomer(sessionToken);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found");
        }
        
        if (username != null && !username.isEmpty()) {
            if (!username.equals(customer.getUsername()) && customerDAO.usernameExists(username)) {
                throw new IllegalArgumentException("Username already exists");
            }
            customer.setUsername(username);
        }
        if (email != null && !email.isEmpty()) {
            if (!email.equals(customer.getEmail()) && customerDAO.emailExists(email)) {
                throw new IllegalArgumentException("Email already exists");
            }
            customer.setEmail(email);
        }
        if (phone != null) {
            customer.setPhone(phone);
        }
        
        customerDAO.update(customer);
    }
    
    /**
     * Changes customer password.
     *
     * @param sessionToken the session token
     * @param oldPassword the current password
     * @param newPassword the new password (min 6 characters)
     * @throws Exception if validation fails or password is incorrect
     */
    public void changePassword(String sessionToken, String oldPassword, String newPassword) throws Exception {
        Customer customer = getCurrentCustomer(sessionToken);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found");
        }
        
        if (!PasswordUtil.verifyPassword(oldPassword, customer.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        
        customerDAO.updatePassword(customer.getCustomerId(), PasswordUtil.hashPassword(newPassword));
    }

    /**
     * Deactivates customer account (soft delete).
     *
     * @param sessionToken the session token
     * @param password the password for confirmation
     * @throws Exception if validation fails or password is incorrect
     */
    public void deactivateAccount(String sessionToken, String password) throws Exception {
        Customer customer = getCurrentCustomer(sessionToken);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found");
        }
        
        if (!PasswordUtil.verifyPassword(password, customer.getPassword())) {
            throw new IllegalArgumentException("Invalid password");
        }
        
        customerDAO.deactivate(customer.getCustomerId());
        
        sessionDAO.deleteAllForUser(customer.getCustomerId(), "CUSTOMER");
        SessionCache.removeByToken(sessionToken);
    }
    
    /**
     * Retrieves paginated list of all customers.
     *
     * @param page the page number (1-indexed)
     * @param size the number of records per page
     * @return list of customers
     * @throws Exception if database operation fails
     */
    public List<Customer> getCustomers(int page, int size) throws Exception {
        return customerDAO.getAll(page, size);
    }
    
    /**
     * Retrieves paginated list of active customers.
     *
     * @param page the page number (1-indexed)
     * @param size the number of records per page
     * @return list of active customers
     * @throws Exception if database operation fails
     */
    public List<Customer> getActiveCustomers(int page, int size) throws Exception {
        return customerDAO.getAllActive(page, size);
    }
    
    /**
     * Retrieves customer by ID.
     *
     * @param id the customer ID
     * @return Customer object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Customer getCustomerById(String id) throws Exception {
        if (id == null || id.isBlank()) {
            return null;
        }
        return customerDAO.getById(id);
    }
    
    /**
     * Deactivates a customer by ID (admin operation).
     *
     * @param customerId the customer ID
     * @throws Exception if validation fails or customer not found
     */
    public void deactivateCustomer(String customerId) throws Exception {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        
        Customer customer = customerDAO.getById(customerId);
        if (customer == null) {
            throw new IllegalArgumentException("Customer not found");
        }
        
        customerDAO.deactivate(customerId);
    }
    
    /**
     * Gets total count of all customers.
     *
     * @return total customer count
     * @throws Exception if database operation fails
     */
    public int getTotalCustomers() throws Exception {
        return customerDAO.getTotalCount();
    }
    
    /**
     * Gets count of active customers.
     *
     * @return active customer count
     * @throws Exception if database operation fails
     */
    public int getActiveCustomersCount() throws Exception {
        return customerDAO.getActiveCount();
    }
}
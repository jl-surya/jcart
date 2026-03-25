package service;

import dao.AdminDAO;
import dao.SessionDAO;
import java.util.List;
import java.util.UUID;
import model.Admin;
import model.Session;
import util.PasswordUtil;
import util.SessionCache;

/**
 * AdminService handles business logic for admin operations.
 * 
 * Includes:
 * - Admin registration with validation
 * - Login authentication with session creation
 * - Profile management (view, update)
 * - Password change functionality
 * - Account activation/deactivation
 * - Permission-based access control
 * - Admin management (CRUD operations)
 */
public class AdminService {
    
    private final AdminDAO adminDAO = new AdminDAO();
    private final SessionDAO sessionDAO = new SessionDAO();
    
    // Permission constants for role-based access control
    public static final String PERM_ADMIN_VIEW = "admins:view";
    public static final String PERM_ADMIN_CREATE = "admins:create";
    public static final String PERM_ADMIN_UPDATE = "admins:update";
    public static final String PERM_ADMIN_DELETE = "admins:delete";
    public static final String PERM_CUSTOMER_VIEW = "customers:view";
    public static final String PERM_CUSTOMER_DELETE = "customers:delete";
    public static final String PERM_PRODUCT_VIEW = "products:view";
    public static final String PERM_PRODUCT_CREATE = "products:create";
    public static final String PERM_PRODUCT_UPDATE = "products:update";
    public static final String PERM_PRODUCT_DELETE = "products:delete";
    public static final String PERM_ORDER_VIEW = "orders:view";
    public static final String PERM_ORDER_UPDATE = "orders:update";
    public static final String PERM_TRANSACTION_VIEW = "transactions:view";
    public static final String PERM_TRANSACTION_UPDATE = "transactions:update";

    /**
     * Registers a new admin account.
     *
     * @param username the username
     * @param email the email address
     * @param password the password (min 6 characters)
     * @param phone the phone number (optional)
     * @param role the admin role
     * @param permissions the array of permissions
     * @return the created Admin object
     * @throws Exception if validation fails or admin already exists
     */
    public Admin register(String username, String email, String password, String phone, 
                          String role, String[] permissions) throws Exception {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        
        if (adminDAO.usernameExists(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (adminDAO.emailExists(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        
        Admin admin = new Admin();
        admin.setUsername(username.trim());
        admin.setEmail(email.trim().toLowerCase());
        admin.setPassword(PasswordUtil.hashPassword(password));
        admin.setPhone(phone != null ? phone.trim() : null);
        admin.setRole(role != null ? role : "ADMIN");
        admin.setPermissions(permissions != null ? permissions : new String[0]);
        admin.setActive(true);
        admin.setSuperAdmin(false);
        
        adminDAO.insert(admin);
        return admin;
    }
    
    /**
     * Authenticates admin and creates a new session.
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
        
        Admin admin = null;
        if (usernameOrEmail.contains("@")) {
            admin = adminDAO.getByEmail(usernameOrEmail.trim().toLowerCase());
        } else {
            admin = adminDAO.getByUsername(usernameOrEmail.trim());
        }
        
        if (admin == null) {
            throw new IllegalArgumentException("Invalid username/email or password");
        }
        if (!admin.isActive()) {
            throw new IllegalArgumentException("Account is deactivated");
        }
        if (!PasswordUtil.verifyPassword(password, admin.getPassword())) {
            throw new IllegalArgumentException("Invalid username/email or password");
        }
        
        Session session = new Session();
        session.setSessionId(UUID.randomUUID());
        session.setUserType("ADMIN");
        session.setUserId(admin.getAdminId());
        
        sessionDAO.create(session);
        SessionCache.put(session);
        session.markSynced();
        sessionDAO.updateSyncTime(session.getSessionId(), session.getLastSyncAt());
        
        return session;
    }
    
    /**
     * Logs out admin by removing session.
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
     * Retrieves current admin from session.
     *
     * @param sessionToken the session token
     * @return Admin object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Admin getCurrentAdmin(String sessionToken) throws Exception {
        Session session = validateSession(sessionToken);
        if (session != null && "ADMIN".equals(session.getUserType())) {
            return adminDAO.getById(session.getUserId());
        }
        return null;
    }
    
    /**
     * Retrieves admin by ID.
     *
     * @param adminId the admin ID
     * @return Admin object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Admin getAdminById(String adminId) throws Exception {
        return adminDAO.getById(adminId);
    }
    
    /**
     * Retrieves paginated list of all admins.
     *
     * @param page the page number (1-indexed)
     * @param size the number of records per page
     * @return list of admins
     * @throws Exception if database operation fails
     */
    public List<Admin> getAllAdmins(int page, int size) throws Exception {
        return adminDAO.getAll(page, size);
    }
    
    /**
     * Gets total count of all admins.
     *
     * @return total admin count
     * @throws Exception if database operation fails
     */
    public int getTotalAdmins() throws Exception {
        return adminDAO.getTotalCount();
    }
    
    /**
     * Updates admin account details (admin management operation).
     * Cannot modify own account or super admin accounts.
     *
     * @param adminId the admin ID to update
     * @param username the new username
     * @param email the new email
     * @param phone the new phone number
     * @param role the new role
     * @param permissions the new permissions array
     * @param isActive the new active status
     * @param currentAdmin the admin performing the update
     * @return updated Admin object
     * @throws Exception if validation fails or update not allowed
     */
    public Admin updateAdmin(String adminId, String username, String email, String phone, 
                             String role, String[] permissions, Boolean isActive, Admin currentAdmin) throws Exception {
        
        Admin admin = adminDAO.getById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("Admin not found");
        }
        
        if (admin.isSuperAdmin()) {
            throw new IllegalArgumentException("Cannot modify SUPER_ADMIN account");
        }

        if (adminId.equals(currentAdmin.getAdminId())) {
            throw new IllegalArgumentException("Cannot modify your own permissions or role");
        }
        
        if (username != null && !username.isEmpty()) {
            if (!username.equals(admin.getUsername()) && adminDAO.usernameExists(username)) {
                throw new IllegalArgumentException("Username already exists");
            }
            admin.setUsername(username);
        }
        if (email != null && !email.isEmpty()) {
            if (!email.equals(admin.getEmail()) && adminDAO.emailExists(email)) {
                throw new IllegalArgumentException("Email already exists");
            }
            admin.setEmail(email);
        }
        if (phone != null) {
            admin.setPhone(phone);
        }
        if (role != null && !role.isEmpty()) {
            admin.setRole(role);
        }
        if (permissions != null) {
            admin.setPermissions(permissions);
        }
        if (isActive != null) {
            admin.setActive(isActive);
        }
        
        adminDAO.update(admin);
        return admin;
    }
    
    /**
     * Updates admin's own profile information.
     *
     * @param adminId the admin ID
     * @param username the new username
     * @param email the new email
     * @param phone the new phone number
     * @return updated Admin object
     * @throws Exception if validation fails
     */
    public Admin updateOwnProfile(String adminId, String username, String email, String phone) throws Exception {
        Admin admin = adminDAO.getById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("Admin not found");
        }
        
        if (username != null && !username.isEmpty()) {
            if (!username.equals(admin.getUsername()) && adminDAO.usernameExists(username)) {
                throw new IllegalArgumentException("Username already exists");
            }
            admin.setUsername(username);
        }
        if (email != null && !email.isEmpty()) {
            if (!email.equals(admin.getEmail()) && adminDAO.emailExists(email)) {
                throw new IllegalArgumentException("Email already exists");
            }
            admin.setEmail(email);
        }
        if (phone != null) {
            admin.setPhone(phone);
        }
        
        adminDAO.updateProfile(adminId, admin.getUsername(), admin.getEmail(), admin.getPhone());
        return admin;
    }
    
    /**
     * Changes admin password.
     *
     * @param adminId the admin ID
     * @param oldPassword the current password
     * @param newPassword the new password (min 6 characters)
     * @throws Exception if validation fails or password is incorrect
     */
    public void changePassword(String adminId, String oldPassword, String newPassword) throws Exception {
        Admin admin = adminDAO.getById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("Admin not found");
        }
        
        if (!PasswordUtil.verifyPassword(oldPassword, admin.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        
        adminDAO.updatePassword(adminId, PasswordUtil.hashPassword(newPassword));
    }
    
    /**
     * Deactivates admin account.
     *
     * @param adminId the admin ID to deactivate
     * @param currentAdmin the admin performing the deactivation
     * @throws Exception if validation fails or operation not allowed
     */
    public void deactivateAdmin(String adminId, Admin currentAdmin) throws Exception {
        Admin admin = adminDAO.getById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("Admin not found");
        }
        
        if (admin.isSuperAdmin()) {
            throw new IllegalArgumentException("Cannot deactivate SUPER_ADMIN account");
        }
        
        if (adminId.equals(currentAdmin.getAdminId())) {
            throw new IllegalArgumentException("Cannot deactivate your own account");
        }
        
        adminDAO.deactivate(adminId);
        sessionDAO.deleteAllForUser(adminId, "ADMIN");
    }

    /**
     * Activates admin account.
     *
     * @param adminId the admin ID to activate
     * @param currentAdmin the admin performing the activation
     * @throws Exception if validation fails or operation not allowed
     */
    public void activateAdmin(String adminId, Admin currentAdmin) throws Exception {
        Admin admin = adminDAO.getById(adminId);
        if (admin == null) {
            throw new IllegalArgumentException("Admin not found");
        }
        
        if (admin.isSuperAdmin()) {
            throw new IllegalArgumentException("Cannot activate SUPER_ADMIN account");
        }
        
        adminDAO.activate(adminId);
    }
    
    /**
     * Checks if admin has a specific permission.
     *
     * @param admin the admin to check
     * @param permission the permission to verify
     * @return true if admin has permission, false otherwise
     */
    public boolean hasPermission(Admin admin, String permission) {
        if (admin == null) return false;
        return admin.hasPermission(permission);
    }
}
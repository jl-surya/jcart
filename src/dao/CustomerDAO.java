package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Customer;
import util.DBUtil;

/**
 * CustomerDAO handles database operations for customer entities.
 * 
 * Includes:
 * - CRUD operations for customer management
 * - Search by ID, username, email
 * - Paginated customer listing
 * - Account activation status management
 */
public class CustomerDAO {
    
    /**
     * Inserts a new customer into database with auto-generated customer_id.
     *
     * @param customer the customer object to insert
     * @throws Exception if database operation fails
     */
    public void insert(Customer customer) throws Exception {
        String sql = "INSERT INTO customers (username, email, password, phone, is_active) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING customer_id";
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, customer.getUsername());
                ps.setString(2, customer.getEmail());
                ps.setString(3, customer.getPassword());
                ps.setString(4, customer.getPhone());
                ps.setBoolean(5, customer.isActive());
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        customer.setCustomerId(rs.getString("customer_id"));
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
     * Retrieves customer by customer ID.
     *
     * @param customerId the customer ID to search
     * @return Customer object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Customer getById(String customerId) throws Exception {
        String sql = "SELECT customer_id, username, email, password, phone, is_active, created_at, updated_at " +
                     "FROM customers WHERE customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Retrieves customer by username.
     *
     * @param username the username to search
     * @return Customer object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Customer getByUsername(String username) throws Exception {
        String sql = "SELECT customer_id, username, email, password, phone, is_active, created_at, updated_at " +
                     "FROM customers WHERE username = ?";
        
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
     * Retrieves customer by email.
     *
     * @param email the email to search
     * @return Customer object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Customer getByEmail(String email) throws Exception {
        String sql = "SELECT customer_id, username, email, password, phone, is_active, created_at, updated_at " +
                     "FROM customers WHERE email = ?";
        
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
     * Retrieves all customers with pagination.
     *
     * @param page the page number (1-indexed)
     * @param size the number of records per page
     * @return list of customers
     * @throws Exception if database operation fails
     */
    public List<Customer> getAll(int page, int size) throws Exception {
        String sql = "SELECT customer_id, username, email, password, phone, is_active, created_at, updated_at " +
                     "FROM customers ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Customer> customers = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapRow(rs));
                }
            }
        }
        return customers;
    }
    
    /**
     * Retrieves all active customers with pagination.
     *
     * @param page the page number (1-indexed)
     * @param size the number of records per page
     * @return list of active customers
     * @throws Exception if database operation fails
     */
    public List<Customer> getAllActive(int page, int size) throws Exception {
        String sql = "SELECT customer_id, username, email, password, phone, is_active, created_at, updated_at " +
                     "FROM customers WHERE is_active = TRUE ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Customer> customers = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapRow(rs));
                }
            }
        }
        return customers;
    }
    
    /**
     * Updates customer profile information.
     *
     * @param customer the customer object with updated details
     * @throws Exception if database operation fails
     */
    public void update(Customer customer) throws Exception {
        String sql = "UPDATE customers SET username = ?, email = ?, phone = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getUsername());
            ps.setString(2, customer.getEmail());
            ps.setString(3, customer.getPhone());
            ps.setString(4, customer.getCustomerId());
            ps.executeUpdate();
        }
    }
    
    /**
     * Updates customer password.
     *
     * @param customerId the customer ID
     * @param newPassword the new hashed password
     * @throws Exception if database operation fails
     */
    public void updatePassword(String customerId, String newPassword) throws Exception {
        String sql = "UPDATE customers SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPassword);
            ps.setString(2, customerId);
            ps.executeUpdate();
        }
    }
    
    /**
     * Deactivates customer account (soft delete).
     *
     * @param customerId the customer ID
     * @throws Exception if database operation fails
     */
    public void deactivate(String customerId) throws Exception {
        String sql = "UPDATE customers SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
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
        String sql = "SELECT COUNT(*) FROM customers WHERE username = ?";
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
        String sql = "SELECT COUNT(*) FROM customers WHERE email = ?";
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
     * Gets total count of all customers.
     *
     * @return total customer count
     * @throws Exception if database operation fails
     */
    public int getTotalCount() throws Exception {
        String sql = "SELECT COUNT(*) FROM customers";
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
     * Gets count of active customers.
     *
     * @return active customer count
     * @throws Exception if database operation fails
     */
    public int getActiveCount() throws Exception {
        String sql = "SELECT COUNT(*) FROM customers WHERE is_active = TRUE";
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
     * Maps ResultSet row to Customer object.
     *
     * @param rs the ResultSet
     * @return mapped Customer object
     * @throws Exception if mapping fails
     */
    private Customer mapRow(ResultSet rs) throws Exception {
        Customer customer = new Customer();
        customer.setCustomerId(rs.getString("customer_id"));
        customer.setUsername(rs.getString("username"));
        customer.setEmail(rs.getString("email"));
        customer.setPassword(rs.getString("password"));
        customer.setPhone(rs.getString("phone"));
        customer.setActive(rs.getBoolean("is_active"));
        customer.setCreatedAt(rs.getTimestamp("created_at"));
        customer.setUpdatedAt(rs.getTimestamp("updated_at"));
        return customer;
    }
}
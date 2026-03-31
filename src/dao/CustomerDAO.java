package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * Gets all customers with filtering and pagination.
     *
     * @param page the page number (1-indexed)
     * @param size the number of records per page
     * @param search search term for username or email
     * @param status filter by active/inactive status
     * @return list of filtered customers
     * @throws Exception if database operation fails
     */
    public List<Customer> getAll(int page, int size, String search, String status, String sortBy, String sortDir) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT customer_id, username, email, password, phone, is_active, created_at, updated_at ");
        sql.append("FROM customers WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(username) LIKE ? OR LOWER(email) LIKE ?) ");
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (status != null && !status.trim().isEmpty()) {
            if ("active".equalsIgnoreCase(status)) {
                sql.append("AND is_active = TRUE ");
            } else if ("inactive".equalsIgnoreCase(status)) {
                sql.append("AND is_active = FALSE ");
            }
        }
        
        String validatedSortBy = validateSortColumn(sortBy);
        String validatedSortDir = (sortDir != null && sortDir.equalsIgnoreCase("asc")) ? "ASC" : "DESC";
        
        sql.append("ORDER BY ");
        if (validatedSortBy != null && !validatedSortBy.isEmpty()) {
            sql.append(validatedSortBy).append(" ").append(validatedSortDir).append(", ");
        }
        sql.append("created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add((page - 1) * size);
        
        List<Customer> customers = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapRow(rs));
                }
            }
        }
        return customers;
    }

    /**
     * Gets total count of filtered customers.
     *
     * @param search search term for username or email
     * @param status filter by active/inactive status
     * @return total count of matching customers
     * @throws Exception if database operation fails
     */
    public int getAllCount(String search, String status) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM customers WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(username) LIKE ? OR LOWER(email) LIKE ?) ");
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (status != null && !status.trim().isEmpty()) {
            if ("active".equalsIgnoreCase(status)) {
                sql.append("AND is_active = TRUE ");
            } else if ("inactive".equalsIgnoreCase(status)) {
                sql.append("AND is_active = FALSE ");
            }
        }
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    /**
     * Gets stats (active count, inactive count) for search results.
     * Consolidated method following product management pattern.
     *
     * @param search search term (optional)
     * @param status filter by active/inactive status (optional)
     * @return map with activeCount and inactiveCount
     * @throws Exception if database operation fails
     */
    public Map<String, Integer> getStats(String search, String status) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("SUM(CASE WHEN is_active = TRUE THEN 1 ELSE 0 END) as active_count, ");
        sql.append("SUM(CASE WHEN is_active = FALSE THEN 1 ELSE 0 END) as inactive_count ");
        sql.append("FROM customers WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(username) LIKE ? OR LOWER(email) LIKE ?) ");
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (status != null && !status.trim().isEmpty()) {
            if ("active".equalsIgnoreCase(status.trim())) {
                sql.append("AND is_active = TRUE ");
            } else if ("inactive".equalsIgnoreCase(status.trim())) {
                sql.append("AND is_active = FALSE ");
            }
        }
        
        Map<String, Integer> stats = new HashMap<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("activeCount", rs.getInt("active_count"));
                    stats.put("inactiveCount", rs.getInt("inactive_count"));
                }
            }
        }
        
        return stats;
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
     * Validates and returns safe sort column name for SQL.
     *
     * @param sortBy column name to validate
     * @return validated column name or null if invalid
     */
    private String validateSortColumn(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            return null;
        }
        
        switch (sortBy.toLowerCase()) {
            case "username":
                return "username";
            case "email":
                return "email";
            case "created_at":
            case "createdat":
                return "created_at";
            default:
                return null;
        }
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
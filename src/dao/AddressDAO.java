package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Address;
import util.DBUtil;

/**
 * AddressDAO handles database operations for customer addresses.
 * 
 * Includes:
 * - CRUD operations for addresses
 * - Default address management
 * - Address listing by customer
 * - Address count retrieval
 */
public class AddressDAO {
    
    /**
     * Inserts a new address for a customer.
     *
     * @param address the address object to insert
     * @throws Exception if database operation fails
     */
    public void insert(Address address) throws Exception {
        String sql = "INSERT INTO addresses (customer_id, recipient_name, address_line, city, state, postal_code, country, phone, is_default) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING address_id";
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, address.getCustomerId());
                ps.setString(2, address.getRecipientName());
                ps.setString(3, address.getAddressLine());
                ps.setString(4, address.getCity());
                ps.setString(5, address.getState());
                ps.setString(6, address.getPostalCode());
                ps.setString(7, address.getCountry());
                ps.setString(8, address.getPhone());
                ps.setBoolean(9, address.isDefault());
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        address.setAddressId(rs.getLong("address_id"));
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
     * Retrieves address by ID for a specific customer.
     *
     * @param addressId the address ID
     * @param customerId the customer ID
     * @return Address object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Address getById(Long addressId, String customerId) throws Exception {
        String sql = "SELECT address_id, customer_id, recipient_name, address_line, city, state, postal_code, country, phone, is_default, created_at " +
                     "FROM addresses WHERE address_id = ? AND customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, addressId);
            ps.setString(2, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Retrieves all addresses for a customer.
     *
     * @param customerId the customer ID
     * @return list of addresses
     * @throws Exception if database operation fails
     */
    public List<Address> getAllByCustomer(String customerId) throws Exception {
        String sql = "SELECT address_id, customer_id, recipient_name, address_line, city, state, postal_code, country, phone, is_default, created_at " +
                     "FROM addresses WHERE customer_id = ? ORDER BY is_default DESC, created_at DESC";
        List<Address> addresses = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    addresses.add(mapRow(rs));
                }
            }
        }
        return addresses;
    }
    
    /**
     * Retrieves default address for a customer.
     *
     * @param customerId the customer ID
     * @return default Address if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Address getDefaultByCustomer(String customerId) throws Exception {
        String sql = "SELECT address_id, customer_id, recipient_name, address_line, city, state, postal_code, country, phone, is_default, created_at " +
                     "FROM addresses WHERE customer_id = ? AND is_default = TRUE";
        
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
     * Updates an existing address.
     *
     * @param address the address object with updated details
     * @throws Exception if database operation fails
     */
    public void update(Address address) throws Exception {
        String sql = "UPDATE addresses SET recipient_name = ?, address_line = ?, city = ?, state = ?, postal_code = ?, country = ?, phone = ? " +
                     "WHERE address_id = ? AND customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, address.getRecipientName());
            ps.setString(2, address.getAddressLine());
            ps.setString(3, address.getCity());
            ps.setString(4, address.getState());
            ps.setString(5, address.getPostalCode());
            ps.setString(6, address.getCountry());
            ps.setString(7, address.getPhone());
            ps.setLong(8, address.getAddressId());
            ps.setString(9, address.getCustomerId());
            ps.executeUpdate();
        }
    }
    
    /**
     * Clears the default flag from all addresses for a customer.
     * Used internally when setting a new default address.
     *
     * @param customerId the customer ID
     * @throws Exception if database operation fails
     */
    public void clearDefault(String customerId) throws Exception {
        String sql = "UPDATE addresses SET is_default = FALSE WHERE customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.executeUpdate();
        }
    }
    
    /**
     * Sets an address as default for a customer.
     * Clears default flag from all other addresses first.
     *
     * @param addressId the address ID to set as default
     * @param customerId the customer ID
     * @throws Exception if database operation fails
     */
    public void setDefault(Long addressId, String customerId) throws Exception {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                String clearDefaultSql = "UPDATE addresses SET is_default = FALSE WHERE customer_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(clearDefaultSql)) {
                    ps.setString(1, customerId);
                    ps.executeUpdate();
                }
                
                String setDefaultSql = "UPDATE addresses SET is_default = TRUE WHERE address_id = ? AND customer_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(setDefaultSql)) {
                    ps.setLong(1, addressId);
                    ps.setString(2, customerId);
                    ps.executeUpdate();
                }
                
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    /**
     * Deletes an address.
     *
     * @param addressId the address ID to delete
     * @param customerId the customer ID
     * @throws Exception if database operation fails
     */
    public void delete(Long addressId, String customerId) throws Exception {
        String sql = "DELETE FROM addresses WHERE address_id = ? AND customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, addressId);
            ps.setString(2, customerId);
            ps.executeUpdate();
        }
    }
    
    /**
     * Gets total count of addresses for a customer.
     *
     * @param customerId the customer ID
     * @return number of addresses
     * @throws Exception if database operation fails
     */
    public int getCountByCustomer(String customerId) throws Exception {
        String sql = "SELECT COUNT(*) FROM addresses WHERE customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    /**
     * Maps ResultSet row to Address object.
     *
     * @param rs the ResultSet
     * @return mapped Address object
     * @throws Exception if mapping fails
     */
    private Address mapRow(ResultSet rs) throws Exception {
        Address address = new Address();
        address.setAddressId(rs.getLong("address_id"));
        address.setCustomerId(rs.getString("customer_id"));
        address.setRecipientName(rs.getString("recipient_name"));
        address.setAddressLine(rs.getString("address_line"));
        address.setCity(rs.getString("city"));
        address.setState(rs.getString("state"));
        address.setPostalCode(rs.getString("postal_code"));
        address.setCountry(rs.getString("country"));
        address.setPhone(rs.getString("phone"));
        address.setDefault(rs.getBoolean("is_default"));
        address.setCreatedAt(rs.getTimestamp("created_at"));
        return address;
    }
}
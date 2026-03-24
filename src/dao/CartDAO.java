package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.CartItem;
import util.DBUtil;

/**
 * CartDAO handles database operations for shopping cart items.
 * 
 * Includes:
 * - Add, update, delete cart items
 * - Clear entire cart
 * - Retrieve cart items with product details
 * - Delete expired cart items (cleanup task)
 */
public class CartDAO {
    
    /**
     * Inserts a new item into cart with 30-day expiration.
     *
     * @param cartItem the cart item to insert
     * @throws Exception if database operation fails
     */
    public void insert(CartItem cartItem) throws Exception {
        String sql = "INSERT INTO cart_items (customer_id, product_id, quantity, added_at, updated_at, expires_at) " +
                     "VALUES (?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days')";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cartItem.getCustomerId());
            ps.setString(2, cartItem.getProductId());
            ps.setInt(3, cartItem.getQuantity());
            ps.executeUpdate();
        }
    }
    
    /**
     * Updates quantity of an existing cart item.
     *
     * @param customerId the customer ID
     * @param productId the product ID
     * @param quantity the new quantity
     * @throws Exception if database operation fails
     */
    public void updateQuantity(String customerId, String productId, int quantity) throws Exception {
        String sql = "UPDATE cart_items SET quantity = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE customer_id = ? AND product_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setString(2, customerId);
            ps.setString(3, productId);
            ps.executeUpdate();
        }
    }
    
    /**
     * Removes a specific item from cart.
     *
     * @param customerId the customer ID
     * @param productId the product ID
     * @throws Exception if database operation fails
     */
    public void delete(String customerId, String productId) throws Exception {
        String sql = "DELETE FROM cart_items WHERE customer_id = ? AND product_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }
    
    /**
     * Clears all items from customer's cart.
     *
     * @param customerId the customer ID
     * @throws Exception if database operation fails
     */
    public void clear(String customerId) throws Exception {
        String sql = "DELETE FROM cart_items WHERE customer_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.executeUpdate();
        }
    }
    
    /**
     * Retrieves a cart item by product ID with product details.
     *
     * @param customerId the customer ID
     * @param productId the product ID
     * @return CartItem if found, null otherwise
     * @throws Exception if database operation fails
     */
    public CartItem getByProduct(String customerId, String productId) throws Exception {
        String sql = "SELECT ci.customer_id, ci.product_id, ci.quantity, ci.added_at, ci.updated_at, ci.expires_at, " +
                     "p.product_name, p.price, p.discount, p.stock_level " +
                     "FROM cart_items ci JOIN products p ON ci.product_id = p.product_id " +
                     "WHERE ci.customer_id = ? AND ci.product_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            ps.setString(2, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Retrieves all cart items for a customer with product details.
     *
     * @param customerId the customer ID
     * @return list of cart items
     * @throws Exception if database operation fails
     */
    public List<CartItem> getAllByCustomer(String customerId) throws Exception {
        String sql = "SELECT ci.customer_id, ci.product_id, ci.quantity, ci.added_at, ci.updated_at, ci.expires_at, " +
                     "p.product_name, p.price, p.discount, p.stock_level " +
                     "FROM cart_items ci JOIN products p ON ci.product_id = p.product_id " +
                     "WHERE ci.customer_id = ? ORDER BY ci.added_at DESC";
        List<CartItem> items = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }
    
    /**
     * Deletes all expired cart items.
     * Called by scheduled task for periodic cleanup.
     *
     * @throws Exception if database operation fails
     */
    public void deleteExpiredItems() throws Exception {
        String sql = "DELETE FROM cart_items WHERE expires_at < CURRENT_TIMESTAMP";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }
    
    /**
     * Maps ResultSet row to CartItem object.
     *
     * @param rs the ResultSet
     * @return mapped CartItem object
     * @throws Exception if mapping fails
     */
    private CartItem mapRow(ResultSet rs) throws Exception {
        CartItem item = new CartItem();
        item.setCustomerId(rs.getString("customer_id"));
        item.setProductId(rs.getString("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setQuantity(rs.getInt("quantity"));
        item.setPrice(rs.getBigDecimal("price"));
        item.setDiscount(rs.getBigDecimal("discount"));
        item.setStockLevel(rs.getInt("stock_level"));
        item.setAddedAt(rs.getTimestamp("added_at"));
        item.setUpdatedAt(rs.getTimestamp("updated_at"));
        item.setExpiresAt(rs.getTimestamp("expires_at"));
        return item;
    }
}
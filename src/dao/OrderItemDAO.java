package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.OrderItem;
import util.DBUtil;

/**
 * OrderItemDAO handles database operations for order items.
 * 
 * Includes:
 * - Insert single order item
 * - Batch insert multiple order items
 * - Retrieve items by order ID
 */
public class OrderItemDAO {
    
    /**
     * Inserts a single order item into database.
     *
     * @param item the order item to insert
     * @throws Exception if database operation fails
     */
    public void insert(OrderItem item) throws Exception {
        String sql = "INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, " +
                     "discount, tax_rate, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, item.getOrderId());
            ps.setString(2, item.getProductId());
            ps.setString(3, item.getProductName());
            ps.setBigDecimal(4, item.getUnitPrice());
            ps.setInt(5, item.getQuantity());
            ps.setBigDecimal(6, item.getDiscount());
            ps.setBigDecimal(7, item.getTaxRate());
            ps.setBigDecimal(8, item.getSubtotal());
            ps.executeUpdate();
        }
    }
    
    /**
     * Inserts multiple order items in batch for better performance.
     *
     * @param items list of order items to insert
     * @throws Exception if database operation fails
     */
    public void insertBatch(List<OrderItem> items) throws Exception {
        String sql = "INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity, " +
                     "discount, tax_rate, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (OrderItem item : items) {
                    ps.setLong(1, item.getOrderId());
                    ps.setString(2, item.getProductId());
                    ps.setString(3, item.getProductName());
                    ps.setBigDecimal(4, item.getUnitPrice());
                    ps.setInt(5, item.getQuantity());
                    ps.setBigDecimal(6, item.getDiscount());
                    ps.setBigDecimal(7, item.getTaxRate());
                    ps.setBigDecimal(8, item.getSubtotal());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    /**
     * Retrieves all order items for a specific order.
     *
     * @param orderId the order ID
     * @return list of order items
     * @throws Exception if database operation fails
     */
    public List<OrderItem> getByOrderId(Long orderId) throws Exception {
        String sql = "SELECT order_item_id, order_id, product_id, product_name, unit_price, quantity, " +
                     "discount, tax_rate, subtotal FROM order_items WHERE order_id = ?";
        List<OrderItem> items = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapRow(rs));
                }
            }
        }
        return items;
    }
    
    /**
     * Maps ResultSet row to OrderItem object.
     *
     * @param rs the ResultSet
     * @return mapped OrderItem object
     * @throws Exception if mapping fails
     */
    private OrderItem mapRow(ResultSet rs) throws Exception {
        OrderItem item = new OrderItem();
        item.setOrderItemId(rs.getLong("order_item_id"));
        item.setOrderId(rs.getLong("order_id"));
        item.setProductId(rs.getString("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setUnitPrice(rs.getBigDecimal("unit_price"));
        item.setQuantity(rs.getInt("quantity"));
        item.setDiscount(rs.getBigDecimal("discount"));
        item.setTaxRate(rs.getBigDecimal("tax_rate"));
        item.setSubtotal(rs.getBigDecimal("subtotal"));
        return item;
    }
}
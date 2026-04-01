package dao;

import dto.OrderSearchRequest;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Order;
import util.DBUtil;

/**
 * OrderDAO handles database operations for orders.
 * 
 * Includes:
 * - Create new order
 * - Update shipping address
 * - Retrieve orders by ID or customer
 * - List orders with filters and pagination
 * - Count orders for pagination
 * - Update order status
 * - Cancel orders
 * - Automated cancellation of expired pending orders
 */
public class OrderDAO {
    
    /**
     * Inserts a new order into database.
     *
     * @param order the order object to insert
     * @throws Exception if database operation fails
     */
    public void insert(Order order) throws Exception {
        String sql = "INSERT INTO orders (customer_id, order_status, payment_status, total_amount, " +
                     "shipping_name, shipping_address_line, shipping_city, shipping_state, shipping_postal_code, " +
                     "shipping_country, invoice_number, payment_deadline) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING order_id";
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, order.getCustomerId());
                ps.setString(2, order.getOrderStatus());
                ps.setString(3, order.getPaymentStatus());
                ps.setBigDecimal(4, order.getTotalAmount());
                ps.setString(5, order.getShippingName());
                ps.setString(6, order.getShippingAddressLine());
                ps.setString(7, order.getShippingCity());
                ps.setString(8, order.getShippingState());
                ps.setString(9, order.getShippingPostalCode());
                ps.setString(10, order.getShippingCountry());
                ps.setString(11, order.getInvoiceNumber());
                ps.setTimestamp(12, order.getPaymentDeadline());
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        order.setOrderId(rs.getLong("order_id"));
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
     * Retrieves order by ID.
     *
     * @param orderId the order ID
     * @return Order object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Order getById(Long orderId) throws Exception {
        String sql = "SELECT order_id, customer_id, order_status, payment_status, total_amount, " +
                     "shipping_name, shipping_address_line, shipping_city, shipping_state, shipping_postal_code, " +
                     "shipping_country, invoice_number, created_at, updated_at, cancelled_at, cancelled_by, payment_deadline " +
                     "FROM orders WHERE order_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves orders with filters and pagination.
     *
     * @param filter the search request with filter criteria
     * @param offset pagination offset
     * @param limit pagination limit
     * @return list of orders matching criteria
     * @throws Exception if database operation fails
     */
    public List<Order> getAll(OrderSearchRequest filter, int offset, int limit) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT order_id, customer_id, order_status, payment_status, total_amount, ")
           .append("shipping_name, shipping_address_line, shipping_city, shipping_state, shipping_postal_code, ")
           .append("shipping_country, invoice_number, created_at, updated_at, cancelled_at, cancelled_by, payment_deadline ")
           .append("FROM orders WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            sql.append("AND (CAST(customer_id AS TEXT) LIKE ? OR LOWER(invoice_number) LIKE LOWER(?)) ");
            String searchPattern = "%" + filter.getKeyword() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (filter.getCustomerId() != null && !filter.getCustomerId().isEmpty()) {
            sql.append("AND customer_id = ? ");
            params.add(filter.getCustomerId());
        }
        
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND order_status = ? ");
            params.add(filter.getStatus());
        }
        
        if (filter.getPaymentStatus() != null && !filter.getPaymentStatus().isEmpty()) {
            sql.append("AND payment_status = ? ");
            params.add(filter.getPaymentStatus());
        }
        
        if (filter.getFromDate() != null) {
            sql.append("AND created_at >= ? ");
            params.add(Timestamp.valueOf(filter.getFromDate() + " 00:00:00"));
        }
        
        if (filter.getToDate() != null) {
            sql.append("AND created_at <= ? ");
            params.add(Timestamp.valueOf(filter.getToDate() + " 23:59:59"));
        }
        
        if (filter.getMinAmount() != null) {
            sql.append("AND total_amount >= ? ");
            params.add(BigDecimal.valueOf(filter.getMinAmount()));
        }
        
        if (filter.getMaxAmount() != null) {
            sql.append("AND total_amount <= ? ");
            params.add(BigDecimal.valueOf(filter.getMaxAmount()));
        }
        
        String validSortBy = validateSortColumn(filter.getSortByOrDefault());
        String validSortDir = filter.getSortDirOrDefault();
        sql.append(" ORDER BY ").append(validSortBy).append(" ").append(validSortDir);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        
        List<Order> orders = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapRow(rs));
                }
            }
        }
        return orders;
    }

    /**
     * Counts orders matching search criteria.
     *
     * @param filter the search request with filter criteria
     * @return total count of matching orders
     * @throws Exception if database operation fails
     */
    public int getAllCount(OrderSearchRequest filter) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM orders WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            sql.append("AND (CAST(customer_id AS TEXT) LIKE ? OR LOWER(invoice_number) LIKE LOWER(?)) ");
            String searchPattern = "%" + filter.getKeyword() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (filter.getCustomerId() != null && !filter.getCustomerId().isEmpty()) {
            sql.append("AND customer_id = ? ");
            params.add(filter.getCustomerId());
        }
        
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND order_status = ? ");
            params.add(filter.getStatus());
        }
        
        if (filter.getPaymentStatus() != null && !filter.getPaymentStatus().isEmpty()) {
            sql.append("AND payment_status = ? ");
            params.add(filter.getPaymentStatus());
        }
        
        if (filter.getFromDate() != null) {
            sql.append("AND created_at >= ? ");
            params.add(Timestamp.valueOf(filter.getFromDate() + " 00:00:00"));
        }
        
        if (filter.getToDate() != null) {
            sql.append("AND created_at <= ? ");
            params.add(Timestamp.valueOf(filter.getToDate() + " 23:59:59"));
        }
        
        if (filter.getMinAmount() != null) {
            sql.append("AND total_amount >= ? ");
            params.add(BigDecimal.valueOf(filter.getMinAmount()));
        }
        
        if (filter.getMaxAmount() != null) {
            sql.append("AND total_amount <= ? ");
            params.add(BigDecimal.valueOf(filter.getMaxAmount()));
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
     * Gets order statistics based on filter criteria.
     *
     * @param filter the search request with filter criteria
     * @return Map containing order counts by status (pending, processing, shipped, delivered, cancelled)
     * @throws Exception if database operation fails
     */
    public java.util.Map<String, Integer> getStats(OrderSearchRequest filter) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("SUM(CASE WHEN order_status = 'PENDING' THEN 1 ELSE 0 END) as pending_count, ");
        sql.append("SUM(CASE WHEN order_status = 'PROCESSING' THEN 1 ELSE 0 END) as processing_count, ");
        sql.append("SUM(CASE WHEN order_status = 'SHIPPED' THEN 1 ELSE 0 END) as shipped_count, ");
        sql.append("SUM(CASE WHEN order_status = 'DELIVERED' THEN 1 ELSE 0 END) as delivered_count, ");
        sql.append("SUM(CASE WHEN order_status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelled_count ");
        sql.append("FROM orders WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
            sql.append("AND (CAST(customer_id AS TEXT) LIKE ? OR LOWER(invoice_number) LIKE LOWER(?)) ");
            String searchPattern = "%" + filter.getKeyword() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (filter.getCustomerId() != null && !filter.getCustomerId().isEmpty()) {
            sql.append("AND customer_id = ? ");
            params.add(filter.getCustomerId());
        }
        
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND order_status = ? ");
            params.add(filter.getStatus());
        }
        
        if (filter.getPaymentStatus() != null && !filter.getPaymentStatus().isEmpty()) {
            sql.append("AND payment_status = ? ");
            params.add(filter.getPaymentStatus());
        }
        
        if (filter.getFromDate() != null) {
            sql.append("AND created_at >= ? ");
            params.add(Timestamp.valueOf(filter.getFromDate() + " 00:00:00"));
        }
        
        if (filter.getToDate() != null) {
            sql.append("AND created_at <= ? ");
            params.add(Timestamp.valueOf(filter.getToDate() + " 23:59:59"));
        }
        
        if (filter.getMinAmount() != null) {
            sql.append("AND total_amount >= ? ");
            params.add(BigDecimal.valueOf(filter.getMinAmount()));
        }
        
        if (filter.getMaxAmount() != null) {
            sql.append("AND total_amount <= ? ");
            params.add(BigDecimal.valueOf(filter.getMaxAmount()));
        }
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                java.util.Map<String, Integer> stats = new java.util.HashMap<>();
                if (rs.next()) {
                    stats.put("pending", rs.getInt("pending_count"));
                    stats.put("processing", rs.getInt("processing_count"));
                    stats.put("shipped", rs.getInt("shipped_count"));
                    stats.put("delivered", rs.getInt("delivered_count"));
                    stats.put("cancelled", rs.getInt("cancelled_count"));
                }
                return stats;
            }
        }
    }

    /**
     * Updates shipping address for an order.
     *
     * @param order the order with updated shipping details
     * @throws Exception if database operation fails
     */
    public void updateShippingAddress(Order order) throws Exception {
        String sql = "UPDATE orders SET shipping_name = ?, shipping_address_line = ?, shipping_city = ?, " +
                    "shipping_state = ?, shipping_postal_code = ?, shipping_country = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE order_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getShippingName());
            ps.setString(2, order.getShippingAddressLine());
            ps.setString(3, order.getShippingCity());
            ps.setString(4, order.getShippingState());
            ps.setString(5, order.getShippingPostalCode());
            ps.setString(6, order.getShippingCountry());
            ps.setLong(7, order.getOrderId());
            ps.executeUpdate();
        }
    }
        
    /**
     * Updates order status and payment status.
     *
     * @param orderId the order ID
     * @param orderStatus the new order status
     * @param paymentStatus the new payment status
     * @return number of rows updated
     * @throws Exception if database operation fails
     */
    public int updateStatus(Long orderId, String orderStatus, String paymentStatus) throws Exception {
        String sql = "UPDATE orders SET order_status = ?, payment_status = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE order_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderStatus);
            ps.setString(2, paymentStatus);
            ps.setLong(3, orderId);
            int updated = ps.executeUpdate();
            return updated;
        }
    }
    
    /**
     * Cancels an order and updates related records.
     *
     * @param orderId the order ID
     * @param cancelledBy the user who cancelled the order
     * @throws Exception if database operation fails
     */
    public void cancel(Long orderId, String cancelledBy) throws Exception {
        String sql = "UPDATE orders SET order_status = 'CANCELLED', payment_status = 'REFUNDED', " +
                     "cancelled_at = CURRENT_TIMESTAMP, cancelled_by = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE order_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cancelledBy);
            ps.setLong(2, orderId);
            ps.executeUpdate();
        }
    }

    /**
     * Automatically cancels expired pending orders and restores stock.
     * Uses CTE to perform multiple operations atomically.
     *
     * @throws Exception if database operation fails
     */
    public void cancelExpiredOrders() throws Exception {
        String sql = "WITH expired_orders AS (" +
                    "    SELECT order_id FROM orders " +
                    "    WHERE order_status = 'PENDING' AND payment_deadline < CURRENT_TIMESTAMP" +
                    "), " +
                    "update_orders AS (" +
                    "    UPDATE orders SET order_status = 'CANCELLED', payment_status = 'FAILED', " +
                    "    updated_at = CURRENT_TIMESTAMP " +
                    "    WHERE order_id IN (SELECT order_id FROM expired_orders) " +
                    "    RETURNING order_id" +
                    "), " +
                    "update_transactions AS (" +
                    "    UPDATE transactions SET transaction_status = 'FAILED' " +
                    "    WHERE order_id IN (SELECT order_id FROM expired_orders) " +
                    "    AND transaction_type = 'PAYMENT' AND transaction_status = 'INITIATED'" +
                    ") " +
                    "UPDATE products p SET stock_level = stock_level + oi.quantity " +
                    "FROM order_items oi, update_orders uo " +
                    "WHERE oi.order_id = uo.order_id AND p.product_id = oi.product_id";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    /**
     * Validates sort column to prevent SQL injection.
     *
     * @param sortBy the sort column
     * @return validated sort column name
     */
    private String validateSortColumn(String sortBy) {
        if (sortBy == null) return "created_at";
        switch (sortBy) {
            case "created_at":
            case "total_amount":
            case "total":
            case "order_status":
            case "payment_status":
            case "customer_id":
                return sortBy.equals("total") ? "total_amount" : sortBy;
            default:
                return "created_at";
        }
    }
    
    /**
     * Maps ResultSet row to Order object.
     *
     * @param rs the ResultSet
     * @return mapped Order object
     * @throws Exception if mapping fails
     */
    private Order mapRow(ResultSet rs) throws Exception {
        Order order = new Order();
        order.setOrderId(rs.getLong("order_id"));
        order.setCustomerId(rs.getString("customer_id"));
        order.setOrderStatus(rs.getString("order_status"));
        order.setPaymentStatus(rs.getString("payment_status"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setShippingName(rs.getString("shipping_name"));
        order.setShippingAddressLine(rs.getString("shipping_address_line"));
        order.setShippingCity(rs.getString("shipping_city"));
        order.setShippingState(rs.getString("shipping_state"));
        order.setShippingPostalCode(rs.getString("shipping_postal_code"));
        order.setShippingCountry(rs.getString("shipping_country"));
        order.setInvoiceNumber(rs.getString("invoice_number"));
        order.setCreatedAt(rs.getTimestamp("created_at"));
        order.setUpdatedAt(rs.getTimestamp("updated_at"));
        order.setCancelledAt(rs.getTimestamp("cancelled_at"));
        order.setCancelledBy(rs.getString("cancelled_by"));
        order.setPaymentDeadline(rs.getTimestamp("payment_deadline"));
        return order;
    }
}
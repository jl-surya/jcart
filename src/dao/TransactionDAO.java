package dao;

import dto.TransactionFilterRequest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Transaction;
import util.DBUtil;

/**
 * TransactionDAO handles database operations for financial transactions.
 * 
 * Includes:
 * - Create payment/refund transactions
 * - Retrieve transactions by ID, order, or customer
 * - List transactions with filters and pagination
 * - Count transactions for pagination
 * - Update transaction status
 * - Update refund reason
 */
public class TransactionDAO {
    
    /**
     * Inserts a new transaction.
     *
     * @param transaction the transaction to insert
     * @throws Exception if database operation fails
     */
    public void insert(Transaction transaction) throws Exception {
        String sql = "INSERT INTO transactions (order_id, transaction_type, transaction_method, transaction_status, " +
                     "amount, transaction_reference, refund_reason, processed_by_type, processed_by, processed_at, " +
                     "verified_by, verified_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING transaction_id";
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, transaction.getOrderId());
                ps.setString(2, transaction.getTransactionType());
                ps.setString(3, transaction.getTransactionMethod());
                ps.setString(4, transaction.getTransactionStatus());
                ps.setBigDecimal(5, transaction.getAmount());
                ps.setString(6, transaction.getTransactionReference());
                ps.setString(7, transaction.getRefundReason());
                ps.setString(8, transaction.getProcessedByType());
                ps.setString(9, transaction.getProcessedBy());
                ps.setTimestamp(10, transaction.getProcessedAt());
                ps.setString(11, transaction.getVerifiedBy());
                ps.setTimestamp(12, transaction.getVerifiedAt());
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        transaction.setTransactionId(rs.getLong("transaction_id"));
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
     * Retrieves transaction by ID.
     *
     * @param transactionId the transaction ID
     * @return Transaction object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Transaction getById(Long transactionId) throws Exception {
        String sql = "SELECT transaction_id, order_id, transaction_type, transaction_method, transaction_status, " +
                     "amount, transaction_reference, refund_reason, processed_by_type, processed_by, processed_at, " +
                     "verified_by, verified_at, created_at FROM transactions WHERE transaction_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Retrieves all transactions for a specific order.
     *
     * @param orderId the order ID
     * @return list of Transaction objects
     * @throws Exception if database operation fails
     */
    public List<Transaction> getAllByOrderId(Long orderId) throws Exception {
        String sql = "SELECT transaction_id, order_id, transaction_type, transaction_method, transaction_status, " +
                     "amount, transaction_reference, refund_reason, processed_by_type, processed_by, processed_at, " +
                     "verified_by, verified_at, created_at FROM transactions WHERE order_id = ? ORDER BY created_at DESC";
        
        List<Transaction> transactions = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
            }
        }
        return transactions;
    }
    
    /**
     * Retrieves transactions for a specific customer with filters.
     *
     * @param customerId the customer ID
     * @param filter the filter criteria
     * @param offset pagination offset
     * @param limit pagination limit
     * @return list of transactions
     * @throws Exception if database operation fails
     */
    public List<Transaction> getByCustomerId(String customerId, TransactionFilterRequest filter, int offset, int limit) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.transaction_id, t.order_id, t.transaction_type, t.transaction_method, t.transaction_status, ")
           .append("t.amount, t.transaction_reference, t.refund_reason, t.processed_by_type, t.processed_by, ")
           .append("t.processed_at, t.verified_by, t.verified_at, t.created_at ")
           .append("FROM transactions t JOIN orders o ON t.order_id = o.order_id ")
           .append("WHERE o.customer_id = ? ");
        
        List<Object> params = new ArrayList<>();
        params.add(customerId);
        
        if (filter.getType() != null && !filter.getType().isEmpty()) {
            sql.append("AND t.transaction_type = ? ");
            params.add(filter.getType());
        }
        
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND t.transaction_status = ? ");
            params.add(filter.getStatus());
        }
        
        if (filter.getFromDate() != null) {
            sql.append("AND t.created_at >= ? ");
            params.add(Timestamp.valueOf(filter.getFromDate() + " 00:00:00"));
        }
        
        if (filter.getToDate() != null) {
            sql.append("AND t.created_at <= ? ");
            params.add(Timestamp.valueOf(filter.getToDate() + " 23:59:59"));
        }
        
        String validSortBy = validateSortColumn(filter.getSortByOrDefault());
        String validSortDir = filter.getSortDirOrDefault();
        sql.append(" ORDER BY ").append(validSortBy).append(" ").append(validSortDir);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        
        List<Transaction> transactions = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
            }
        }
        return transactions;
    }
    
    /**
     * Retrieves all transactions with filters (admin view).
     *
     * @param filter the filter criteria
     * @param offset pagination offset
     * @param limit pagination limit
     * @return list of transactions
     * @throws Exception if database operation fails
     */
    public List<Transaction> getAll(TransactionFilterRequest filter, int offset, int limit) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT transaction_id, order_id, transaction_type, transaction_method, transaction_status, ")
           .append("amount, transaction_reference, refund_reason, processed_by_type, processed_by, ")
           .append("processed_at, verified_by, verified_at, created_at FROM transactions WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (filter.getOrderId() != null) {
            sql.append("AND order_id = ? ");
            params.add(filter.getOrderId());
        }
        
        if (filter.getType() != null && !filter.getType().isEmpty()) {
            sql.append("AND transaction_type = ? ");
            params.add(filter.getType());
        }
        
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND transaction_status = ? ");
            params.add(filter.getStatus());
        }
        
        if (filter.getFromDate() != null) {
            sql.append("AND created_at >= ? ");
            params.add(Timestamp.valueOf(filter.getFromDate() + " 00:00:00"));
        }
        
        if (filter.getToDate() != null) {
            sql.append("AND created_at <= ? ");
            params.add(Timestamp.valueOf(filter.getToDate() + " 23:59:59"));
        }
        
        if (filter.getCustomerId() != null && !filter.getCustomerId().isEmpty()) {
            sql.append("AND order_id IN (SELECT order_id FROM orders WHERE customer_id = ?) ");
            params.add(filter.getCustomerId());
        }
        
        String validSortBy = validateSortColumn(filter.getSortByOrDefault());
        String validSortDir = filter.getSortDirOrDefault();
        sql.append(" ORDER BY ").append(validSortBy).append(" ").append(validSortDir);
        sql.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        
        List<Transaction> transactions = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapRow(rs));
                }
            }
        }
        return transactions;
    }
    
    /**
     * Counts transactions for a specific customer with filters.
     *
     * @param customerId the customer ID
     * @param filter the filter criteria
     * @return total count
     * @throws Exception if database operation fails
     */
    public int countByCustomerId(String customerId, TransactionFilterRequest filter) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM transactions t JOIN orders o ON t.order_id = o.order_id ")
           .append("WHERE o.customer_id = ? ");
        
        List<Object> params = new ArrayList<>();
        params.add(customerId);
        
        if (filter.getType() != null && !filter.getType().isEmpty()) {
            sql.append("AND t.transaction_type = ? ");
            params.add(filter.getType());
        }
        
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND t.transaction_status = ? ");
            params.add(filter.getStatus());
        }
        
        if (filter.getFromDate() != null) {
            sql.append("AND t.created_at >= ? ");
            params.add(Timestamp.valueOf(filter.getFromDate() + " 00:00:00"));
        }
        
        if (filter.getToDate() != null) {
            sql.append("AND t.created_at <= ? ");
            params.add(Timestamp.valueOf(filter.getToDate() + " 23:59:59"));
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
     * Counts all transactions with filters (admin view).
     *
     * @param filter the filter criteria
     * @return total count
     * @throws Exception if database operation fails
     */
    public int countAll(TransactionFilterRequest filter) throws Exception {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM transactions WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (filter.getOrderId() != null) {
            sql.append("AND order_id = ? ");
            params.add(filter.getOrderId());
        }
        
        if (filter.getType() != null && !filter.getType().isEmpty()) {
            sql.append("AND transaction_type = ? ");
            params.add(filter.getType());
        }
        
        if (filter.getStatus() != null && !filter.getStatus().isEmpty()) {
            sql.append("AND transaction_status = ? ");
            params.add(filter.getStatus());
        }
        
        if (filter.getFromDate() != null) {
            sql.append("AND created_at >= ? ");
            params.add(Timestamp.valueOf(filter.getFromDate() + " 00:00:00"));
        }
        
        if (filter.getToDate() != null) {
            sql.append("AND created_at <= ? ");
            params.add(Timestamp.valueOf(filter.getToDate() + " 23:59:59"));
        }
        
        if (filter.getCustomerId() != null && !filter.getCustomerId().isEmpty()) {
            sql.append("AND order_id IN (SELECT order_id FROM orders WHERE customer_id = ?) ");
            params.add(filter.getCustomerId());
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
     * Updates transaction status.
     *
     * @param transactionId the transaction ID
     * @param status the new status
     * @param verifiedBy the admin who verified (null for non-verification statuses)
     * @throws Exception if database operation fails
     */
    public void updateStatus(Long transactionId, String status, String verifiedBy) throws Exception {
        String sql;
        if (verifiedBy != null) {
            sql = "UPDATE transactions SET transaction_status = ?, verified_by = ?, verified_at = CURRENT_TIMESTAMP " +
                "WHERE transaction_id = ?";
            try (Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setString(2, verifiedBy);
                ps.setLong(3, transactionId);
                ps.executeUpdate();
            }
        } else {
            sql = "UPDATE transactions SET transaction_status = ?, processed_at = CURRENT_TIMESTAMP " +
                "WHERE transaction_id = ?";
            try (Connection conn = DBUtil.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setLong(2, transactionId);
                ps.executeUpdate();
            }
        }
    }
    
    /**
     * Updates refund reason for a transaction.
     *
     * @param transactionId the transaction ID
     * @param reason the refund reason
     * @throws Exception if database operation fails
     */
    public void updateRefundReason(Long transactionId, String reason) throws Exception {
        String sql = "UPDATE transactions SET refund_reason = ? WHERE transaction_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setLong(2, transactionId);
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
            case "amount":
            case "transaction_status":
            case "order_id":
                return sortBy;
            default:
                return "created_at";
        }
    }
    
    /**
     * Maps ResultSet row to Transaction object.
     *
     * @param rs the ResultSet
     * @return mapped Transaction object
     * @throws Exception if mapping fails
     */
    private Transaction mapRow(ResultSet rs) throws Exception {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(rs.getLong("transaction_id"));
        transaction.setOrderId(rs.getLong("order_id"));
        transaction.setTransactionType(rs.getString("transaction_type"));
        transaction.setTransactionMethod(rs.getString("transaction_method"));
        transaction.setTransactionStatus(rs.getString("transaction_status"));
        transaction.setAmount(rs.getBigDecimal("amount"));
        transaction.setTransactionReference(rs.getString("transaction_reference"));
        transaction.setRefundReason(rs.getString("refund_reason"));
        transaction.setProcessedByType(rs.getString("processed_by_type"));
        transaction.setProcessedBy(rs.getString("processed_by"));
        transaction.setProcessedAt(rs.getTimestamp("processed_at"));
        transaction.setVerifiedBy(rs.getString("verified_by"));
        transaction.setVerifiedAt(rs.getTimestamp("verified_at"));
        transaction.setCreatedAt(rs.getTimestamp("created_at"));
        return transaction;
    }
}
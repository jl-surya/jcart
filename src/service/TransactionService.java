package service;

import dao.OrderDAO;
import dao.TransactionDAO;
import dto.TransactionFilterRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Order;
import model.Transaction;

/**
 * TransactionService handles business logic for transaction operations.
 * 
 * Includes:
 * - Retrieve customer transactions with filters
 * - Retrieve single transaction for customer
 * - Retrieve all transactions for admin
 * - Process refund approval/rejection actions
 */
public class TransactionService {
    
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentGateway paymentGateway = new PaymentGateway();
    
    /**
     * Retrieves paginated transactions for a customer with filters.
     *
     * @param customerId the customer ID
     * @param filter the filter criteria
     * @return map containing transactions, page, size, total, totalPages
     * @throws Exception if database operation fails
     */
    public Map<String, Object> getCustomerTransactions(String customerId, TransactionFilterRequest filter) throws Exception {
        int page = filter.getPageOrDefault();
        int size = filter.getSizeOrDefault();
        int offset = (page - 1) * size;
        
        List<Transaction> transactions = transactionDAO.getByCustomerId(customerId, filter, offset, size);
        int total = transactionDAO.countByCustomerId(customerId, filter);
        
        Map<String, Object> result = new HashMap<>();
        result.put("transactions", transactions);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        
        return result;
    }
    
    /**
     * Retrieves a specific transaction for a customer with ownership verification.
     *
     * @param transactionId the transaction ID
     * @param customerId the customer ID for ownership verification
     * @return Transaction object
     * @throws Exception if transaction not found or not owned by customer
     */
    public Transaction getCustomerTransaction(Long transactionId, String customerId) throws Exception {
        Transaction transaction = transactionDAO.getById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        
        Order order = orderDAO.getById(transaction.getOrderId());
        if (order == null || !order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Transaction not found");
        }
        
        return transaction;
    }
    
    /**
     * Retrieves all transactions with filters (admin view).
     *
     * @param filter the filter criteria
     * @return map containing transactions, page, size, total, totalPages
     * @throws Exception if database operation fails
     */
    public Map<String, Object> getAllTransactions(TransactionFilterRequest filter) throws Exception {
        int page = filter.getPageOrDefault();
        int size = filter.getSizeOrDefault();
        int offset = (page - 1) * size;
        
        List<Transaction> transactions = transactionDAO.getAll(filter, offset, size);
        int total = transactionDAO.countAll(filter);
        
        Map<String, Object> result = new HashMap<>();
        result.put("transactions", transactions);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        
        return result;
    }
    
    /**
     * Retrieves a single transaction by ID (admin view).
     *
     * @param transactionId the transaction ID
     * @return Transaction object
     * @throws Exception if transaction not found
     */
    public Transaction getTransaction(Long transactionId) throws Exception {
        return transactionDAO.getById(transactionId);
    }
    
    /**
     * Processes refund approval or rejection action.
     *
     * @param transactionId the refund transaction ID
     * @param action the action (APPROVE or REJECT)
     * @param reason the rejection reason (required for REJECT)
     * @param adminId the admin performing the action
     * @throws Exception if action is invalid or transaction already processed
     */
    public void processRefundAction(Long transactionId, String action, String reason, String adminId) throws Exception {
        Transaction refundTransaction = transactionDAO.getById(transactionId);
        if (refundTransaction == null || !"REFUND".equals(refundTransaction.getTransactionType())) {
            throw new IllegalArgumentException("Invalid refund transaction");
        }
        
        if (!"PENDING".equals(refundTransaction.getTransactionStatus())) {
            throw new IllegalArgumentException("Refund already processed");
        }
        
        Order order = orderDAO.getById(refundTransaction.getOrderId());
        
        if ("APPROVE".equalsIgnoreCase(action)) {
            refundTransaction.setTransactionStatus("COMPLETED");
            transactionDAO.updateStatus(transactionId, "COMPLETED", adminId);
            
            orderDAO.updateStatus(order.getOrderId(), order.getOrderStatus(), "REFUNDED");
            
            Transaction paymentTransaction = transactionDAO.getByOrderIdAndType(order.getOrderId(), "PAYMENT");
            if (paymentTransaction != null && !"REFUNDED".equals(paymentTransaction.getTransactionStatus())) {
                transactionDAO.updateStatus(paymentTransaction.getTransactionId(), "REFUNDED", adminId);
            }
            
        } else if ("REJECT".equalsIgnoreCase(action)) {
            refundTransaction.setTransactionStatus("REJECTED");
            if (reason != null) {
                transactionDAO.updateRefundReason(transactionId, reason);
            }
            transactionDAO.updateStatus(transactionId, "REJECTED", adminId);
        } else {
            throw new IllegalArgumentException("Invalid action. Use APPROVE or REJECT");
        }
    }
}
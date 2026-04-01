package service;

import dao.OrderDAO;
import dao.TransactionDAO;
import dto.TransactionSearchRequest;
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
     * Retrieves a specific transaction for a customer with ownership verification.
     *
     * @param transactionId the transaction ID
     * @param customerId the customer ID for ownership verification
     * @return Transaction object
     * @throws Exception if transaction not found or not owned by customer
     */
    public Transaction getTransaction(Long transactionId, String customerId) throws Exception {
        Transaction transaction = transactionDAO.getById(transactionId);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found");
        }
        
        if(customerId != null && !transaction.getProcessedBy().equals(customerId)) {
            throw new IllegalArgumentException("Transaction not found for this customer");
        }
        
        return transaction;
    }
       
    /**
     * Searches transactions with filters and pagination.
     *
     * @param searchReq the search request with filters and pagination
     * @param isAdmin if true, includes individual stat fields (total, payments, refunds, pendingRefunds) in response
     * @return map containing transactions, page, size, total, totalPages, and optionally individual stat fields
     * @throws Exception if database operation fails
     */
    public Map<String, Object> searchTransactions(TransactionSearchRequest searchReq, boolean isAdmin) throws Exception {
        int page = searchReq.getPageOrDefault();
        int size = searchReq.getSizeOrDefault();
        int offset = (page - 1) * size;
        
        List<Transaction> transactions = transactionDAO.getAll(searchReq, offset, size);
        int total = transactionDAO.getAllCount(searchReq);
        
        Map<String, Object> result = new HashMap<>();
        result.put("transactions", transactions);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));

        if(isAdmin) {
            Map<String, Integer> stats = transactionDAO.getStats(searchReq);
            result.put("total", stats.getOrDefault("total", 0));
            result.put("payments", stats.getOrDefault("payments", 0));
            result.put("refunds", stats.getOrDefault("refunds", 0));
            result.put("pendingRefunds", stats.getOrDefault("pendingRefunds", 0));
        }
        
        return result;
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
            paymentGateway.processRefund(refundTransaction, adminId);
            transactionDAO.updateStatus(transactionId, "PAID", adminId);
            
            orderDAO.updateStatus(order.getOrderId(), order.getOrderStatus(), "REFUNDED");
            
            List<Transaction> payments = transactionDAO.getAllByOrderId(order.getOrderId());
            for (Transaction payment : payments) {
                if ("PAYMENT".equals(payment.getTransactionType()) && 
                    !"REFUNDED".equals(payment.getTransactionStatus())) {
                    transactionDAO.updateStatus(payment.getTransactionId(), "REFUNDED", adminId);
                    break;
                }
            }
            
        } else if ("REJECT".equalsIgnoreCase(action)) {
            paymentGateway.rejectRefund(refundTransaction, reason, adminId);
            if (reason != null) {
                transactionDAO.updateRefundReason(transactionId, reason);
            }
            transactionDAO.updateStatus(transactionId, "REJECTED", adminId);
            
            orderDAO.updateStatus(order.getOrderId(), order.getOrderStatus(), "REJECTED");
            
        } else {
            throw new IllegalArgumentException("Invalid action. Use APPROVE or REJECT");
        }
    }
}
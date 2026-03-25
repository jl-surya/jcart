package service;

import java.sql.Timestamp;
import model.Order;
import model.Transaction;

/**
 * PaymentGateway simulates external payment processing.
 * 
 * Includes:
 * - Payment initiation with transaction creation
 * - Payment processing with success/failure simulation
 * - Refund processing for cancelled orders
 */
public class PaymentGateway {
    
    /**
     * Initiates a payment transaction for an order.
     *
     * @param order the order to pay for
     * @param paymentMethod the payment method
     * @return initiated Transaction object
     */
    public Transaction initiatePayment(Order order, String paymentMethod) {
        Transaction transaction = new Transaction();
        transaction.setOrderId(order.getOrderId());
        transaction.setTransactionType("PAYMENT");
        transaction.setTransactionMethod(paymentMethod);
        transaction.setAmount(order.getTotalAmount());
        transaction.setTransactionStatus("INITIATED");
        transaction.setTransactionReference("TXN" + System.currentTimeMillis() + order.getOrderId());
        transaction.setProcessedByType("SYSTEM");
        
        return transaction;
    }
    
    /**
     * Processes a payment with success/failure simulation.
     *
     * @param transaction the transaction to process
     * @param success whether payment is successful
     * @return processed Transaction object with updated status
     */
    public Transaction processPayment(Transaction transaction, boolean success) {
        if (success) {
            transaction.setTransactionStatus("COMPLETED");
            transaction.setProcessedAt(new Timestamp(System.currentTimeMillis()));
        } else {
            transaction.setTransactionStatus("FAILED");
            transaction.setProcessedAt(new Timestamp(System.currentTimeMillis()));
        }
        return transaction;
    }
    
    /**
     * Processes a refund for a payment transaction.
     *
     * @param originalPayment the original payment transaction
     * @param reason reason for refund
     * @param adminId admin ID who approved the refund (null for system-initiated)
     * @return refund Transaction object
     */
    public Transaction processRefund(Transaction originalPayment, String reason, String adminId) {
        Transaction refund = new Transaction();
        refund.setOrderId(originalPayment.getOrderId());
        refund.setTransactionType("REFUND");
        refund.setTransactionMethod(originalPayment.getTransactionMethod());
        refund.setAmount(originalPayment.getAmount());
        refund.setTransactionStatus("COMPLETED");
        refund.setTransactionReference("REF" + System.currentTimeMillis() + originalPayment.getOrderId());
        refund.setRefundReason(reason);
        refund.setProcessedByType(adminId != null ? "ADMIN" : "SYSTEM");
        refund.setProcessedBy(adminId);
        refund.setProcessedAt(new Timestamp(System.currentTimeMillis()));
        
        if (adminId != null) {
            refund.setApprovedBy(adminId);
            refund.setApprovedAt(new Timestamp(System.currentTimeMillis()));
        }
        
        return refund;
    }
}
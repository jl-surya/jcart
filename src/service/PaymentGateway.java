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
     * @param customerId the customer ID who initiated payment
     * @return initiated Transaction object
     */
    public Transaction initiatePayment(Order order, String paymentMethod, String customerId) {
        Transaction transaction = new Transaction();
        transaction.setOrderId(order.getOrderId());
        transaction.setTransactionType("PAYMENT");
        transaction.setTransactionMethod(paymentMethod);
        transaction.setAmount(order.getTotalAmount());
        transaction.setTransactionStatus("PENDING");
        transaction.setTransactionReference("TXN" + System.currentTimeMillis() + order.getOrderId());
        transaction.setProcessedByType("CUSTOMER");
        transaction.setProcessedBy(customerId);
        
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
            transaction.setTransactionStatus("PAID");
            transaction.setProcessedAt(new Timestamp(System.currentTimeMillis()));
        } else {
            transaction.setTransactionStatus("FAILED");
            transaction.setProcessedAt(new Timestamp(System.currentTimeMillis()));
        }
        return transaction;
    }
    
    /**
     * Initiates a refund transaction for a payment.
     * Creates a pending refund that requires approval by an admin.
     * Called by superadmin (A0000001) when customer requests cancellation.
     *
     * @param originalPayment the original payment transaction
     * @param reason reason for refund request
     * @return initiated refund Transaction object with PENDING status
     */
    public Transaction initiateRefund(Transaction originalPayment, String reason) {
        Transaction refund = new Transaction();
        refund.setOrderId(originalPayment.getOrderId());
        refund.setTransactionType("REFUND");
        refund.setTransactionMethod(originalPayment.getTransactionMethod());
        refund.setAmount(originalPayment.getAmount());
        refund.setTransactionStatus("PENDING");
        refund.setTransactionReference("REF" + System.currentTimeMillis() + originalPayment.getOrderId());
        refund.setRefundReason(reason);
        refund.setProcessedByType("ADMIN");
        refund.setProcessedBy("A0000001");
        
        return refund;
    }
    
    /**
     * Processes (approves) a refund transaction.
     * Called by an admin with transaction permission to complete the refund.
     *
     * @param refundTransaction the pending refund transaction
     * @param adminId admin ID who processes/approves the refund
     * @return processed refund Transaction object with REFUNDED status
     */
    public Transaction processRefund(Transaction refundTransaction, String adminId) {
        refundTransaction.setTransactionStatus("REFUNDED");
        refundTransaction.setProcessedAt(new Timestamp(System.currentTimeMillis()));
        refundTransaction.setVerifiedBy(adminId);
        refundTransaction.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        
        return refundTransaction;
    }
    
    /**
     * Rejects a refund transaction.
     * Called by an admin with transaction permission to reject the refund request.
     *
     * @param refundTransaction the pending refund transaction
     * @param reason rejection reason
     * @param adminId admin ID who rejects the refund
     * @return rejected refund Transaction object with REJECTED status
     */
    public Transaction rejectRefund(Transaction refundTransaction, String reason, String adminId) {
        refundTransaction.setTransactionStatus("REJECTED");
        refundTransaction.setRefundReason(reason);
        refundTransaction.setProcessedAt(new Timestamp(System.currentTimeMillis()));
        refundTransaction.setVerifiedBy(adminId);
        refundTransaction.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        
        return refundTransaction;
    }
}
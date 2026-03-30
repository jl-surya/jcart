package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Transaction represents a financial transaction in the system.
 * 
 * Includes:
 * - Transaction identification (transactionId, transactionReference)
 * - Order association (orderId)
 * - Transaction details (type, method, status)
 * - Financial information (amount)
 * - Processing information (processedBy, processedAt, processedByType)
 * - Verification information (verifiedBy, verifiedAt)
 * - Refund reason (if applicable)
 * - Timestamp for creation
 */
public class Transaction implements Serializable {
    
    private Long transactionId;
    private Long orderId;
    private String transactionType;
    private String transactionMethod;
    private String transactionStatus;
    private BigDecimal amount;
    private String transactionReference;
    private String refundReason;
    private String processedByType;
    private String processedBy;
    private Timestamp processedAt;
    private String verifiedBy;
    private Timestamp verifiedAt;
    private Timestamp createdAt;

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getTransactionMethod() { return transactionMethod; }
    public void setTransactionMethod(String transactionMethod) { this.transactionMethod = transactionMethod; }

    public String getTransactionStatus() { return transactionStatus; }
    public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public String getProcessedByType() { return processedByType; }
    public void setProcessedByType(String processedByType) { this.processedByType = processedByType; }

    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

    public Timestamp getProcessedAt() { return processedAt; }
    public void setProcessedAt(Timestamp processedAt) { this.processedAt = processedAt; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public Timestamp getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Timestamp verifiedAt) { this.verifiedAt = verifiedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
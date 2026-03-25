package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Order represents a customer order in the system.
 * 
 * Includes:
 * - Order identification (orderId, invoiceNumber)
 * - Customer association (customerId)
 * - Order and payment status
 * - Financial information (totalAmount)
 * - Shipping address details
 * - Timestamps for tracking (created, updated, cancelled)
 * - Cancellation information (cancelledAt, cancelledBy)
 * - Payment deadline for pending orders
 */
public class Order implements Serializable {
    
    private Long orderId;
    private String customerId;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private String shippingName;
    private String shippingAddressLine;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;
    private String invoiceNumber;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp cancelledAt;
    private String cancelledBy;
    private Timestamp paymentDeadline;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getShippingName() { return shippingName; }
    public void setShippingName(String shippingName) { this.shippingName = shippingName; }

    public String getShippingAddressLine() { return shippingAddressLine; }
    public void setShippingAddressLine(String shippingAddressLine) { this.shippingAddressLine = shippingAddressLine; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

    public String getShippingState() { return shippingState; }
    public void setShippingState(String shippingState) { this.shippingState = shippingState; }

    public String getShippingPostalCode() { return shippingPostalCode; }
    public void setShippingPostalCode(String shippingPostalCode) { this.shippingPostalCode = shippingPostalCode; }

    public String getShippingCountry() { return shippingCountry; }
    public void setShippingCountry(String shippingCountry) { this.shippingCountry = shippingCountry; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Timestamp getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Timestamp cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public Timestamp getPaymentDeadline() { return paymentDeadline; }
    public void setPaymentDeadline(Timestamp paymentDeadline) { this.paymentDeadline = paymentDeadline; }
}
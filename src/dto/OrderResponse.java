package dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * OrderResponse represents the complete order details for API response.
 * 
 * Includes:
 * - Order identification (orderId, invoiceNumber, invoiceDate)
 * - Customer information
 * - Shipping address details
 * - Order items with product details
 * - Financial breakdown (subtotal, tax, shipping, discount, total)
 * - Payment information
 * - Order and payment status
 * - Payment deadline for pending orders
 */
public class OrderResponse {
    private Long orderId;
    private String invoiceNumber;
    private Timestamp invoiceDate;
    private Map<String, String> customer;
    private Map<String, Object> shippingAddress;
    private List<Map<String, Object>> items;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal discount;
    private BigDecimal total;
    private List<Map<String, Object>> payments;
    private String orderStatus;
    private String paymentStatus;
    private Timestamp paymentDeadline;
    
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public Timestamp getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(Timestamp invoiceDate) { this.invoiceDate = invoiceDate; }
    public Map<String, String> getCustomer() { return customer; }
    public void setCustomer(Map<String, String> customer) { this.customer = customer; }
    public Map<String, Object> getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(Map<String, Object> shippingAddress) { this.shippingAddress = shippingAddress; }
    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTax() { return tax; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public BigDecimal getShipping() { return shipping; }
    public void setShipping(BigDecimal shipping) { this.shipping = shipping; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public List<Map<String, Object>> getPayments() { return payments; }
    public void setPayments(List<Map<String, Object>> payments) { this.payments = payments; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public Timestamp getPaymentDeadline() { return paymentDeadline; }
    public void setPaymentDeadline(Timestamp paymentDeadline) { this.paymentDeadline = paymentDeadline; }
}
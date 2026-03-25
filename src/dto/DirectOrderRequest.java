package dto;

/**
 * DirectOrderRequest holds request details for creating a direct order.
 * 
 * Includes:
 * - Product details (product ID, quantity)
 * - Address selection (saved address or one-time address)
 * - Payment method selection
 */
public class DirectOrderRequest {
    private String productId;
    private Integer quantity;
    private String addressType;
    private Long addressId;
    private OrderRequest.OneTimeAddress oneTimeAddress;
    private String paymentMethod;
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getAddressType() { return addressType; }
    public void setAddressType(String addressType) { this.addressType = addressType; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public OrderRequest.OneTimeAddress getOneTimeAddress() { return oneTimeAddress; }
    public void setOneTimeAddress(OrderRequest.OneTimeAddress oneTimeAddress) { this.oneTimeAddress = oneTimeAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
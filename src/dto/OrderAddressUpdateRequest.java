package dto;

/**
 * OrderAddressUpdateRequest holds request details for updating order shipping address.
 * 
 * Includes:
 * - Address selection type (saved address or one-time address)
 * - Saved address ID (if using existing address)
 * - One-time address details (if using new address)
 */
public class OrderAddressUpdateRequest {
    private String addressType;
    private Long addressId;
    private OrderRequest.OneTimeAddress oneTimeAddress;
    
    public String getAddressType() { return addressType; }
    public void setAddressType(String addressType) { this.addressType = addressType; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public OrderRequest.OneTimeAddress getOneTimeAddress() { return oneTimeAddress; }
    public void setOneTimeAddress(OrderRequest.OneTimeAddress oneTimeAddress) { this.oneTimeAddress = oneTimeAddress; }
}
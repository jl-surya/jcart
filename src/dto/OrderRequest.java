package dto;

/**
 * OrderRequest holds request details for creating an order from cart.
 * 
 * Includes:
 * - Address selection (saved address or one-time address)
 * - Payment method selection
 * - Nested OneTimeAddress class for temporary addresses
 */
public class OrderRequest {
    private String addressType;
    private Long addressId;
    private OneTimeAddress oneTimeAddress;
    private String paymentMethod;
    
    public String getAddressType() { return addressType; }
    public void setAddressType(String addressType) { this.addressType = addressType; }
    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
    public OneTimeAddress getOneTimeAddress() { return oneTimeAddress; }
    public void setOneTimeAddress(OneTimeAddress oneTimeAddress) { this.oneTimeAddress = oneTimeAddress; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    /**
     * OneTimeAddress holds temporary address details for orders without saved address.
     * Used when customer chooses to enter a new address during checkout.
     */
    public static class OneTimeAddress {
        private String recipientName;
        private String addressLine;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phone;
        
        public String getRecipientName() { return recipientName; }
        public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
        public String getAddressLine() { return addressLine; }
        public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}
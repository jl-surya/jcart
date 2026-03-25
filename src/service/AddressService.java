package service;

import dao.AddressDAO;
import dto.AddressRequest;
import dto.OrderRequest;
import java.util.List;
import model.Address;

/**
 * AddressService handles business logic for customer address operations.
 * 
 * Includes:
 * - Address creation with limit validation
 * - Retrieve all addresses for a customer
 * - Retrieve single address by ID
 * - Retrieve default address
 * - Update address details
 * - Set default address
 * - Delete address
 * - Automatic default assignment for first address
 */
public class AddressService {
    
    private final AddressDAO addressDAO = new AddressDAO();
    private static final int MAX_ADDRESSES = 10;

    /**
     * Validates address request fields.
     *
     * @param request the address request to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateAddress(AddressRequest request) {
        if (request.getRecipientName() == null || request.getRecipientName().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient name is required");
        }
        if (request.getAddressLine() == null || request.getAddressLine().trim().isEmpty()) {
            throw new IllegalArgumentException("Address line is required");
        }
        if (request.getCity() == null || request.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        if (request.getPostalCode() == null || request.getPostalCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code is required");
        }
        if (request.getCountry() == null || request.getCountry().trim().isEmpty()) {
            throw new IllegalArgumentException("Country is required");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
    }
    
    /**
     * Validates one-time address fields for order checkout.
     *
     * @param oneTime the one-time address to validate
     * @throws IllegalArgumentException if validation fails
     */
    public void validateOneTimeAddress(OrderRequest.OneTimeAddress oneTime) {
        if (oneTime.getRecipientName() == null || oneTime.getRecipientName().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient name is required");
        }
        if (oneTime.getAddressLine() == null || oneTime.getAddressLine().trim().isEmpty()) {
            throw new IllegalArgumentException("Address line is required");
        }
        if (oneTime.getCity() == null || oneTime.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        if (oneTime.getPostalCode() == null || oneTime.getPostalCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code is required");
        }
        if (oneTime.getCountry() == null || oneTime.getCountry().trim().isEmpty()) {
            throw new IllegalArgumentException("Country is required");
        }
        if (oneTime.getPhone() == null || oneTime.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
    }
    
    /**
     * Creates a new address for a customer.
     * First address is automatically set as default.
     *
     * @param customerId the customer ID
     * @param request the address request with details
     * @return the created Address object
     * @throws Exception if address limit exceeded or database error occurs
     */
    public Address createAddress(String customerId, AddressRequest request) throws Exception {
        validateAddress(request);
        
        int currentCount = addressDAO.getCountByCustomer(customerId);
        if (currentCount >= MAX_ADDRESSES) {
            throw new IllegalArgumentException("Maximum " + MAX_ADDRESSES + " addresses allowed per customer");
        }

        boolean isDefault = request.getIsDefault() != null && request.getIsDefault();
        
        if (currentCount == 0) {
            isDefault = true;
        }
        
        if (isDefault && currentCount > 0) {
            Address existingDefault = addressDAO.getDefaultByCustomer(customerId);
            if (existingDefault != null) {
                addressDAO.clearDefault(customerId);
            }
        }
        
        Address address = new Address();
        address.setCustomerId(customerId);
        address.setRecipientName(request.getRecipientName());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        address.setPhone(request.getPhone());
        address.setDefault(isDefault);
        
        addressDAO.insert(address);
        
        return address;
    }
    
    /**
     * Retrieves all addresses for a customer.
     *
     * @param customerId the customer ID
     * @return list of addresses
     * @throws Exception if database operation fails
     */
    public List<Address> getAddresses(String customerId) throws Exception {
        return addressDAO.getAllByCustomer(customerId);
    }
    
    /**
     * Retrieves a specific address by ID.
     *
     * @param addressId the address ID
     * @param customerId the customer ID for ownership verification
     * @return the Address object
     * @throws Exception if address not found
     */
    public Address getAddress(Long addressId, String customerId) throws Exception {
        Address address = addressDAO.getById(addressId, customerId);
        if (address == null) {
            throw new IllegalArgumentException("Address not found");
        }
        return address;
    }
    
    /**
     * Retrieves the default address for a customer.
     *
     * @param customerId the customer ID
     * @return the default Address object
     * @throws Exception if no default address found
     */
    public Address getDefaultAddress(String customerId) throws Exception {
        Address address = addressDAO.getDefaultByCustomer(customerId);
        if (address == null) {
            throw new IllegalArgumentException("No default address found");
        }
        return address;
    }
    
    /**
     * Updates an existing address.
     *
     * @param addressId the address ID
     * @param customerId the customer ID for ownership verification
     * @param request the address request with updated details
     * @throws Exception if address not found or update fails
     */
    public void updateAddress(Long addressId, String customerId, AddressRequest request) throws Exception {
        Address address = getAddress(addressId, customerId);
        
        if (request.getRecipientName() != null) address.setRecipientName(request.getRecipientName());
        if (request.getAddressLine() != null) address.setAddressLine(request.getAddressLine());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());
        if (request.getCountry() != null) address.setCountry(request.getCountry());
        if (request.getPhone() != null) address.setPhone(request.getPhone());
        
        addressDAO.update(address);
        
        if (request.getIsDefault() != null && request.getIsDefault()) {
            setDefaultAddress(addressId, customerId);
        }
    }
    
    /**
     * Sets an address as the default for a customer.
     *
     * @param addressId the address ID
     * @param customerId the customer ID
     * @throws Exception if address not found or update fails
     */
    public void setDefaultAddress(Long addressId, String customerId) throws Exception {
        Address address = getAddress(addressId, customerId);
        addressDAO.setDefault(addressId, customerId);
    }
    
    /**
     * Deletes an address.
     * If the deleted address is default, assigns a new default if any addresses remain (recently added).
     *
     * @param addressId the address ID
     * @param customerId the customer ID for ownership verification
     * @throws Exception if address not found or deletion fails
     */
    public void deleteAddress(Long addressId, String customerId) throws Exception {
        Address address = getAddress(addressId, customerId);
        boolean wasDefault = address.isDefault();
        
        addressDAO.delete(addressId, customerId);
        
        if (wasDefault) {
            List<Address> remainingAddresses = addressDAO.getAllByCustomer(customerId);
            if (!remainingAddresses.isEmpty()) {
                Address newDefault = remainingAddresses.get(0);
                addressDAO.setDefault(newDefault.getAddressId(), customerId);
            }
        }
    }
}
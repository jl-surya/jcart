package dto;

/**
 * UpdateCartItemRequest holds request details for updating cart item quantity.
 * 
 * Includes:
 * - New quantity for the cart item
 */
public class UpdateCartItemRequest {
    private Integer quantity;
    
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
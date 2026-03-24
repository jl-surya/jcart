package dto;

/**
 * CartItemRequest holds request details for adding an item to cart.
 * 
 * Includes:
 * - Product ID to identify the item
 * - Quantity to add to cart
 */
public class CartItemRequest {
    private String productId;
    private Integer quantity;
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
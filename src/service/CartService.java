package service;

import dao.CartDAO;
import dao.ProductDAO;
import dto.CartItemRequest;
import java.util.List;
import model.CartItem;
import model.Product;

/**
 * CartService handles business logic for shopping cart operations.
 * 
 * Includes:
 * - Add items to cart with quantity validation
 * - Update cart item quantities
 * - Remove items from cart
 * - Clear entire cart
 * - Retrieve cart with product details
 * - Stock availability validation
 * - Active product validation
 */
public class CartService {
    
    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private static final int MAX_QUANTITY = 50;
    
    /**
     * Adds an item to cart or updates quantity if item already exists.
     *
     * @param customerId the customer ID
     * @param request the cart item request with product ID and quantity
     * @return the CartItem object (existing or newly created)
     * @throws Exception if quantity invalid, product not found, or product inactive
     */
    public CartItem addToCart(String customerId, CartItemRequest request) throws Exception {
        String productId = request.getProductId();
        int quantity = request.getQuantity();
        
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between 1 and " + MAX_QUANTITY);
        }
        
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }

        if (!product.isActive()) {
            throw new IllegalArgumentException("Product is not available");
        }
        
        CartItem existing = cartDAO.getByProduct(customerId, productId);
        
        if (existing != null) {
            int newQuantity = existing.getQuantity() + quantity;
            if (newQuantity > MAX_QUANTITY) {
                throw new IllegalArgumentException("Total quantity cannot exceed " + MAX_QUANTITY);
            }
            cartDAO.updateQuantity(customerId, productId, newQuantity);
            existing.setQuantity(newQuantity);
            return existing;
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCustomerId(customerId);
            cartItem.setProductId(productId);
            cartItem.setQuantity(quantity);
            cartItem.setProductName(product.getProductName());
            cartItem.setPrice(product.getPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItem.setStockLevel(product.getStockLevel());
            cartDAO.insert(cartItem);
            return cartItem;
        }
    }
    
    /**
     * Updates the quantity of an existing cart item.
     *
     * @param customerId the customer ID
     * @param productId the product ID
     * @param quantity the new quantity (must be between 1 and MAX_QUANTITY)
     * @throws Exception if quantity invalid or item not found in cart
     */
    public void updateCartItemQuantity(String customerId, String productId, int quantity) throws Exception {
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw new IllegalArgumentException("Quantity must be between 1 and " + MAX_QUANTITY);
        }
        
        CartItem existing = cartDAO.getByProduct(customerId, productId);
        if (existing == null) {
            throw new IllegalArgumentException("Item not found in cart");
        }
        
        cartDAO.updateQuantity(customerId, productId, quantity);
    }
    
    /**
     * Removes a specific item from cart.
     *
     * @param customerId the customer ID
     * @param productId the product ID
     * @throws Exception if item not found in cart
     */
    public void removeFromCart(String customerId, String productId) throws Exception {
        CartItem existing = cartDAO.getByProduct(customerId, productId);
        if (existing == null) {
            throw new IllegalArgumentException("Item not found in cart");
        }
        cartDAO.delete(customerId, productId);
    }
    
    /**
     * Clears all items from customer's cart.
     *
     * @param customerId the customer ID
     * @throws Exception if database operation fails
     */
    public void clearCart(String customerId) throws Exception {
        cartDAO.clear(customerId);
    }
    
    /**
     * Retrieves all items in customer's cart with product details.
     *
     * @param customerId the customer ID
     * @return list of cart items
     * @throws Exception if database operation fails
     */
    public List<CartItem> getCart(String customerId) throws Exception {
        return cartDAO.getAllByCustomer(customerId);
    }
}
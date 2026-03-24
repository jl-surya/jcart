package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * CartItem represents an item in a customer's shopping cart.
 * 
 * Includes:
 * - Product information (ID, name, price, discount)
 * - Quantity and stock availability
 * - Timestamps for tracking (added, updated, expiry)
 * - Utility method to calculate subtotal after discount
 */
public class CartItem implements Serializable {
    
    private String customerId;
    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal price;
    private BigDecimal discount;
    private int stockLevel;
    private Timestamp addedAt;
    private Timestamp updatedAt;
    private Timestamp expiresAt;

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public int getStockLevel() { return stockLevel; }
    public void setStockLevel(int stockLevel) { this.stockLevel = stockLevel; }

    public Timestamp getAddedAt() { return addedAt; }
    public void setAddedAt(Timestamp addedAt) { this.addedAt = addedAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
    
    /**
     * Calculates subtotal for this cart item after applying discount.
     *
     * @return subtotal (unit price after discount × quantity)
     */
    public BigDecimal getSubtotal() {
        if (price == null) return BigDecimal.ZERO;
        BigDecimal unitPrice = price;
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = price.multiply(discount).divide(BigDecimal.valueOf(100));
            unitPrice = price.subtract(discountAmount);
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
package model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Product represents a product entity in the catalog.
 * 
 * Includes:
 * - Basic product information (name, category)
 * - Pricing details (price, discount, tax rate)
 * - Inventory management (stock level)
 * - Shipping attributes (cost, method)
 * - Product attributes (age group, gender, location, seasonality)
 * - Active status for product visibility
 * - Utility method to calculate final price after discount
 */
public class Product implements Serializable {
    
    private String productId;
    private String productName;
    private String category;
    private BigDecimal price;
    private BigDecimal discount;
    private BigDecimal taxRate;
    private Integer stockLevel;
    private String ageGroup;
    private String location;
    private String gender;
    private BigDecimal shippingCost;
    private String shippingMethod;
    private String seasonality;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private BigDecimal finalPrice;

    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public Integer getStockLevel() { return stockLevel; }
    public void setStockLevel(Integer stockLevel) { this.stockLevel = stockLevel; }

    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public BigDecimal getShippingCost() { return shippingCost; }
    public void setShippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; }

    public String getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(String shippingMethod) { this.shippingMethod = shippingMethod; }

    public String getSeasonality() { return seasonality; }
    public void setSeasonality(String seasonality) { this.seasonality = seasonality; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    /**
     * Calculates final price after applying discount.
     *
     * @return final price after discount, or original price if no discount
     */
    public BigDecimal getFinalPrice() {
        if (finalPrice != null) return finalPrice;
        if (price == null) return BigDecimal.ZERO;
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = price.multiply(discount).divide(BigDecimal.valueOf(100));
            return price.subtract(discountAmount);
        }
        return price;
    }
    
    public void setFinalPrice(BigDecimal finalPrice) { this.finalPrice = finalPrice; }
    
    /**
     * Calculates and sets the final price field for serialization.
     * Call this before serializing the product to JSON.
     */
    public void calculateFinalPrice() {
        if (price == null) {
            this.finalPrice = BigDecimal.ZERO;
            return;
        }
        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountAmount = price.multiply(discount).divide(BigDecimal.valueOf(100));
            this.finalPrice = price.subtract(discountAmount);
        } else {
            this.finalPrice = price;
        }
    }
}
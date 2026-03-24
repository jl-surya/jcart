package dto;

import java.math.BigDecimal;

/**
 * ProductUpdateRequest holds product update details.
 * All fields are optional for partial updates.
 * 
 * Includes:
 * - Basic product information (name, category)
 * - Pricing details (price, discount, tax rate)
 * - Inventory management (stock level)
 * - Shipping attributes (cost, method)
 * - Product attributes (age group, gender, location, seasonality)
 */
public class ProductUpdateRequest {
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
}
package service;

import dao.ProductDAO;
import dto.ProductCreateRequest;
import dto.ProductSearchRequest;
import dto.ProductUpdateRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Product;

/**
 * ProductService handles business logic for product operations.
 * 
 * Includes:
 * - Product creation with validation
 * - Product retrieval by ID with active status filtering
 * - Product updates (partial updates supported)
 * - Product activation/deactivation
 * - Advanced search with filters and pagination
 * - Filter options retrieval for UI
 * - Stock management
 */
public class ProductService {
    
    private final ProductDAO productDAO = new ProductDAO();
    
    /**
     * Creates a new product.
     *
     * @param request the product creation request
     * @return the created Product object
     * @throws Exception if validation fails or database error occurs
     */
    public Product createProduct(ProductCreateRequest request) throws Exception {
        validateProductCreate(request);
        
        Product product = new Product();
        product.setProductName(request.getProductName());
        product.setCategory(request.getCategory());
        product.setPrice(request.getPrice());
        product.setDiscount(request.getDiscount());
        product.setTaxRate(request.getTaxRate());
        product.setStockLevel(request.getStockLevel());
        product.setAgeGroup(request.getAgeGroup());
        product.setLocation(request.getLocation());
        product.setGender(request.getGender());
        product.setShippingCost(request.getShippingCost());
        product.setShippingMethod(request.getShippingMethod());
        product.setSeasonality(request.getSeasonality());
        product.setActive(true);
        
        productDAO.insert(product);
        return product;
    }
    
    /**
     * Retrieves product by ID (includes inactive products).
     *
     * @param productId the product ID
     * @return Product object if found
     * @throws Exception if product not found
     */
    public Product getProduct(String productId) throws Exception {
        return getProduct(productId, false);
    }

    /**
     * Retrieves product by ID with option to require active status.
     *
     * @param productId the product ID
     * @param requireActive if true, only returns active products
     * @return Product object if found
     * @throws Exception if product not found or inactive when required
     */
    public Product getProduct(String productId, boolean requireActive) throws Exception {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Product ID is required");
        }
        
        Product product = productDAO.getById(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }
        
        if (requireActive && !product.isActive()) {
            throw new IllegalArgumentException("Product not found");
        }
        
        return product;
    }
    
    /**
     * Updates product information (partial updates supported).
     *
     * @param productId the product ID to update
     * @param request the update request with fields to modify
     * @return updated Product object
     * @throws Exception if product not found or update fails
     */
    public Product updateProduct(String productId, ProductUpdateRequest request) throws Exception {
        Product product = getProduct(productId);
        
        if (request.getProductName() != null) product.setProductName(request.getProductName());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDiscount() != null) product.setDiscount(request.getDiscount());
        if (request.getTaxRate() != null) product.setTaxRate(request.getTaxRate());
        if (request.getStockLevel() != null) product.setStockLevel(request.getStockLevel());
        if (request.getAgeGroup() != null) product.setAgeGroup(request.getAgeGroup());
        if (request.getLocation() != null) product.setLocation(request.getLocation());
        if (request.getGender() != null) product.setGender(request.getGender());
        if (request.getShippingCost() != null) product.setShippingCost(request.getShippingCost());
        if (request.getShippingMethod() != null) product.setShippingMethod(request.getShippingMethod());
        if (request.getSeasonality() != null) product.setSeasonality(request.getSeasonality());
        
        productDAO.update(product);
        return product;
    }
    
    /**
     * Deactivates a product (soft delete).
     *
     * @param productId the product ID to deactivate
     * @throws Exception if product not found or already inactive
     */
    public void deleteProduct(String productId) throws Exception {
        Product product = getProduct(productId);
        if (!product.isActive()) {
            throw new IllegalArgumentException("Product is already inactive");
        }
        productDAO.deactivate(productId);
    }

    /**
     * Activates a product.
     *
     * @param productId the product ID to activate
     * @throws Exception if product not found or already active
     */
    public void activateProduct(String productId) throws Exception {
        Product product = getProduct(productId);
        if (product.isActive()) {
            throw new IllegalArgumentException("Product is already active");
        }
        productDAO.activate(productId);
    }

    /**
     * Searches products with filters and pagination.
     *
     * @param request the search request with filters and pagination
     * @return map containing products, page, size, total, totalPages
     * @throws Exception if database operation fails
     */
    public Map<String, Object> searchProducts(ProductSearchRequest request) throws Exception {
        int page = request.getPageOrDefault();
        int size = request.getSizeOrDefault();
        int offset = (page - 1) * size;
        
        List<Product> products = productDAO.search(
            request.getKeyword(),
            request.getCategory(),
            request.getAgeGroup(),
            request.getGender(),
            request.getSeasonality(),
            request.getMinPrice(),
            request.getMaxPrice(),
            request.getInStock(),
            request.getShowInactive(),
            request.getSortByOrDefault(),
            request.getSortDirOrDefault(),
            offset,
            size
        );
        
        int total = productDAO.countSearch(
            request.getKeyword(),
            request.getCategory(),
            request.getAgeGroup(),
            request.getGender(),
            request.getSeasonality(),
            request.getMinPrice(),
            request.getMaxPrice(),
            request.getInStock(),
            request.getShowInactive()
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("products", products);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        
        return result;
    }    

    /**
     * Gets available filter options for product search UI.
     *
     * @param onlyActive if true, only returns options from active products
     * @return map of filter categories with their available values
     * @throws Exception if database operation fails
     */
    public Map<String, List<String>> getFilterOptions(boolean onlyActive) throws Exception {
        Map<String, List<String>> options = new HashMap<>();
        options.put("categories", productDAO.getCategories(onlyActive));
        options.put("ageGroups", productDAO.getAgeGroups(onlyActive));
        options.put("genders", productDAO.getGenders(onlyActive));
        options.put("seasonality", java.util.Arrays.asList("Yes", "No"));
        options.put("locations", productDAO.getLocations(onlyActive));
        return options;
    }
    
    /**
     * Updates product stock level.
     *
     * @param productId the product ID
     * @param quantity the new quantity (must be positive)
     * @throws Exception if product not found or quantity invalid
     */
    public void updateStock(String productId, int quantity) throws Exception {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        productDAO.updateStock(productId, quantity);
    }
    
    /**
     * Validates product creation request.
     *
     * @param request the product creation request
     * @throws IllegalArgumentException if validation fails
     */
    private void validateProductCreate(ProductCreateRequest request) {
        if (request.getProductName() == null || request.getProductName().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }
        if (request.getStockLevel() != null && request.getStockLevel() < 0) {
            throw new IllegalArgumentException("Stock level cannot be negative");
        }
        if (request.getDiscount() != null && (request.getDiscount().compareTo(BigDecimal.ZERO) < 0 || 
            request.getDiscount().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Discount must be between 0 and 100");
        }
    }
}
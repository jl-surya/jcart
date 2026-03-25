package dao;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Product;
import util.DBUtil;

/**
 * ProductDAO handles database operations for product entities.
 * 
 * Includes:
 * - CRUD operations for product management
 * - Advanced search with multiple filters
 * - Stock management with quantity validation
 * - Product activation/deactivation
 * - Filter options retrieval for UI
 */
public class ProductDAO {
    
    /**
     * Inserts a new product into database with auto-generated product_id.
     *
     * @param product the product object to insert
     * @throws Exception if database operation fails
     */
    public void insert(Product product) throws Exception {
        String sql = "INSERT INTO products (product_name, category, price, discount, tax_rate, stock_level, " +
                     "age_group, location, gender, shipping_cost, shipping_method, seasonality, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING product_id";
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, product.getProductName());
                ps.setString(2, product.getCategory());
                ps.setBigDecimal(3, product.getPrice());
                ps.setBigDecimal(4, product.getDiscount());
                ps.setBigDecimal(5, product.getTaxRate());
                ps.setInt(6, product.getStockLevel() != null ? product.getStockLevel() : 0);
                ps.setString(7, product.getAgeGroup());
                ps.setString(8, product.getLocation());
                ps.setString(9, product.getGender());
                ps.setBigDecimal(10, product.getShippingCost());
                ps.setString(11, product.getShippingMethod());
                ps.setString(12, product.getSeasonality());
                ps.setBoolean(13, product.isActive());
                
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        product.setProductId(rs.getString("product_id"));
                    }
                }
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    /**
     * Retrieves product by ID.
     *
     * @param productId the product ID
     * @return Product object if found, null otherwise
     * @throws Exception if database operation fails
     */
    public Product getById(String productId) throws Exception {
        String sql = "SELECT product_id, product_name, category, price, discount, tax_rate, stock_level, " +
                     "age_group, location, gender, shipping_cost, shipping_method, seasonality, is_active, " +
                     "created_at, updated_at FROM products WHERE product_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }
    
    /**
     * Updates product information.
     *
     * @param product the product object with updated details
     * @throws Exception if database operation fails
     */
    public void update(Product product) throws Exception {
        String sql = "UPDATE products SET product_name = ?, category = ?, price = ?, discount = ?, " +
                     "tax_rate = ?, stock_level = ?, age_group = ?, location = ?, gender = ?, " +
                     "shipping_cost = ?, shipping_method = ?, seasonality = ?, is_active = ?, " +
                     "updated_at = CURRENT_TIMESTAMP WHERE product_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.getProductName());
            ps.setString(2, product.getCategory());
            ps.setBigDecimal(3, product.getPrice());
            ps.setBigDecimal(4, product.getDiscount());
            ps.setBigDecimal(5, product.getTaxRate());
            ps.setInt(6, product.getStockLevel() != null ? product.getStockLevel() : 0);
            ps.setString(7, product.getAgeGroup());
            ps.setString(8, product.getLocation());
            ps.setString(9, product.getGender());
            ps.setBigDecimal(10, product.getShippingCost());
            ps.setString(11, product.getShippingMethod());
            ps.setString(12, product.getSeasonality());
            ps.setBoolean(13, product.isActive());
            ps.setString(14, product.getProductId());
            ps.executeUpdate();
        }
    }
    
    /**
     * Deactivates a product (soft delete).
     *
     * @param productId the product ID to deactivate
     * @throws Exception if database operation fails
     */
    public void deactivate(String productId) throws Exception {
        String sql = "UPDATE products SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE product_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            ps.executeUpdate();
        }
    }
    
    /**
     * Activates a product.
     *
     * @param productId the product ID to activate
     * @throws Exception if product not found or database operation fails
     */
    public void activate(String productId) throws Exception {
        String sql = "UPDATE products SET is_active = TRUE, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE product_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Product not found");
            }
        }
    }
    
    /**
     * Updates product stock level after purchase.
     * Ensures sufficient stock before reducing.
     *
     * @param productId the product ID
     * @param quantity the quantity to reduce
     * @throws Exception if insufficient stock or product inactive
     */
    public void updateStock(String productId, int quantity) throws Exception {
        String sql = "UPDATE products SET stock_level = stock_level - ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE product_id = ? AND stock_level >= ? AND is_active = TRUE";
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setString(2, productId);
            ps.setInt(3, quantity);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Insufficient stock or product is inactive");
            }
        }
    }
    
    /**
     * Searches products with multiple filters and pagination.
     *
     * @param keyword search keyword for product name or location
     * @param category product category filter
     * @param ageGroup age group filter
     * @param gender gender filter
     * @param seasonality seasonality filter
     * @param minPrice minimum price filter
     * @param maxPrice maximum price filter
     * @param inStock filter for in-stock products only
     * @param showInactive include inactive products
     * @param sortBy sort column
     * @param sortDir sort direction (ASC/DESC)
     * @param offset pagination offset
     * @param limit pagination limit
     * @return list of products matching criteria
     * @throws Exception if database operation fails
     */
    public List<Product> search(String keyword, String category, String ageGroup, String gender, 
                                 String seasonality, Double minPrice, Double maxPrice,
                                 Boolean inStock, Boolean showInactive, String sortBy, 
                                 String sortDir, int offset, int limit) throws Exception {
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT product_id, product_name, category, price, discount, tax_rate, stock_level, ")
           .append("age_group, location, gender, shipping_cost, shipping_method, seasonality, is_active, ")
           .append("created_at, updated_at FROM products WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (showInactive != null) {
            if (showInactive) {
                sql.append("AND is_active = FALSE ");
            } else {
                sql.append("AND is_active = TRUE ");
            }
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            String searchPattern = "%" + keyword + "%";
            sql.append("AND (product_name ILIKE ? OR location ILIKE ?) ");
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (category != null && !category.isEmpty()) {
            sql.append("AND category = ? ");
            params.add(category);
        }
        
        if (gender != null && !gender.isEmpty()) {
            sql.append("AND gender = ? ");
            params.add(gender);
        }
        
        if (ageGroup != null && !ageGroup.isEmpty()) {
            sql.append("AND age_group = ? ");
            params.add(ageGroup);
        }
        
        if (seasonality != null && !seasonality.isEmpty()) {
            sql.append("AND seasonality = ? ");
            params.add(seasonality);
        }
        
        if (minPrice != null) {
            sql.append("AND price >= ? ");
            params.add(BigDecimal.valueOf(minPrice));
        }
        
        if (maxPrice != null) {
            sql.append("AND price <= ? ");
            params.add(BigDecimal.valueOf(maxPrice));
        }
        
        if (inStock != null && inStock) {
            sql.append("AND stock_level > 0 ");
        }
        
        String primarySortColumn = validateSortColumn(sortBy);
        String primarySortDir = sortDir != null && sortDir.equalsIgnoreCase("asc") ? "ASC" : "DESC";
        
        sql.append(" ORDER BY ");
        
        if (sortBy != null && !sortBy.isEmpty()) {
            sql.append(primarySortColumn).append(" ").append(primarySortDir).append(", ");
        } else {
            sql.append("created_at DESC, ");
        }
        
        sql.append("product_id DESC ");
        sql.append(" LIMIT ? OFFSET ?");
        
        params.add(limit);
        params.add(offset);
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> products = new ArrayList<>();
                while (rs.next()) {
                    products.add(mapRow(rs));
                }
                return products;
            }
        }
    }
    
    /**
     * Counts products matching search criteria.
     *
     * @param keyword search keyword
     * @param category category filter
     * @param ageGroup age group filter
     * @param gender gender filter
     * @param seasonality seasonality filter
     * @param minPrice minimum price filter
     * @param maxPrice maximum price filter
     * @param inStock filter for in-stock products
     * @param showInactive include inactive products
     * @return total count of matching products
     * @throws Exception if database operation fails
     */
    public int countSearch(String keyword, String category, String ageGroup, String gender, 
                           String seasonality, Double minPrice, Double maxPrice,
                           Boolean inStock, Boolean showInactive) throws Exception {
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM products WHERE 1=1 ");
        
        List<Object> params = new ArrayList<>();
        
        if (showInactive != null) {
            if (showInactive) {
                sql.append("AND is_active = FALSE ");
            } else {
                sql.append("AND is_active = TRUE ");
            }
        }
        
        if (keyword != null && !keyword.isEmpty()) {
            String searchPattern = "%" + keyword + "%";
            sql.append("AND (product_name ILIKE ? OR location ILIKE ?) ");
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (category != null && !category.isEmpty()) {
            sql.append("AND category = ? ");
            params.add(category);
        }
        
        if (gender != null && !gender.isEmpty()) {
            sql.append("AND gender = ? ");
            params.add(gender);
        }
        
        if (ageGroup != null && !ageGroup.isEmpty()) {
            sql.append("AND age_group = ? ");
            params.add(ageGroup);
        }
        
        if (seasonality != null && !seasonality.isEmpty()) {
            sql.append("AND seasonality = ? ");
            params.add(seasonality);
        }
        
        if (minPrice != null) {
            sql.append("AND price >= ? ");
            params.add(BigDecimal.valueOf(minPrice));
        }
        
        if (maxPrice != null) {
            sql.append("AND price <= ? ");
            params.add(BigDecimal.valueOf(maxPrice));
        }
        
        if (inStock != null && inStock) {
            sql.append("AND stock_level > 0 ");
        }
        
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
    
    /**
     * Gets distinct categories from products.
     *
     * @param onlyActive if true, only includes active products
     * @return list of categories
     * @throws Exception if database operation fails
     */
    public List<String> getCategories(boolean onlyActive) throws Exception {
        String sql;
        if (onlyActive) {
            sql = "SELECT DISTINCT category FROM products WHERE is_active = TRUE AND category IS NOT NULL ORDER BY category";
        } else {
            sql = "SELECT DISTINCT category FROM products WHERE category IS NOT NULL ORDER BY category";
        }
        List<String> categories = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        }
        return categories;
    }

    /**
     * Gets distinct age groups from products.
     *
     * @param onlyActive if true, only includes active products
     * @return list of age groups
     * @throws Exception if database operation fails
     */
    public List<String> getAgeGroups(boolean onlyActive) throws Exception {
        String sql;
        if (onlyActive) {
            sql = "SELECT DISTINCT age_group FROM products WHERE is_active = TRUE AND age_group IS NOT NULL ORDER BY age_group";
        } else {
            sql = "SELECT DISTINCT age_group FROM products WHERE age_group IS NOT NULL ORDER BY age_group";
        }
        List<String> ageGroups = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ageGroups.add(rs.getString("age_group"));
            }
        }
        return ageGroups;
    }

    /**
     * Gets distinct genders from products.
     *
     * @param onlyActive if true, only includes active products
     * @return list of genders
     * @throws Exception if database operation fails
     */
    public List<String> getGenders(boolean onlyActive) throws Exception {
        String sql;
        if (onlyActive) {
            sql = "SELECT DISTINCT gender FROM products WHERE is_active = TRUE AND gender IS NOT NULL ORDER BY gender";
        } else {
            sql = "SELECT DISTINCT gender FROM products WHERE gender IS NOT NULL ORDER BY gender";
        }
        List<String> genders = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                genders.add(rs.getString("gender"));
            }
        }
        return genders;
    }

    /**
     * Gets distinct locations from products.
     *
     * @param onlyActive if true, only includes active products
     * @return list of locations
     * @throws Exception if database operation fails
     */
    public List<String> getLocations(boolean onlyActive) throws Exception {
        String sql;
        if (onlyActive) {
            sql = "SELECT DISTINCT location FROM products WHERE is_active = TRUE AND location IS NOT NULL ORDER BY location";
        } else {
            sql = "SELECT DISTINCT location FROM products WHERE location IS NOT NULL ORDER BY location";
        }
        List<String> locations = new ArrayList<>();
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                locations.add(rs.getString("location"));
            }
        }
        return locations;
    }
    
    /**
     * Validates sort column to prevent SQL injection.
     *
     * @param sortBy the sort column
     * @return validated sort column name
     */
    private String validateSortColumn(String sortBy) {
        if (sortBy == null) return "created_at";
        
        switch (sortBy) {
            case "price":
            case "product_name":
            case "category":
            case "stock_level":
            case "created_at":
            case "discount":
            case "gender":
            case "age_group":
            case "seasonality":
            case "location":
                return sortBy;
            default:
                return "created_at";
        }
    }
    
    /**
     * Maps ResultSet row to Product object.
     *
     * @param rs the ResultSet
     * @return mapped Product object
     * @throws Exception if mapping fails
     */
    private Product mapRow(ResultSet rs) throws Exception {
        Product product = new Product();
        product.setProductId(rs.getString("product_id"));
        product.setProductName(rs.getString("product_name"));
        product.setCategory(rs.getString("category"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setDiscount(rs.getBigDecimal("discount"));
        product.setTaxRate(rs.getBigDecimal("tax_rate"));
        product.setStockLevel(rs.getInt("stock_level"));
        product.setAgeGroup(rs.getString("age_group"));
        product.setLocation(rs.getString("location"));
        product.setGender(rs.getString("gender"));
        product.setShippingCost(rs.getBigDecimal("shipping_cost"));
        product.setShippingMethod(rs.getString("shipping_method"));
        product.setSeasonality(rs.getString("seasonality"));
        product.setActive(rs.getBoolean("is_active"));
        product.setCreatedAt(rs.getTimestamp("created_at"));
        product.setUpdatedAt(rs.getTimestamp("updated_at"));
        return product;
    }
    
    /**
     * Deducts stock for a product after order placement.
     *
     * @param productId the product ID
     * @param quantity the quantity to deduct
     * @throws Exception if insufficient stock or product inactive
     */
    public void deductStock(String productId, int quantity) throws Exception {
        String sql = "UPDATE products SET stock_level = stock_level - ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE product_id = ? AND stock_level >= ? AND is_active = TRUE";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setString(2, productId);
            ps.setInt(3, quantity);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new IllegalArgumentException("Insufficient stock or product inactive");
            }
        }
    }

    /**
     * Restores stock for a product when order is cancelled.
     *
     * @param productId the product ID
     * @param quantity the quantity to restore
     * @throws Exception if database operation fails
     */
    public void restoreStock(String productId, int quantity) throws Exception {
        String sql = "UPDATE products SET stock_level = stock_level + ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE product_id = ?";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setString(2, productId);
            ps.executeUpdate();
        }
    }

    /**
     * Restores stock for all items in a cancelled order.
     *
     * @param orderId the order ID
     * @throws Exception if database operation fails
     */
    public void restoreStockForOrder(Long orderId) throws Exception {
        String sql = "UPDATE products p SET stock_level = stock_level + oi.quantity " +
                    "FROM order_items oi WHERE oi.order_id = ? AND p.product_id = oi.product_id";
        
        try (Connection conn = DBUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.executeUpdate();
        }
    }
}
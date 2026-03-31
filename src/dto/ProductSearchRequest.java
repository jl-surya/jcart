package dto;

/**
 * ProductSearchRequest holds search and filter criteria for product listing.
 * 
 * Includes:
 * - Search filters (keyword, category, age group, gender, seasonality)
 * - Price range filters (minPrice, maxPrice)
 * - Stock and status filters (inStock, showInactive)
 * - Sorting options (sortBy, sortDir)
 * - Pagination parameters (page, size)
 * - Utility methods for default values
 */
public class ProductSearchRequest {
    private String keyword;
    private String category;
    private String ageGroup;
    private String gender;
    private String seasonality;
    private Double minPrice;
    private Double maxPrice;
    private Boolean inStock;
    private Boolean lowStock;
    private Boolean showInactive;
    private String sortBy;
    private String sortDir;
    private Integer page;
    private Integer size;
    
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getSeasonality() { return seasonality; }
    public void setSeasonality(String seasonality) { this.seasonality = seasonality; }
    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }
    public Boolean getLowStock() { return lowStock; }
    public void setLowStock(Boolean lowStock) { this.lowStock = lowStock; }
    public Boolean getShowInactive() { return showInactive; }
    public void setShowInactive(Boolean showInactive) { this.showInactive = showInactive; }
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getSortDir() { return sortDir; }
    public void setSortDir(String sortDir) { this.sortDir = sortDir; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    
    /**
     * Gets page number with default value 1 if not set.
     *
     * @return page number (minimum 1)
     */
    public int getPageOrDefault() {
        return page != null && page > 0 ? page : 1;
    }
    
    /**
     * Gets page size with default value 20 if not set.
     * Maximum page size is 100.
     *
     * @return page size (between 1 and 100)
     */
    public int getSizeOrDefault() {
        return size != null && size > 0 && size <= 100 ? size : 20;
    }
    
    /**
     * Gets sort field with default value "created_at" if not set.
     *
     * @return sort field name
     */
    public String getSortByOrDefault() {
        return sortBy != null && !sortBy.isEmpty() ? sortBy : "created_at";
    }
    
    /**
     * Gets sort direction with default value "DESC" if not set.
     *
     * @return sort direction (ASC or DESC)
     */
    public String getSortDirOrDefault() {
        return sortDir != null && sortDir.equalsIgnoreCase("asc") ? "ASC" : "DESC";
    }
}
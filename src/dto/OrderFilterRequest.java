package dto;

/**
 * OrderFilterRequest holds search and filter criteria for order listing.
 * 
 * Includes:
 * - Customer filter (customerId)
 * - Status filter (order status)
 * - Date range filters (fromDate, toDate)
 * - Amount range filters (minAmount, maxAmount)
 * - Sorting options (sortBy, sortDir)
 * - Pagination parameters (page, size)
 * - Utility methods for default values
 */
public class OrderFilterRequest {
    private String customerId;
    private String status;
    private String fromDate;
    private String toDate;
    private Double minAmount;
    private Double maxAmount;
    private String sortBy;
    private String sortDir;
    private Integer page;
    private Integer size;
    
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public Double getMinAmount() { return minAmount; }
    public void setMinAmount(Double minAmount) { this.minAmount = minAmount; }
    public Double getMaxAmount() { return maxAmount; }
    public void setMaxAmount(Double maxAmount) { this.maxAmount = maxAmount; }
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
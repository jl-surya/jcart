package dto;

/**
 * TransactionSearchRequest holds search and filter criteria for transaction listing.
 * 
 * Includes:
 * - Customer filter (customerId)
 * - Order filter (orderId)
 * - Type filter (PAYMENT, REFUND)
 * - Status filter (transaction status)
 * - Payment method filter
 * - Date range filters (fromDate, toDate)
 * - Sorting options (sortBy, sortDir)
 * - Pagination parameters (page, size)
 * - Utility methods for default values
 */
public class TransactionSearchRequest {
    private String keyword;
    private String customerId;
    private Long orderId;
    private String type;
    private String status;
    private String paymentMethod;
    private String fromDate;
    private String toDate;
    private String sortBy;
    private String sortDir;
    private Integer page;
    private Integer size;
    
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
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
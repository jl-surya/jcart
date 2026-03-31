package dto;

/**
 * CustomerSearchRequest holds search and filter criteria for customer listing.
 * 
 * Includes:
 * - Search filters (search keyword, status)
 * - Sorting options (sortBy, sortDir)
 * - Pagination parameters (page, size)
 */
public class CustomerSearchRequest {
    private String search;
    private String status;
    private String sortBy;
    private String sortDir;
    private Integer page;
    private Integer size;
    
    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
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
     * Gets page size with default value 10 if not set.
     * Maximum page size is 100.
     *
     * @return page size (between 1 and 100)
     */
    public int getSizeOrDefault() {
        return size != null && size > 0 && size <= 100 ? size : 10;
    }
}

package controller;

import config.AsyncExecutor;
import dto.ProductSearchRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Product;
import service.ProductService;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * ProductController handles public product endpoints for customers.
 * 
 * Includes:
 * - Product search with filters
 * - View single product details
 * - Filter options retrieval
 * - Only active products are visible to customers
 */
public class ProductController extends BaseController {
    
    private final ProductService productService = new ProductService();

    /**
     * Handles GET requests for product endpoints.
     * Supports retrieving a single product by ID.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            sendError(resp, "Use POST /products/search for product search", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else if (pathInfo.matches("/\\w+")) {
            String productId = pathInfo.substring(1);

            if (!isValidProductId(productId)) {
                sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            handleGetProduct(req, resp, productId);
        } else if ("/filter-options".equals(pathInfo)) {
            handleFilterOptions(req, resp);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for product endpoints.
     * Supports product search operations.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        
        if ("/search".equals(pathInfo)) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            handleSearch(req, resp, sb.toString());
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    /**
     * Handles GET /products/{id} - retrieves a single product (active only).
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param productId the product ID
     */
    private void handleGetProduct(HttpServletRequest req, HttpServletResponse resp, String productId) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    Product product = productService.getProduct(productId, true);
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("productId", product.getProductId());
                    data.put("productName", product.getProductName());
                    data.put("category", product.getCategory());
                    data.put("price", product.getPrice());
                    data.put("finalPrice", product.getFinalPrice());
                    data.put("discount", product.getDiscount());
                    data.put("taxRate", product.getTaxRate());
                    data.put("stockLevel", product.getStockLevel());
                    data.put("inStock", product.getStockLevel() != null && product.getStockLevel() > 0);
                    data.put("ageGroup", product.getAgeGroup());
                    data.put("location", product.getLocation());
                    data.put("gender", product.getGender());
                    data.put("shippingCost", product.getShippingCost());
                    data.put("shippingMethod", product.getShippingMethod());
                    data.put("seasonality", product.getSeasonality());
                    
                    sendSuccess(response, data);
                    
                } catch (Exception e) {
                    try {
                        sendError((HttpServletResponse) asyncContext.getResponse(), e.getMessage(), 
                                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } catch (IOException ignored) {}
                } finally {
                    asyncContext.complete();
                }
            });
        } catch (RejectedExecutionException ex) {
            try {
                sendError(resp, "Server overloaded", HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Handles POST /products/search - searches products with filters.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleSearch(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    ProductSearchRequest searchReq = parseSearchRequestFromJson(jsonBody);
                    
                    searchReq.setShowInactive(false);
                    
                    Map<String, Object> result = productService.searchProducts(searchReq);
                    
                    sendSuccess(response, result);
                    
                } catch (Exception e) {
                    try {
                        sendError((HttpServletResponse) asyncContext.getResponse(), e.getMessage(), 
                                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } catch (IOException ignored) {}
                } finally {
                    asyncContext.complete();
                }
            });
        } catch (RejectedExecutionException ex) {
            try {
                sendError(resp, "Server overloaded", HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Handles GET /products/filter-options - retrieves available filter values.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    private void handleFilterOptions(HttpServletRequest req, HttpServletResponse resp) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    Map<String, List<String>> options = productService.getFilterOptions(true);
                    sendSuccess(response, options);
                    
                } catch (Exception e) {
                    try {
                        sendError((HttpServletResponse) asyncContext.getResponse(), e.getMessage(), 
                                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } catch (IOException ignored) {}
                } finally {
                    asyncContext.complete();
                }
            });
        } catch (RejectedExecutionException ex) {
            try {
                sendError(resp, "Server overloaded", HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Validates product ID format.
     * Product IDs should follow pattern: P followed by 7 digits (e.g., P0000001)
     *
     * @param productId the product ID to validate
     * @return true if product ID format is valid, false otherwise
     */
    private boolean isValidProductId(String productId) {
        return productId != null && productId.matches("^P\\d{7}$");
    }
    
    /**
     * Parses JSON into ProductSearchRequest object.
     *
     * @param json the JSON string
     * @return populated ProductSearchRequest object
     */
    private ProductSearchRequest parseSearchRequestFromJson(String json) {
        ProductSearchRequest searchReq = new ProductSearchRequest();
        searchReq.setKeyword(JsonUtil.getString(json, "keyword"));
        searchReq.setCategory(JsonUtil.getString(json, "category"));
        searchReq.setAgeGroup(JsonUtil.getString(json, "ageGroup"));
        searchReq.setGender(JsonUtil.getString(json, "gender"));
        searchReq.setSeasonality(JsonUtil.getString(json, "seasonality"));
        
        String minPrice = JsonUtil.getString(json, "minPrice");
        if (minPrice != null) {
            try {
                searchReq.setMinPrice(Double.parseDouble(minPrice));
            } catch (NumberFormatException ignored) {}
        }
        
        String maxPrice = JsonUtil.getString(json, "maxPrice");
        if (maxPrice != null) {
            try {
                searchReq.setMaxPrice(Double.parseDouble(maxPrice));
            } catch (NumberFormatException ignored) {}
        }
        
        String inStock = JsonUtil.getString(json, "inStock");
        if (inStock != null) {
            searchReq.setInStock(Boolean.parseBoolean(inStock));
        }
        
        searchReq.setSortBy(JsonUtil.getString(json, "sortBy"));
        searchReq.setSortDir(JsonUtil.getString(json, "sortDir"));
        
        String page = JsonUtil.getString(json, "page");
        if (page != null) {
            try {
                searchReq.setPage(Integer.parseInt(page));
            } catch (NumberFormatException ignored) {}
        }
        
        String size = JsonUtil.getString(json, "size");
        if (size != null) {
            try {
                searchReq.setSize(Integer.parseInt(size));
            } catch (NumberFormatException ignored) {}
        }
        
        return searchReq;
    }
}
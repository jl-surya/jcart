package controller;

import config.AsyncExecutor;
import dto.ProductCreateRequest;
import dto.ProductSearchRequest;
import dto.ProductUpdateRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Product;
import service.ProductService;
import service.AdminService;
import model.Admin;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * ProductManagementController handles admin product management operations.
 * 
 * Includes:
 * - Product CRUD operations (create, read, update, delete)
 * - Product search with filters and pagination
 * - Product activation/deactivation
 * - Admin-only access with authentication
 */
@WebServlet(value = "/admin/products/*", asyncSupported = true)
public class ProductManagementController extends BaseController {
    
    private final ProductService productService = new ProductService();
    private final AdminService adminService = new AdminService();

    /**
     * Handles GET requests for admin product endpoints.
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
            sendError(resp, "Use POST /admin/products/search for product search", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else if (pathInfo.matches("/\\w+")) {
            String productId = pathInfo.substring(1);

            if (!isValidProductId(productId)) {
                sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            handleGetProduct(req, resp, productId);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for admin product endpoints.
     * Supports create, update, delete, search, and activate operations.
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
        
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String jsonBody = sb.toString();
        
        String method = JsonUtil.getString(jsonBody, "_method");
        
        if (pathInfo == null || pathInfo.equals("/")) {
            if (method != null && method.equalsIgnoreCase("PATCH")) {
                sendError(resp, "Product ID required for update", HttpServletResponse.SC_BAD_REQUEST);
            } else {
                handleCreateProduct(req, resp, jsonBody);
            }
            return;
        }
        
        if ("/search".equals(pathInfo)) {
            handleSearch(req, resp, jsonBody);
            return;
        }
        
        if (pathInfo.matches("/\\w+")) {
            String productId = pathInfo.substring(1);
            
            if ("PATCH".equalsIgnoreCase(method)) {
                handleUpdateProduct(req, resp, productId, jsonBody);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDeleteProduct(req, resp, productId, jsonBody);
            } else if ("ACTIVATE".equalsIgnoreCase(method)) {
                handleActivateProduct(req, resp, productId, jsonBody);
            } else {
                sendError(resp, "Method not allowed", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Handles POST /admin/products/ - creates a new product.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleCreateProduct(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_PRODUCT_CREATE)) {
                        sendError(response, "Permission denied. Requires 'products:create'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    ProductCreateRequest createReq = parseCreateRequest(jsonBody);
                    Product product = productService.createProduct(createReq);
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("productId", product.getProductId());
                    data.put("productName", product.getProductName());
                    
                    sendSuccess(response, "Product created successfully", data);
                    
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
     * Handles GET /admin/products/{id} - retrieves a single product (includes inactive).
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
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_PRODUCT_VIEW)) {
                        sendError(response, "Permission denied. Requires 'products:view'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    Product product = productService.getProduct(productId, false);
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("productId", product.getProductId());
                    data.put("productName", product.getProductName());
                    data.put("category", product.getCategory());
                    data.put("price", product.getPrice());
                    data.put("finalPrice", product.getFinalPrice());
                    data.put("discount", product.getDiscount());
                    data.put("taxRate", product.getTaxRate());
                    data.put("stockLevel", product.getStockLevel());
                    data.put("ageGroup", product.getAgeGroup());
                    data.put("location", product.getLocation());
                    data.put("gender", product.getGender());
                    data.put("shippingCost", product.getShippingCost());
                    data.put("shippingMethod", product.getShippingMethod());
                    data.put("seasonality", product.getSeasonality());
                    data.put("isActive", product.isActive()); 
                    data.put("createdAt", product.getCreatedAt());
                    data.put("updatedAt", product.getUpdatedAt());
                    
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
     * Handles POST /admin/products/search - searches products with filters.
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
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_PRODUCT_VIEW)) {
                        sendError(response, "Permission denied. Requires 'products:view'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    ProductSearchRequest searchReq = parseSearchRequest(jsonBody);
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
     * Handles PATCH /admin/products/{id} - updates a product.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param productId the product ID to update
     * @param jsonBody the request body as JSON string
     */
    private void handleUpdateProduct(HttpServletRequest req, HttpServletResponse resp, 
                                      String productId, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_PRODUCT_UPDATE)) {
                        sendError(response, "Permission denied. Requires 'products:update'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    ProductUpdateRequest updateReq = parseUpdateRequest(jsonBody);
                    productService.updateProduct(productId, updateReq);
                    
                    sendSuccess(response, "Product updated successfully", null);
                    
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
     * Handles ACTIVATE /admin/products/{id} - activates a product.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param productId the product ID to activate
     * @param jsonBody the request body with confirmation
     */
    private void handleActivateProduct(HttpServletRequest req, HttpServletResponse resp, 
                                        String productId, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_PRODUCT_UPDATE)) {
                        sendError(response, "Permission denied. Requires 'products:update'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    String confirm = JsonUtil.getString(jsonBody, "confirm");
                    if (!"ACTIVATE".equals(confirm)) {
                        throw new IllegalArgumentException("Type 'ACTIVATE' to confirm activation");
                    }
                    
                    productService.activateProduct(productId);
                    
                    sendSuccess(response, "Product activated successfully", null);
                    
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
     * Handles DELETE /admin/products/{id} - deactivates a product.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param productId the product ID to deactivate
     * @param jsonBody the request body with confirmation
     */
    private void handleDeleteProduct(HttpServletRequest req, HttpServletResponse resp, 
                                      String productId, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_PRODUCT_DELETE)) {
                        sendError(response, "Permission denied. Requires 'products:delete'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    String confirm = JsonUtil.getString(jsonBody, "confirm");
                    if (!"DELETE".equals(confirm)) {
                        throw new IllegalArgumentException("Type 'DELETE' to confirm deletion");
                    }
                    
                    productService.deleteProduct(productId);
                    
                    sendSuccess(response, "Product deleted successfully", null);
                    
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
     * Parses JSON into ProductCreateRequest object.
     *
     * @param json the JSON string
     * @return populated ProductCreateRequest object
     */
    private ProductCreateRequest parseCreateRequest(String json) {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setProductName(JsonUtil.getString(json, "productName"));
        req.setCategory(JsonUtil.getString(json, "category"));
        
        String price = JsonUtil.getString(json, "price");
        if (price != null) {
            req.setPrice(new BigDecimal(price));
        }
        
        String discount = JsonUtil.getString(json, "discount");
        if (discount != null) {
            req.setDiscount(new BigDecimal(discount));
        }
        
        String taxRate = JsonUtil.getString(json, "taxRate");
        if (taxRate != null) {
            req.setTaxRate(new BigDecimal(taxRate));
        }
        
        String stockLevel = JsonUtil.getString(json, "stockLevel");
        if (stockLevel != null) {
            req.setStockLevel(Integer.parseInt(stockLevel));
        }
        
        req.setAgeGroup(JsonUtil.getString(json, "ageGroup"));
        req.setLocation(JsonUtil.getString(json, "location"));
        req.setGender(JsonUtil.getString(json, "gender"));
        
        String shippingCost = JsonUtil.getString(json, "shippingCost");
        if (shippingCost != null) {
            req.setShippingCost(new BigDecimal(shippingCost));
        }
        
        req.setShippingMethod(JsonUtil.getString(json, "shippingMethod"));
        req.setSeasonality(JsonUtil.getString(json, "seasonality"));
        
        return req;
    }
    
    /**
     * Parses JSON into ProductSearchRequest object for admin search.
     *
     * @param json the JSON string
     * @return populated ProductSearchRequest object
     */
    private ProductSearchRequest parseSearchRequest(String json) {
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

        String lowStock = JsonUtil.getString(json, "lowStock");
        if (lowStock != null) {
            searchReq.setLowStock(Boolean.parseBoolean(lowStock));
        }

        String showInactive = JsonUtil.getString(json, "showInactive");
        if (showInactive != null) {
            searchReq.setShowInactive(Boolean.parseBoolean(showInactive));
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
    /**
     * Parses JSON into ProductUpdateRequest object.
     *
     * @param json the JSON string
     * @return populated ProductUpdateRequest object
     */
    private ProductUpdateRequest parseUpdateRequest(String json) {
        ProductUpdateRequest req = new ProductUpdateRequest();
        req.setProductName(JsonUtil.getString(json, "productName"));
        req.setCategory(JsonUtil.getString(json, "category"));
        
        String price = JsonUtil.getString(json, "price");
        if (price != null) {
            req.setPrice(new BigDecimal(price));
        }
        
        String discount = JsonUtil.getString(json, "discount");
        if (discount != null) {
            req.setDiscount(new BigDecimal(discount));
        }
        
        String taxRate = JsonUtil.getString(json, "taxRate");
        if (taxRate != null) {
            req.setTaxRate(new BigDecimal(taxRate));
        }
        
        String stockLevel = JsonUtil.getString(json, "stockLevel");
        if (stockLevel != null) {
            req.setStockLevel(Integer.parseInt(stockLevel));
        }
        
        req.setAgeGroup(JsonUtil.getString(json, "ageGroup"));
        req.setLocation(JsonUtil.getString(json, "location"));
        req.setGender(JsonUtil.getString(json, "gender"));
        
        String shippingCost = JsonUtil.getString(json, "shippingCost");
        if (shippingCost != null) {
            req.setShippingCost(new BigDecimal(shippingCost));
        }
        
        req.setShippingMethod(JsonUtil.getString(json, "shippingMethod"));
        req.setSeasonality(JsonUtil.getString(json, "seasonality"));
        
        return req;
    }
}
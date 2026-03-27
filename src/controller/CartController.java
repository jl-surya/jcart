package controller;

import config.AsyncExecutor;
import dto.CartItemRequest;
import dto.UpdateCartItemRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.CartItem;
import service.CartService;
import service.CustomerService;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * CartController handles customer shopping cart operations.
 * 
 * Includes:
 * - View cart with item details and stock status
 * - Add items to cart
 * - Update item quantities
 * - Remove items from cart
 * - Clear entire cart
 * - Stock validation for cart items
 */
@WebServlet(value = "/customer/cart/*", asyncSupported = true)
public class CartController extends BaseController {
    
    private final CartService cartService = new CartService();
    private final CustomerService customerService = new CustomerService();

    /**
     * Handles GET requests for cart endpoints.
     * Retrieves current cart contents.
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
            handleGetCart(req, resp);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for cart endpoints.
     * Supports add, update, remove, and clear cart operations.
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
            handleAddToCart(req, resp, jsonBody);
            return;
        }
        
        if ("/clear".equals(pathInfo)) {
            handleClearCart(req, resp);
            return;
        }
        
        if (pathInfo.matches("/\\w+")) {
            String productId = pathInfo.substring(1);
            
            if ("PATCH".equalsIgnoreCase(method)) {
                handleUpdateQuantity(req, resp, productId, jsonBody);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleRemoveFromCart(req, resp, productId);
            } else {
                sendError(resp, "Method not allowed", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Handles GET /customer/cart/ - retrieves current cart with stock validation.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    private void handleGetCart(HttpServletRequest req, HttpServletResponse resp) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    model.Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    List<CartItem> items = cartService.getCart(customer.getCustomerId());
                    
                    Map<String, Object> data = new HashMap<>();
                    List<Map<String, Object>> itemList = new java.util.ArrayList<>();
                    int totalItems = 0;
                    BigDecimal totalAmount = BigDecimal.ZERO;
                    int inStockCount = 0;
                    int lowStockCount = 0;
                    int outOfStockCount = 0;
                    
                    for (CartItem item : items) {
                        Map<String, Object> itemData = new HashMap<>();
                        itemData.put("productId", item.getProductId());
                        itemData.put("productName", item.getProductName());
                        itemData.put("quantity", item.getQuantity());
                        itemData.put("price", item.getPrice());
                        itemData.put("discount", item.getDiscount());
                        itemData.put("subtotal", item.getSubtotal());
                        itemData.put("availableStock", item.getStockLevel());
                        itemData.put("addedAt", item.getAddedAt());
                        itemData.put("expiresAt", item.getExpiresAt());
                        
                        String stockStatus;
                        String stockMessage;
                        if (item.getStockLevel() == 0) {
                            stockStatus = "OUT_OF_STOCK";
                            stockMessage = "Out of stock";
                            outOfStockCount++;
                        } else if (item.getQuantity() > item.getStockLevel()) {
                            stockStatus = "LOW_STOCK";
                            stockMessage = "Only " + item.getStockLevel() + " available";
                            lowStockCount++;
                        } else {
                            stockStatus = "IN_STOCK";
                            stockMessage = "In stock";
                            inStockCount++;
                        }
                        
                        itemData.put("stockStatus", stockStatus);
                        itemData.put("stockMessage", stockMessage);
                        itemList.add(itemData);
                        
                        totalItems += item.getQuantity();
                        totalAmount = totalAmount.add(item.getSubtotal());
                    }
                    
                    data.put("items", itemList);
                    data.put("totalItems", totalItems);
                    data.put("totalAmount", totalAmount);
                    
                    Map<String, Object> stockSummary = new HashMap<>();
                    stockSummary.put("inStock", inStockCount);
                    stockSummary.put("lowStock", lowStockCount);
                    stockSummary.put("outOfStock", outOfStockCount);
                    data.put("stockSummary", stockSummary);
                    
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
     * Handles POST /customer/cart/ - adds an item to cart.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleAddToCart(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    model.Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    CartItemRequest cartRequest = parseCartItemRequest(jsonBody);
                    if (cartRequest.getProductId() == null || cartRequest.getQuantity() == null) {
                        throw new IllegalArgumentException("Product ID and quantity are required");
                    }
                    
                    CartItem item = cartService.addToCart(customer.getCustomerId(), cartRequest);
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("productId", item.getProductId());
                    data.put("productName", item.getProductName());
                    data.put("quantity", item.getQuantity());
                    data.put("price", item.getPrice());
                    data.put("subtotal", item.getSubtotal());
                    data.put("availableStock", item.getStockLevel());
                    
                    sendSuccess(response, "Item added to cart", data);
                    
                } catch (Exception e) {
                    try {
                        sendError((HttpServletResponse) asyncContext.getResponse(), e.getMessage(), 
                                 HttpServletResponse.SC_BAD_REQUEST);
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
     * Handles PATCH /customer/cart/{productId} - updates item quantity.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param productId the product ID
     * @param jsonBody the request body as JSON string
     */
    private void handleUpdateQuantity(HttpServletRequest req, HttpServletResponse resp, 
                                       String productId, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    model.Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    UpdateCartItemRequest updateRequest = parseUpdateRequest(jsonBody);
                    if (updateRequest.getQuantity() == null) {
                        throw new IllegalArgumentException("Quantity is required");
                    }
                    
                    cartService.updateCartItemQuantity(customer.getCustomerId(), productId, updateRequest.getQuantity());
                    
                    sendSuccess(response, "Cart updated successfully", null);
                    
                } catch (Exception e) {
                    try {
                        sendError((HttpServletResponse) asyncContext.getResponse(), e.getMessage(), 
                                 HttpServletResponse.SC_BAD_REQUEST);
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
     * Handles DELETE /customer/cart/{productId} - removes an item from cart.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param productId the product ID
     */
    private void handleRemoveFromCart(HttpServletRequest req, HttpServletResponse resp, String productId) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    model.Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    cartService.removeFromCart(customer.getCustomerId(), productId);
                    
                    sendSuccess(response, "Item removed from cart", null);
                    
                } catch (Exception e) {
                    try {
                        sendError((HttpServletResponse) asyncContext.getResponse(), e.getMessage(), 
                                 HttpServletResponse.SC_BAD_REQUEST);
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
     * Handles POST /customer/cart/clear - clears entire cart.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    private void handleClearCart(HttpServletRequest req, HttpServletResponse resp) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    model.Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    cartService.clearCart(customer.getCustomerId());
                    
                    sendSuccess(response, "Cart cleared successfully", null);
                    
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
     * Parses JSON into CartItemRequest object.
     *
     * @param json the JSON string
     * @return populated CartItemRequest object
     */
    private CartItemRequest parseCartItemRequest(String json) {
        CartItemRequest req = new CartItemRequest();
        req.setProductId(JsonUtil.getString(json, "productId"));
        
        String quantity = JsonUtil.getString(json, "quantity");
        if (quantity != null) {
            try {
                req.setQuantity(Integer.parseInt(quantity));
            } catch (NumberFormatException ignored) {}
        }
        
        return req;
    }
    
    /**
     * Parses JSON into UpdateCartItemRequest object.
     *
     * @param json the JSON string
     * @return populated UpdateCartItemRequest object
     */
    private UpdateCartItemRequest parseUpdateRequest(String json) {
        UpdateCartItemRequest req = new UpdateCartItemRequest();
        
        String quantity = JsonUtil.getString(json, "quantity");
        if (quantity != null) {
            try {
                req.setQuantity(Integer.parseInt(quantity));
            } catch (NumberFormatException ignored) {}
        }
        
        return req;
    }
}
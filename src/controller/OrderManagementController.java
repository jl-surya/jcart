package controller;

import config.AsyncExecutor;
import dto.OrderSearchRequest;
import dto.OrderResponse;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Admin;
import service.AdminService;
import service.OrderService;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * OrderManagementController handles order management operations.
 * 
 * Includes:
 * - List all orders with filters and pagination
 * - View single order details
 * - Update order status
 * - Permission-based access control
 */
@WebServlet(value = "/admin/orders/*", asyncSupported = true)
public class OrderManagementController extends BaseController {
    
    private final OrderService orderService = new OrderService();
    private final AdminService adminService = new AdminService();

    /**
     * Handles GET requests for admin order endpoints.
     * Supports retrieving a single order by ID.
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
            sendError(resp, "Use POST /admin/orders/search for order search", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else if (pathInfo.matches("/\\d+")) {
            Long orderId = Long.parseLong(pathInfo.substring(1));
            
            handleGetOrder(req, resp, orderId);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for admin order endpoints.
     * Supports search and updating order status.
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
        
        if ("/search".equals(pathInfo)) {
            handleSearch(req, resp, jsonBody);
            return;
        }
        
        if (pathInfo != null && pathInfo.matches("/\\d+/status")) {
            String[] parts = pathInfo.split("/");
            Long orderId = Long.parseLong(parts[1]);
            
            handleUpdateStatus(req, resp, orderId, jsonBody);
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Handles GET /admin/orders/{id} - retrieves a single order by ID.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param orderId the order ID
     */
    private void handleGetOrder(HttpServletRequest req, HttpServletResponse resp, Long orderId) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_ORDER_VIEW)) {
                        sendError(response, "Permission denied. Requires 'orders:view'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    OrderResponse orderResponse = orderService.getOrder(orderId, null);
                    
                    sendSuccess(response, orderResponse);
                    
                } catch (Exception e) {
                    try {
                        sendError((HttpServletResponse) asyncContext.getResponse(), e.getMessage(), 
                                 HttpServletResponse.SC_NOT_FOUND);
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
     * Handles POST /admin/orders/search - searches orders with filters.
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
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_ORDER_VIEW)) {
                        sendError(response, "Permission denied. Requires 'orders:view'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    OrderSearchRequest searchReq = parseSearchRequest(jsonBody);
                    Map<String, Object> result = orderService.searchOrders(searchReq, true);
                    
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
     * Handles POST /admin/orders/{id}/status - updates order status.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param orderId the order ID
     * @param jsonBody the request body as JSON string
     */
    private void handleUpdateStatus(HttpServletRequest req, HttpServletResponse resp, Long orderId, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_ORDER_UPDATE)) {
                        sendError(response, "Permission denied. Requires 'orders:update'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    String method = JsonUtil.getString(jsonBody, "_method");
                    String status = JsonUtil.getString(jsonBody, "status");
                    
                    if (!"PATCH".equalsIgnoreCase(method)) {
                        sendError(response, "Method not allowed. Use PATCH", 
                                 HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                    
                    orderService.updateOrderStatus(orderId, status);
                    
                    sendSuccess(response, "Order status updated successfully", null);
                    
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
     * Parses JSON body into OrderSearchRequest object.
     *
     * @param json the JSON string
     * @return populated OrderSearchRequest
     */
    private OrderSearchRequest parseSearchRequest(String json) {
        OrderSearchRequest searchReq = new OrderSearchRequest();
        searchReq.setKeyword(JsonUtil.getString(json, "keyword"));
        searchReq.setCustomerId(JsonUtil.getString(json, "customerId"));
        searchReq.setStatus(JsonUtil.getString(json, "status"));
        searchReq.setPaymentStatus(JsonUtil.getString(json, "paymentStatus"));
        searchReq.setFromDate(JsonUtil.getString(json, "fromDate"));
        searchReq.setToDate(JsonUtil.getString(json, "toDate"));
        
        String minAmount = JsonUtil.getString(json, "minAmount");
        if (minAmount != null) {
            try {
                searchReq.setMinAmount(Double.parseDouble(minAmount));
            } catch (NumberFormatException ignored) {}
        }
        
        String maxAmount = JsonUtil.getString(json, "maxAmount");
        if (maxAmount != null) {
            try {
                searchReq.setMaxAmount(Double.parseDouble(maxAmount));
            } catch (NumberFormatException ignored) {}
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
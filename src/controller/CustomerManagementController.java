package controller;

import config.AsyncExecutor;
import dto.CustomerSearchRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.Admin;
import model.Customer;
import service.CustomerService;
import service.AdminService;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * CustomerManagementController handles customer management operations for admin users.
 * 
 * Includes:
 * - List all customers with pagination and filters
 * - View single customer details
 * - Deactivate customer accounts
 * - Permission-based access control
 */
@WebServlet(value = "/admin/customers/*", asyncSupported = true)
public class CustomerManagementController extends BaseController {
    
    private final CustomerService customerService = new CustomerService();
    private final AdminService adminService = new AdminService();

    /**
     * Handles GET requests for customer management endpoints.
     * Lists customers or retrieves single customer based on path.
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
        
        if (pathInfo != null && pathInfo.matches("/\\w+")) {
            String customerId = pathInfo.substring(1);
            handleGetCustomer(req, resp, customerId);
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Handles POST requests for customer management endpoints.
     * Supports DELETE operation via method override.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String jsonBody = sb.toString();
        
        String method = JsonUtil.getString(jsonBody, "_method");
        if (method == null) {
            method = req.getHeader("X-HTTP-Method-Override");
        }
        if (method == null) {
            method = req.getParameter("_method");
        }
        
        String pathInfo = req.getPathInfo();
        
        if ("/search".equals(pathInfo)) {
            handleSearch(req, resp, jsonBody);
            return;
        }
        
        if (pathInfo != null && pathInfo.matches("/\\w+")) {
            if ("DELETE".equalsIgnoreCase(method)) {
                handleDeactivateCustomer(req, resp, jsonBody);
            } else {
                sendError(resp, "Method not allowed", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Handles GET /admin/customers/{id} - retrieves a single customer.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param customerId the customer ID
     */
    private void handleGetCustomer(HttpServletRequest req, HttpServletResponse resp, String customerId) {
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

                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_CUSTOMER_VIEW)) {
                        sendError(response, "Permission denied. Requires 'customers:view'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    Customer customer = customerService.getCustomerById(customerId);
                    if (customer != null) {
                        sendSuccess(response, formatCustomer(customer));
                    } else {
                        sendError(response, "Customer not found", HttpServletResponse.SC_NOT_FOUND);
                    }
                    
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
     * Handles POST /admin/customers/search - searches customers with filters.
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

                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_CUSTOMER_VIEW)) {
                        sendError(response, "Permission denied. Requires 'customers:view'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }

                    CustomerSearchRequest searchReq = parseSearchRequestFromJson(jsonBody);
                    Map<String, Object> result = customerService.searchCustomers(searchReq);
                    
                    // Format customers for response
                    @SuppressWarnings("unchecked")
                    List<Customer> customers = (List<Customer>) result.get("customers");
                    result.put("customers", formatCustomerList(customers));
                    
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
     * Handles DELETE /admin/customers/{id} - deactivates a customer account.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleDeactivateCustomer(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String pathInfo = request.getPathInfo();
                    String sessionToken = getSessionTokenFromCookie(request);
                    
                    Admin currentAdmin = adminService.getCurrentAdmin(sessionToken);
                    if (currentAdmin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    if (pathInfo == null || pathInfo.length() <= 1) {
                        sendError(response, "Customer ID required", HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    
                    String customerId = pathInfo.substring(1);
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_CUSTOMER_DELETE)) {
                        sendError(response, "Permission denied. Requires 'customers:delete'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    Customer customer = customerService.getCustomerById(customerId);
                    if (customer == null) {
                        sendError(response, "Customer not found", HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    
                    if (!customer.isActive()) {
                        sendError(response, "Customer is already deactivated", HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    
                    customerService.deactivateCustomer(customerId);
                    sendSuccess(response, "Customer deactivated successfully", null);
                    
                } catch (Exception e) {
                    try {
                        sendError((HttpServletResponse) asyncContext.getResponse(), e.getMessage(), 
                                 HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    } catch (IOException ignored) {}
                } finally {
                    try {
                        asyncContext.complete();
                    } catch (IllegalStateException ignored) {}
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
     * Format customer list for response (excludes passwords).
     */
    private List<Map<String, Object>> formatCustomerList(List<Customer> customers) {
        return customers.stream().map(this::formatCustomer).toList();
    }
    
    /**
     * Format single customer for response (excludes password).
     */
    private Map<String, Object> formatCustomer(Customer customer) {
        Map<String, Object> data = new HashMap<>();
        data.put("customerId", customer.getCustomerId());
        data.put("username", customer.getUsername());
        data.put("email", customer.getEmail());
        data.put("phone", customer.getPhone());
        data.put("isActive", customer.isActive());
        data.put("createdAt", customer.getCreatedAt());
        data.put("updatedAt", customer.getUpdatedAt());
        return data;
    }
    
    /**
     * Parses JSON into CustomerSearchRequest object.
     *
     * @param json the JSON string
     * @return populated CustomerSearchRequest object
     */
    private CustomerSearchRequest parseSearchRequestFromJson(String json) {
        CustomerSearchRequest searchReq = new CustomerSearchRequest();
        searchReq.setSearch(JsonUtil.getString(json, "search"));
        searchReq.setStatus(JsonUtil.getString(json, "status"));
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

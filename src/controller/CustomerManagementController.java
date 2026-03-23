package controller;

import config.AsyncExecutor;
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
 * - List all customers with pagination
 * - Filter customers by status (active/all)
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
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_CUSTOMER_VIEW)) {
                        sendError(response, "Permission denied. Requires 'customers:view'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    if (pathInfo == null || pathInfo.equals("/")) {
                        int page = parseInt(request.getParameter("page"), 1);
                        int size = parseInt(request.getParameter("size"), 20);
                        String filter = request.getParameter("filter");
                        
                        List<Customer> customers;
                        int total;
                        
                        if ("active".equals(filter)) {
                            customers = customerService.getActiveCustomers(page, size);
                            total = customerService.getActiveCustomersCount();
                        } else {
                            customers = customerService.getCustomers(page, size);
                            total = customerService.getTotalCustomers();
                        }
                        
                        Map<String, Object> data = new HashMap<>();
                        data.put("customers", customers);
                        data.put("page", page);
                        data.put("size", size);
                        data.put("total", total);
                        
                        sendSuccess(response, data);
                    } else {
                        String customerId = pathInfo.substring(1);
                        Customer customer = customerService.getCustomerById(customerId);
                        if (customer != null) {
                            sendSuccess(response, customer);
                        } else {
                            sendError(response, "Customer not found", HttpServletResponse.SC_NOT_FOUND);
                        }
                    }
                    
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
            sendError(resp, "Server overloaded", HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
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
        
        if ("DELETE".equalsIgnoreCase(method)) {
            handleDeactivateCustomer(req, resp, jsonBody);
        } else {
            sendError(resp, "Method not allowed", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
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
}
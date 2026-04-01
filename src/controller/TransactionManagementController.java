package controller;

import config.AsyncExecutor;
import dto.TransactionSearchRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Admin;
import model.Transaction;
import service.AdminService;
import service.TransactionService;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * TransactionManagementController handles transaction management operations.
 * 
 * Includes:
 * - List all transactions with filters and pagination
 * - View single transaction details
 * - Process refund approvals and rejections
 * - Permission-based access control
 */
@WebServlet(value = "/admin/transactions/*", asyncSupported = true)
public class TransactionManagementController extends BaseController {
    
    private final TransactionService transactionService = new TransactionService();
    private final AdminService adminService = new AdminService();

    /**
     * Handles GET requests for admin transaction endpoints.
     * Supports retrieving a single transaction by ID.
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
            sendError(resp, "Use POST /admin/transactions/search for transaction search", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else if (pathInfo.matches("/\\d+")) {
            Long transactionId = Long.parseLong(pathInfo.substring(1));
            
            handleGetTransaction(req, resp, transactionId);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for admin transaction endpoints.
     * Supports search and refund approval/rejection actions.
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
        
        if (pathInfo != null && pathInfo.matches("/\\d+/action")) {
            String[] parts = pathInfo.split("/");
            Long transactionId = Long.parseLong(parts[1]);
            
            handleTransactionAction(req, resp, transactionId, jsonBody);
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Handles GET /admin/transactions/{id} - retrieves a single transaction by ID.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param transactionId the transaction ID
     */
    private void handleGetTransaction(HttpServletRequest req, HttpServletResponse resp, Long transactionId) {
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
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_TRANSACTION_VIEW)) {
                        sendError(response, "Permission denied. Requires 'transactions:view'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    Transaction transaction = transactionService.getTransaction(transactionId, null);
                    
                    sendSuccess(response, transaction);
                    
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
     * Handles POST /admin/transactions/search - searches transactions with filters.
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
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_TRANSACTION_VIEW)) {
                        sendError(response, "Permission denied. Requires 'transactions:view'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    TransactionSearchRequest searchReq = parseSearchRequest(jsonBody);
                    Map<String, Object> result = transactionService.searchTransactions(searchReq, true);
                    
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
     * Handles POST /admin/transactions/{id}/action - processes refund approval/rejection.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param transactionId the transaction ID
     * @param jsonBody the request body as JSON string
     */
    private void handleTransactionAction(HttpServletRequest req, HttpServletResponse resp, Long transactionId, String jsonBody) {
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
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_TRANSACTION_UPDATE)) {
                        sendError(response, "Permission denied. Requires 'transactions:update'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    String method = JsonUtil.getString(jsonBody, "_method");
                    String action = JsonUtil.getString(jsonBody, "action");
                    String reason = JsonUtil.getString(jsonBody, "reason");
                    
                    if (!"POST".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method)) {
                        sendError(response, "Method not allowed", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }
                    
                    transactionService.processRefundAction(transactionId, action, reason, currentAdmin.getAdminId());
                    
                    String message = "APPROVE".equalsIgnoreCase(action) ? "Refund approved successfully" : "Refund rejected successfully";
                    sendSuccess(response, message, null);
                    
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
     * Parses JSON body into TransactionSearchRequest object.
     *
     * @param json the JSON string
     * @return populated TransactionSearchRequest
     */
    private TransactionSearchRequest parseSearchRequest(String json) {
        TransactionSearchRequest searchReq = new TransactionSearchRequest();
        searchReq.setKeyword(JsonUtil.getString(json, "keyword"));
        searchReq.setCustomerId(JsonUtil.getString(json, "customerId"));
        
        String orderId = JsonUtil.getString(json, "orderId");
        if (orderId != null) {
            try {
                searchReq.setOrderId(Long.parseLong(orderId));
            } catch (NumberFormatException ignored) {}
        }
        
        searchReq.setType(JsonUtil.getString(json, "type"));
        searchReq.setStatus(JsonUtil.getString(json, "status"));
        searchReq.setPaymentMethod(JsonUtil.getString(json, "paymentMethod"));
        searchReq.setFromDate(JsonUtil.getString(json, "fromDate"));
        searchReq.setToDate(JsonUtil.getString(json, "toDate"));
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
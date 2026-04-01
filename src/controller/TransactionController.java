package controller;

import config.AsyncExecutor;
import dto.TransactionSearchRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Customer;
import model.Transaction;
import service.CustomerService;
import service.TransactionService;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * TransactionController handles customer transaction management endpoints.
 * 
 * Includes:
 * - View transaction history with filters
 * - View single transaction details
 * - Read-only access for customers
 */
@WebServlet(value = "/customer/transactions/*", asyncSupported = true)
public class TransactionController extends BaseController {
    
    private final TransactionService transactionService = new TransactionService();
    private final CustomerService customerService = new CustomerService();

    /**
     * Handles GET requests for customer transaction endpoints.
     * Supports listing transactions or retrieving a single transaction by ID.
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
            sendError(resp, "Use POST /customer/transactions/search for transaction search", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else if (pathInfo.matches("/\\d+")) {
            Long transactionId = Long.parseLong(pathInfo.substring(1));
            
            handleGetTransaction(req, resp, transactionId);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for customer transaction endpoints.
     * Supports transaction search operations.
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
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }
    
    /**
     * Handles GET /customer/transactions/{id} - retrieves a single transaction by ID.
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
                    Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    Transaction transaction = transactionService.getTransaction(transactionId, customer.getCustomerId());
                    
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
     * Handles GET /customer/transactions/ - retrieves customer's transactions with filters.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    /**
     * Handles POST /customer/transactions/search - searches transactions with filters.
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
                    Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    TransactionSearchRequest searchReq = parseSearchRequest(jsonBody);
                    searchReq.setCustomerId(customer.getCustomerId());

                    Map<String, Object> result = transactionService.searchTransactions(searchReq, false);
                    
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
     * Parses JSON body into TransactionSearchRequest object.
     *
     * @param json the JSON string
     * @return populated TransactionSearchRequest
     */
    private TransactionSearchRequest parseSearchRequest(String json) {
        TransactionSearchRequest searchReq = new TransactionSearchRequest();
        searchReq.setKeyword(JsonUtil.getString(json, "keyword"));
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
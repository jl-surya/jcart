package controller;

import config.AsyncExecutor;
import dto.TransactionFilterRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Customer;
import model.Transaction;
import service.CustomerService;
import service.TransactionService;
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
            handleGetTransactions(req, resp);
        } else if (pathInfo.matches("/\\d+")) {
            Long transactionId = Long.parseLong(pathInfo.substring(1));
            handleGetTransaction(req, resp, transactionId);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests - not allowed for customer transaction endpoints.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an input or output error occurs
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        sendError(resp, "Method not allowed", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }
    
    /**
     * Handles GET /customer/transactions/ - retrieves customer's transactions with filters.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    private void handleGetTransactions(HttpServletRequest req, HttpServletResponse resp) {
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
                    
                    TransactionFilterRequest filter = parseTransactionFilterRequest(request);
                    Map<String, Object> result = transactionService.getCustomerTransactions(customer.getCustomerId(), filter);
                    
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
                    
                    Transaction transaction = transactionService.getCustomerTransaction(transactionId, customer.getCustomerId());
                    
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
     * Parses request parameters into TransactionFilterRequest object.
     *
     * @param req the HTTP request object
     * @return populated TransactionFilterRequest
     */
    private TransactionFilterRequest parseTransactionFilterRequest(HttpServletRequest req) {
        TransactionFilterRequest filter = new TransactionFilterRequest();
        filter.setType(req.getParameter("type"));
        filter.setStatus(req.getParameter("status"));
        filter.setFromDate(req.getParameter("fromDate"));
        filter.setToDate(req.getParameter("toDate"));
        filter.setSortBy(req.getParameter("sortBy"));
        filter.setSortDir(req.getParameter("sortDir"));
        
        String page = req.getParameter("page");
        if (page != null) {
            try {
                filter.setPage(Integer.parseInt(page));
            } catch (NumberFormatException ignored) {}
        }
        
        String size = req.getParameter("size");
        if (size != null) {
            try {
                filter.setSize(Integer.parseInt(size));
            } catch (NumberFormatException ignored) {}
        }
        
        return filter;
    }
}
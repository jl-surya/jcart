package controller;

import config.AsyncExecutor;
import dto.OrderFilterRequest;
import dto.OrderRequest;
import dto.DirectOrderRequest;
import dto.UpdateOrderAddressRequest;
import dto.OrderResponse;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Customer;
import service.CustomerService;
import service.OrderService;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * OrderController handles customer order management endpoints.
 * 
 * Includes:
 * - View order history with filters
 * - View single order details
 * - Create order from cart
 * - Create direct order (single product)
 * - Update shipping address for pending orders
 * - Cancel pending orders
 */
@WebServlet(value = "/customer/orders/*", asyncSupported = true)
public class OrderController extends BaseController {
    
    private final OrderService orderService = new OrderService();
    private final CustomerService customerService = new CustomerService();

    /**
     * Handles GET requests for customer order endpoints.
     * Supports listing orders or retrieving a single order by ID.
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
            handleGetOrders(req, resp);
        } else if (pathInfo.matches("/\\d+")) {
            Long orderId = Long.parseLong(pathInfo.substring(1));
            handleGetOrder(req, resp, orderId);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for customer order endpoints.
     * Supports create from cart, direct order, update address, and cancel order.
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
        
        if ("/cart".equals(pathInfo)) {
            handleCreateFromCart(req, resp, jsonBody);
            return;
        }
        
        if ("/direct".equals(pathInfo)) {
            handleCreateDirect(req, resp, jsonBody);
            return;
        }

        if (pathInfo.matches("/\\d+/address")) {
            String[] parts = pathInfo.split("/");
            Long orderId = Long.parseLong(parts[1]);
            handleUpdateOrderAddress(req, resp, orderId, jsonBody);
            return;
        }
        
        if (pathInfo.matches("/\\d+/cancel")) {
            String[] parts = pathInfo.split("/");
            Long orderId = Long.parseLong(parts[1]);
            handleCancelOrder(req, resp, orderId);
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Handles GET /customer/orders/ - retrieves customer's orders with filters.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    private void handleGetOrders(HttpServletRequest req, HttpServletResponse resp) {
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
                    
                    OrderFilterRequest filter = parseOrderFilterRequest(request);
                    Map<String, Object> result = orderService.getCustomerOrders(customer.getCustomerId(), filter);
                    
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
     * Handles GET /customer/orders/{id} - retrieves a single order by ID.
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
                    Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    OrderResponse orderResponse = orderService.getOrder(orderId, customer.getCustomerId());
                    
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
     * Handles POST /customer/orders/cart - creates order from cart items.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleCreateFromCart(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
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
                    
                    OrderRequest orderRequest = parseOrderRequest(jsonBody);
                    OrderResponse orderResponse = orderService.createOrderFromCart(customer.getCustomerId(), orderRequest);
                    
                    sendSuccess(response, "Order created successfully", orderResponse);
                    
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
     * Handles POST /customer/orders/direct - creates order for a single product.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleCreateDirect(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
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
                    
                    DirectOrderRequest directRequest = parseDirectOrderRequest(jsonBody);
                    OrderResponse orderResponse = orderService.createDirectOrder(customer.getCustomerId(), directRequest);
                    
                    sendSuccess(response, "Order created successfully", orderResponse);
                    
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
     * Handles POST /customer/orders/{id}/address - updates shipping address for pending order.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param orderId the order ID
     * @param jsonBody the request body as JSON string
     */
    private void handleUpdateOrderAddress(HttpServletRequest req, HttpServletResponse resp, 
                                           Long orderId, String jsonBody) {
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
                    
                    UpdateOrderAddressRequest addressRequest = parseUpdateAddressRequest(jsonBody);
                    orderService.updateOrderAddress(orderId, customer.getCustomerId(), addressRequest);
                    
                    sendSuccess(response, "Shipping address updated successfully", null);
                    
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
     * Handles POST /customer/orders/{id}/cancel - cancels a pending order.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param orderId the order ID
     */
    private void handleCancelOrder(HttpServletRequest req, HttpServletResponse resp, Long orderId) {
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
                    
                    orderService.cancelOrder(orderId, customer.getCustomerId());
                    
                    sendSuccess(response, "Order cancelled successfully", null);
                    
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
     * Parses JSON into OrderRequest object.
     *
     * @param json the JSON string
     * @return populated OrderRequest
     */
    private OrderRequest parseOrderRequest(String json) {
        OrderRequest req = new OrderRequest();
        
        Map<String, String> parsed = JsonUtil.jsonToMap(json);
        
        req.setAddressType(parsed.get("addressType"));
        
        String addressId = parsed.get("addressId");
        if (addressId != null && !addressId.isEmpty()) {
            try {
                req.setAddressId(Long.parseLong(addressId));
            } catch (NumberFormatException ignored) {}
        }
        
        String oneTimeJson = parsed.get("oneTimeAddress");
        if (oneTimeJson != null && !oneTimeJson.isEmpty()) {
            Map<String, String> oneTimeMap = JsonUtil.jsonToMap(oneTimeJson);
            OrderRequest.OneTimeAddress oneTime = new OrderRequest.OneTimeAddress();
            oneTime.setRecipientName(oneTimeMap.get("recipientName"));
            oneTime.setAddressLine(oneTimeMap.get("addressLine"));
            oneTime.setCity(oneTimeMap.get("city"));
            oneTime.setState(oneTimeMap.get("state"));
            oneTime.setPostalCode(oneTimeMap.get("postalCode"));
            oneTime.setCountry(oneTimeMap.get("country"));
            oneTime.setPhone(oneTimeMap.get("phone"));
            req.setOneTimeAddress(oneTime);
        }
        
        req.setPaymentMethod(parsed.get("paymentMethod"));
        return req;
    }
    
    /**
     * Parses JSON into DirectOrderRequest object.
     *
     * @param json the JSON string
     * @return populated DirectOrderRequest
     */
    private DirectOrderRequest parseDirectOrderRequest(String json) {
        DirectOrderRequest req = new DirectOrderRequest();
        
        Map<String, String> parsed = JsonUtil.jsonToMap(json);
        
        req.setProductId(parsed.get("productId"));
        
        String quantity = parsed.get("quantity");
        if (quantity != null && !quantity.isEmpty()) {
            try {
                req.setQuantity(Integer.parseInt(quantity));
            } catch (NumberFormatException ignored) {}
        }
        
        req.setAddressType(parsed.get("addressType"));
        
        String addressId = parsed.get("addressId");
        if (addressId != null && !addressId.isEmpty()) {
            try {
                req.setAddressId(Long.parseLong(addressId));
            } catch (NumberFormatException ignored) {}
        }
        
        String oneTimeJson = parsed.get("oneTimeAddress");
        if (oneTimeJson != null && !oneTimeJson.isEmpty()) {
            Map<String, String> oneTimeMap = JsonUtil.jsonToMap(oneTimeJson);
            OrderRequest.OneTimeAddress oneTime = new OrderRequest.OneTimeAddress();
            oneTime.setRecipientName(oneTimeMap.get("recipientName"));
            oneTime.setAddressLine(oneTimeMap.get("addressLine"));
            oneTime.setCity(oneTimeMap.get("city"));
            oneTime.setState(oneTimeMap.get("state"));
            oneTime.setPostalCode(oneTimeMap.get("postalCode"));
            oneTime.setCountry(oneTimeMap.get("country"));
            oneTime.setPhone(oneTimeMap.get("phone"));
            req.setOneTimeAddress(oneTime);
        }
        
        req.setPaymentMethod(parsed.get("paymentMethod"));
        return req;
    }
    
    /**
     * Parses JSON into UpdateOrderAddressRequest object.
     *
     * @param json the JSON string
     * @return populated UpdateOrderAddressRequest
     */
    private UpdateOrderAddressRequest parseUpdateAddressRequest(String json) {
        UpdateOrderAddressRequest req = new UpdateOrderAddressRequest();
        
        Map<String, String> parsed = JsonUtil.jsonToMap(json);
        
        req.setAddressType(parsed.get("addressType"));
        
        String addressId = parsed.get("addressId");
        if (addressId != null && !addressId.isEmpty()) {
            try {
                req.setAddressId(Long.parseLong(addressId));
            } catch (NumberFormatException ignored) {}
        }
        
        String oneTimeJson = parsed.get("oneTimeAddress");
        if (oneTimeJson != null && !oneTimeJson.isEmpty()) {
            Map<String, String> oneTimeMap = JsonUtil.jsonToMap(oneTimeJson);
            OrderRequest.OneTimeAddress oneTime = new OrderRequest.OneTimeAddress();
            oneTime.setRecipientName(oneTimeMap.get("recipientName"));
            oneTime.setAddressLine(oneTimeMap.get("addressLine"));
            oneTime.setCity(oneTimeMap.get("city"));
            oneTime.setState(oneTimeMap.get("state"));
            oneTime.setPostalCode(oneTimeMap.get("postalCode"));
            oneTime.setCountry(oneTimeMap.get("country"));
            oneTime.setPhone(oneTimeMap.get("phone"));
            req.setOneTimeAddress(oneTime);
        }
        
        return req;
    }
    
    /**
     * Parses request parameters into OrderFilterRequest object.
     *
     * @param req the HTTP request object
     * @return populated OrderFilterRequest
     */
    private OrderFilterRequest parseOrderFilterRequest(HttpServletRequest req) {
        OrderFilterRequest filter = new OrderFilterRequest();
        filter.setStatus(req.getParameter("status"));
        filter.setFromDate(req.getParameter("fromDate"));
        filter.setToDate(req.getParameter("toDate"));
        
        String minAmount = req.getParameter("minAmount");
        if (minAmount != null) {
            try {
                filter.setMinAmount(Double.parseDouble(minAmount));
            } catch (NumberFormatException ignored) {}
        }
        
        String maxAmount = req.getParameter("maxAmount");
        if (maxAmount != null) {
            try {
                filter.setMaxAmount(Double.parseDouble(maxAmount));
            } catch (NumberFormatException ignored) {}
        }
        
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
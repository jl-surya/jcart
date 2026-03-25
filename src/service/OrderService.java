package service;

import dao.*;
import dto.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.*;
import util.DBUtil;

/**
 * OrderService handles business logic for order management.
 * 
 * Includes:
 * - Create orders from cart or direct product
 * - Order status management with state transitions
 * - Payment status management
 * - Order cancellation with stock restoration
 * - Order retrieval with filters and pagination
 * - Address resolution for shipping
 * - Invoice generation
 * - Expired order cancellation
 */
public class OrderService {
    
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final CartDAO cartDAO = new CartDAO();
    private final AddressDAO addressDAO = new AddressDAO();
    private final AddressService addressService = new AddressService();
    private final PaymentGateway paymentGateway = new PaymentGateway();
    
    // Order status transition rules
    private static final Map<String, List<String>> ALLOWED_ORDER_STATUS_TRANSITIONS = new HashMap<>();
    // Payment status transition rules
    private static final Map<String, List<String>> ALLOWED_PAYMENT_STATUS_TRANSITIONS = new HashMap<>();

    static {
        ALLOWED_ORDER_STATUS_TRANSITIONS.put("PENDING", Arrays.asList("PROCESSING", "CANCELLED"));
        ALLOWED_ORDER_STATUS_TRANSITIONS.put("PROCESSING", Arrays.asList("SHIPPED", "CANCELLED"));
        ALLOWED_ORDER_STATUS_TRANSITIONS.put("SHIPPED", Arrays.asList("DELIVERED"));
        ALLOWED_ORDER_STATUS_TRANSITIONS.put("DELIVERED", new ArrayList<>());
        ALLOWED_ORDER_STATUS_TRANSITIONS.put("CANCELLED", new ArrayList<>());

        ALLOWED_PAYMENT_STATUS_TRANSITIONS.put("PENDING", Arrays.asList("PAID", "FAILED"));
        ALLOWED_PAYMENT_STATUS_TRANSITIONS.put("PAID", Arrays.asList("REFUNDED"));
        ALLOWED_PAYMENT_STATUS_TRANSITIONS.put("FAILED", new ArrayList<>());
        ALLOWED_PAYMENT_STATUS_TRANSITIONS.put("REFUNDED", new ArrayList<>());
    }
    
    /**
     * Resolves shipping address from order request (saved address or one-time address).
     *
     * @param customerId the customer ID
     * @param request the order request
     * @return resolved Address object
     * @throws Exception if address resolution fails
     */
    private Address resolveAddress(String customerId, OrderRequest request) throws Exception {
        String addressType = request.getAddressType();
        
        if (addressType == null || "SAVED".equalsIgnoreCase(addressType)) {
            Address address = null;
            if (request.getAddressId() != null) {
                address = addressDAO.getById(request.getAddressId(), customerId);
                if (address == null) {
                    throw new IllegalArgumentException("Address not found");
                }
            } else {
                address = addressDAO.getDefaultByCustomer(customerId);
                if (address == null) {
                    throw new IllegalArgumentException("No saved address found. Please add an address or provide a new one.");
                }
            }
            
            if (address.getRecipientName() == null || address.getRecipientName().trim().isEmpty()) {
                throw new IllegalArgumentException("Recipient name is required in saved address");
            }
            if (address.getAddressLine() == null || address.getAddressLine().trim().isEmpty()) {
                throw new IllegalArgumentException("Address line is required in saved address");
            }
            if (address.getCity() == null || address.getCity().trim().isEmpty()) {
                throw new IllegalArgumentException("City is required in saved address");
            }
            if (address.getPostalCode() == null || address.getPostalCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Postal code is required in saved address");
            }
            if (address.getCountry() == null || address.getCountry().trim().isEmpty()) {
                throw new IllegalArgumentException("Country is required in saved address");
            }
            if (address.getPhone() == null || address.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number is required in saved address");
            }
            
            return address;
            
        } else if ("NEW".equalsIgnoreCase(addressType)) {
            OrderRequest.OneTimeAddress oneTime = request.getOneTimeAddress();
            if (oneTime == null) {
                throw new IllegalArgumentException("One-time address details required");
            }
            
            addressService.validateOneTimeAddress(oneTime);
            
            Address tempAddress = new Address();
            tempAddress.setCustomerId(customerId);
            tempAddress.setRecipientName(oneTime.getRecipientName());
            tempAddress.setAddressLine(oneTime.getAddressLine());
            tempAddress.setCity(oneTime.getCity());
            tempAddress.setState(oneTime.getState());
            tempAddress.setPostalCode(oneTime.getPostalCode());
            tempAddress.setCountry(oneTime.getCountry());
            tempAddress.setPhone(oneTime.getPhone());
            tempAddress.setDefault(false);
            
            return tempAddress;
            
        } else {
            throw new IllegalArgumentException("Invalid addressType. Use 'SAVED' or 'NEW'");
        }
    }
    
    /**
     * Resolves shipping address for order update (saved address or one-time address).
     *
     * @param customerId the customer ID
     * @param request the address update request
     * @return resolved Address object
     * @throws Exception if address resolution fails
     */
    private Address resolveAddressForUpdate(String customerId, UpdateOrderAddressRequest request) throws Exception {
        String addressType = request.getAddressType();
        
        if (addressType == null || "SAVED".equalsIgnoreCase(addressType)) {
            Address address = null;
            if (request.getAddressId() != null) {
                address = addressDAO.getById(request.getAddressId(), customerId);
                if (address == null) {
                    throw new IllegalArgumentException("Address not found");
                }
            } else {
                address = addressDAO.getDefaultByCustomer(customerId);
                if (address == null) {
                    throw new IllegalArgumentException("No saved address found");
                }
            }
            
            if (address.getRecipientName() == null || address.getRecipientName().trim().isEmpty()) {
                throw new IllegalArgumentException("Recipient name is required in saved address");
            }
            if (address.getAddressLine() == null || address.getAddressLine().trim().isEmpty()) {
                throw new IllegalArgumentException("Address line is required in saved address");
            }
            if (address.getCity() == null || address.getCity().trim().isEmpty()) {
                throw new IllegalArgumentException("City is required in saved address");
            }
            if (address.getPostalCode() == null || address.getPostalCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Postal code is required in saved address");
            }
            if (address.getCountry() == null || address.getCountry().trim().isEmpty()) {
                throw new IllegalArgumentException("Country is required in saved address");
            }
            if (address.getPhone() == null || address.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number is required in saved address");
            }
            
            return address;
            
        } else if ("NEW".equalsIgnoreCase(addressType)) {
            OrderRequest.OneTimeAddress oneTime = request.getOneTimeAddress();
            if (oneTime == null) {
                throw new IllegalArgumentException("One-time address details required");
            }
            
            addressService.validateOneTimeAddress(oneTime);
            
            Address tempAddress = new Address();
            tempAddress.setCustomerId(customerId);
            tempAddress.setRecipientName(oneTime.getRecipientName());
            tempAddress.setAddressLine(oneTime.getAddressLine());
            tempAddress.setCity(oneTime.getCity());
            tempAddress.setState(oneTime.getState());
            tempAddress.setPostalCode(oneTime.getPostalCode());
            tempAddress.setCountry(oneTime.getCountry());
            tempAddress.setPhone(oneTime.getPhone());
            
            return tempAddress;
            
        } else {
            throw new IllegalArgumentException("Invalid addressType. Use 'SAVED' or 'NEW'");
        }
    }
    
    /**
     * Converts DirectOrderRequest to OrderRequest for address resolution.
     *
     * @param request the direct order request
     * @return converted OrderRequest
     */
    private OrderRequest convertToOrderRequest(DirectOrderRequest request) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setAddressType(request.getAddressType());
        orderRequest.setAddressId(request.getAddressId());
        
        if (request.getOneTimeAddress() != null) {
            OrderRequest.OneTimeAddress oneTime = new OrderRequest.OneTimeAddress();
            oneTime.setRecipientName(request.getOneTimeAddress().getRecipientName());
            oneTime.setAddressLine(request.getOneTimeAddress().getAddressLine());
            oneTime.setCity(request.getOneTimeAddress().getCity());
            oneTime.setState(request.getOneTimeAddress().getState());
            oneTime.setPostalCode(request.getOneTimeAddress().getPostalCode());
            oneTime.setCountry(request.getOneTimeAddress().getCountry());
            oneTime.setPhone(request.getOneTimeAddress().getPhone());
            orderRequest.setOneTimeAddress(oneTime);
        }
        
        return orderRequest;
    }
    
    /**
     * Converts order item requests to CartItem list for processing.
     *
     * @param itemRequests list of order item requests
     * @return list of CartItem objects
     * @throws Exception if product not found
     */
    private List<CartItem> convertToCartItems(List<OrderItemRequest> itemRequests) throws Exception {
        List<CartItem> cartItems = new ArrayList<>();
        for (OrderItemRequest itemReq : itemRequests) {
            Product product = productDAO.getById(itemReq.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + itemReq.getProductId());
            }
            CartItem cartItem = new CartItem();
            cartItem.setProductId(itemReq.getProductId());
            cartItem.setQuantity(itemReq.getQuantity());
            cartItem.setProductName(product.getProductName());
            cartItem.setPrice(product.getPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItem.setStockLevel(product.getStockLevel());
            cartItems.add(cartItem);
        }
        return cartItems;
    }
    
    /**
     * Creates an order from cart items.
     *
     * @param customerId the customer ID
     * @param request the order request with address and payment details
     * @return OrderResponse with order details
     * @throws Exception if order creation fails
     */
    public OrderResponse createOrderFromCart(String customerId, OrderRequest request) throws Exception {
        List<CartItem> cartItems = cartDAO.getAllByCustomer(customerId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }
        
        Address address = resolveAddress(customerId, request);
        
        return createOrder(customerId, cartItems, address, request.getPaymentMethod());
    }
    
    /**
     * Creates a direct order for a single product.
     *
     * @param customerId the customer ID
     * @param request the direct order request
     * @return OrderResponse with order details
     * @throws Exception if order creation fails
     */
    public OrderResponse createDirectOrder(String customerId, DirectOrderRequest request) throws Exception {
        Product product = productDAO.getById(request.getProductId());
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }
        if (!product.isActive()) {
            throw new IllegalArgumentException("Product is not available");
        }
        if (request.getQuantity() < 1 || request.getQuantity() > 50) {
            throw new IllegalArgumentException("Quantity must be between 1 and 50");
        }
        
        List<CartItem> items = new ArrayList<>();
        CartItem cartItem = new CartItem();
        cartItem.setProductId(request.getProductId());
        cartItem.setQuantity(request.getQuantity());
        cartItem.setProductName(product.getProductName());
        cartItem.setPrice(product.getPrice());
        cartItem.setDiscount(product.getDiscount());
        cartItem.setStockLevel(product.getStockLevel());
        items.add(cartItem);
        
        Address address = resolveAddress(customerId, convertToOrderRequest(request));
        
        return createOrder(customerId, items, address, request.getPaymentMethod());
    }

    /**
     * Core order creation logic with transaction management.
     * Deducts stock, creates order, processes payment, and clears cart on success.
     *
     * @param customerId the customer ID
     * @param cartItems list of cart items
     * @param address shipping address
     * @param paymentMethod payment method
     * @return OrderResponse with order details
     * @throws Exception if order creation fails
     */
    private OrderResponse createOrder(String customerId, List<CartItem> cartItems, Address address, String paymentMethod) throws Exception {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                Order order = new Order();
                order.setCustomerId(customerId);
                order.setOrderStatus("PENDING");
                order.setPaymentStatus("PENDING");
                order.setShippingName(address.getRecipientName());
                order.setShippingAddressLine(address.getAddressLine());
                order.setShippingCity(address.getCity());
                order.setShippingState(address.getState());
                order.setShippingPostalCode(address.getPostalCode());
                order.setShippingCountry(address.getCountry());
                order.setInvoiceNumber(generateInvoiceNumber());
                order.setPaymentDeadline(new Timestamp(System.currentTimeMillis() + (5 * 60 * 1000)));
                
                BigDecimal totalAmount = BigDecimal.ZERO;
                List<OrderItem> orderItems = new ArrayList<>();
                
                for (CartItem cartItem : cartItems) {
                    Product product = productDAO.getById(cartItem.getProductId());
                    if (product == null) {
                        throw new IllegalArgumentException("Product not found: " + cartItem.getProductId());
                    }
                    if (!product.isActive()) {
                        throw new IllegalArgumentException("Product not available: " + product.getProductName());
                    }
                    
                    productDAO.deductStock(product.getProductId(), cartItem.getQuantity());
                    
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductId(product.getProductId());
                    orderItem.setProductName(product.getProductName());
                    orderItem.setUnitPrice(product.getPrice());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setDiscount(product.getDiscount());
                    orderItem.setTaxRate(product.getTaxRate());
                    
                    BigDecimal finalPrice = product.getFinalPrice();
                    BigDecimal subtotal = finalPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                    orderItem.setSubtotal(subtotal);
                    
                    totalAmount = totalAmount.add(subtotal);
                    orderItems.add(orderItem);
                }
                
                order.setTotalAmount(totalAmount);
                orderDAO.insert(order);
                
                for (OrderItem orderItem : orderItems) {
                    orderItem.setOrderId(order.getOrderId());
                }
                orderItemDAO.insertBatch(orderItems);
                
                Transaction payment = paymentGateway.initiatePayment(order, paymentMethod);
                payment.setOrderId(order.getOrderId());
                transactionDAO.insert(payment);
                       
                boolean paymentSuccess = true;
                payment = paymentGateway.processPayment(payment, paymentSuccess);
                transactionDAO.updateStatus(payment.getTransactionId(), payment.getTransactionStatus(), null);
                    
                if ("COMPLETED".equals(payment.getTransactionStatus())) {
                    order.setOrderStatus("PROCESSING");
                    order.setPaymentStatus("PAID");
                    int updated = orderDAO.updateStatus(order.getOrderId(), "PROCESSING", "PAID");
                    
                    cartDAO.clear(customerId);
                } else {
                    order.setOrderStatus("CANCELLED");
                    order.setPaymentStatus("FAILED");
                    orderDAO.updateStatus(order.getOrderId(), "CANCELLED", "FAILED");
                    
                    for (OrderItem orderItem : orderItems) {
                        productDAO.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
                    }
                    
                    throw new IllegalArgumentException("Payment failed");
                }
                
                conn.commit();
                
                return buildOrderResponse(order, orderItems, payment);
                
            } catch (Exception e) {
                e.printStackTrace();
                conn.rollback();
                throw e;
            }
        }
    }
    
    /**
     * Updates shipping address for an order (only allowed for pending orders).
     *
     * @param orderId the order ID
     * @param customerId the customer ID for ownership verification
     * @param request the address update request
     * @throws Exception if order not found or status doesn't allow update
     */
    public void updateOrderAddress(Long orderId, String customerId, UpdateOrderAddressRequest request) throws Exception {
        Order order = orderDAO.getById(orderId);
        if (order == null || !order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Order not found");
        }
        
        if ("SHIPPED".equals(order.getOrderStatus()) || "DELIVERED".equals(order.getOrderStatus()) || "CANCELLED".equals(order.getOrderStatus())) {
            throw new IllegalArgumentException("Address cannot be modified. Order status: " + order.getOrderStatus());
        }
        
        Address address = resolveAddressForUpdate(customerId, request);
        
        order.setShippingName(address.getRecipientName());
        order.setShippingAddressLine(address.getAddressLine());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());
        
        orderDAO.updateShippingAddress(order);
    }
    
    /**
     * Retrieves order by ID with optional customer ownership check.
     *
     * @param orderId the order ID
     * @param customerId the customer ID (null for admin view)
     * @return OrderResponse with order details
     * @throws Exception if order not found
     */
    public OrderResponse getOrder(Long orderId, String customerId) throws Exception {
        Order order = orderDAO.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        
        if (customerId != null && !order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Order not found");
        }
        
        List<OrderItem> items = orderItemDAO.getByOrderId(orderId);
        Transaction payment = transactionDAO.getByOrderIdAndType(orderId, "PAYMENT");
        
        return buildOrderResponse(order, items, payment);
    }
    
    /**
     * Retrieves paginated orders for a customer with filters.
     *
     * @param customerId the customer ID
     * @param filter the filter criteria
     * @return map containing orders, page, size, total, totalPages
     * @throws Exception if database operation fails
     */
    public Map<String, Object> getCustomerOrders(String customerId, OrderFilterRequest filter) throws Exception {
        int page = filter.getPageOrDefault();
        int size = filter.getSizeOrDefault();
        int offset = (page - 1) * size;
        
        List<Order> orders = orderDAO.getByCustomerId(customerId, filter, offset, size);
        int total = orderDAO.countByCustomerId(customerId, filter);
        
        List<OrderResponse> orderResponses = new ArrayList<>();
        for (Order order : orders) {
            List<OrderItem> items = orderItemDAO.getByOrderId(order.getOrderId());
            Transaction payment = transactionDAO.getByOrderIdAndType(order.getOrderId(), "PAYMENT");
            orderResponses.add(buildOrderResponse(order, items, payment));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("orders", orderResponses);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        
        return result;
    }
    
    /**
     * Cancels an order by customer.
     * Restores stock and initiates refund.
     *
     * @param orderId the order ID
     * @param customerId the customer ID for ownership verification
     * @throws Exception if order cannot be cancelled
     */
    public void cancelOrder(Long orderId, String customerId) throws Exception {
        Order order = orderDAO.getById(orderId);
        if (order == null || !order.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Order not found");
        }
        
        String currentOrderStatus = order.getOrderStatus();
        String currentPaymentStatus = order.getPaymentStatus();
        
        if (!"PROCESSING".equals(currentOrderStatus)) {
            throw new IllegalArgumentException(
                String.format("Order cannot be cancelled. Current status: '%s'. Only PROCESSING orders can be cancelled", 
                    currentOrderStatus)
            );
        }
        
        if (!"PAID".equals(currentPaymentStatus)) {
            throw new IllegalArgumentException(
                String.format("Order cannot be cancelled. Payment status: '%s'. Only PAID orders can be cancelled",
                    currentPaymentStatus)
            );
        }
        
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                orderDAO.updateStatus(orderId, "CANCELLED", "REFUNDED");
            
                productDAO.restoreStockForOrder(orderId);
                
                Transaction payment = transactionDAO.getByOrderIdAndType(orderId, "PAYMENT");
                
                Transaction refund = paymentGateway.processRefund(payment, "Cancelled by customer", null);
                refund.setTransactionStatus("PENDING");
                transactionDAO.insert(refund);
                
                transactionDAO.updateStatus(payment.getTransactionId(), "REFUNDED", null);
                
                conn.commit();
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }
    
    /**
     * Retrieves all orders with filters (admin view).
     *
     * @param filter the filter criteria
     * @return map containing orders, page, size, total, totalPages
     * @throws Exception if database operation fails
     */
    public Map<String, Object> getAllOrders(OrderFilterRequest filter) throws Exception {
        int page = filter.getPageOrDefault();
        int size = filter.getSizeOrDefault();
        int offset = (page - 1) * size;
        
        List<Order> orders = orderDAO.getAll(filter, offset, size);
        int total = orderDAO.countAll(filter);
        
        List<OrderResponse> orderResponses = new ArrayList<>();
        for (Order order : orders) {
            List<OrderItem> items = orderItemDAO.getByOrderId(order.getOrderId());
            Transaction payment = transactionDAO.getByOrderIdAndType(order.getOrderId(), "PAYMENT");
            orderResponses.add(buildOrderResponse(order, items, payment));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("orders", orderResponses);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        
        return result;
    }
    
    /**
     * Updates order status with validation against allowed transitions.
     *
     * @param orderId the order ID
     * @param newOrderStatus the new order status
     * @throws Exception if transition is not allowed
     */
    public void updateOrderStatus(Long orderId, String newOrderStatus) throws Exception {
        Order order = orderDAO.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        
        String currentOrderStatus = order.getOrderStatus();
        String currentPaymentStatus = order.getPaymentStatus();
        
        List<String> allowedNextOrderStatuses = ALLOWED_ORDER_STATUS_TRANSITIONS.get(currentOrderStatus);
        if (allowedNextOrderStatuses == null || !allowedNextOrderStatuses.contains(newOrderStatus)) {
            throw new IllegalArgumentException(
                String.format("Cannot change order status from '%s' to '%s'. Allowed transitions: %s",
                    currentOrderStatus, newOrderStatus, allowedNextOrderStatuses != null ? allowedNextOrderStatuses : "none")
            );
        }
        
        String newPaymentStatus = determinePaymentStatus(currentOrderStatus, newOrderStatus, currentPaymentStatus);
        
        if (!currentPaymentStatus.equals(newPaymentStatus)) {
            List<String> allowedNextPaymentStatuses = ALLOWED_PAYMENT_STATUS_TRANSITIONS.get(currentPaymentStatus);
            if (allowedNextPaymentStatuses == null || !allowedNextPaymentStatuses.contains(newPaymentStatus)) {
                throw new IllegalArgumentException(
                    String.format("Invalid payment status transition from '%s' to '%s' for order status change '%s' -> '%s'",
                        currentPaymentStatus, newPaymentStatus, currentOrderStatus, newOrderStatus)
                );
            }
        }
        
        orderDAO.updateStatus(orderId, newOrderStatus, newPaymentStatus);
    }

    /**
     * Determines payment status based on order status change.
     *
     * @param currentOrderStatus current order status
     * @param newOrderStatus new order status
     * @param currentPaymentStatus current payment status
     * @return new payment status
     */
    private String determinePaymentStatus(String currentOrderStatus, String newOrderStatus, String currentPaymentStatus) {
        if ("PENDING".equals(currentOrderStatus) && "PROCESSING".equals(newOrderStatus)) {
            return "PAID";
        }
        
        if ("PENDING".equals(currentOrderStatus) && "CANCELLED".equals(newOrderStatus)) {
            return "FAILED";
        }
        
        if ("PROCESSING".equals(currentOrderStatus) && "CANCELLED".equals(newOrderStatus)) {
            return "REFUNDED";
        }
        
        if (("PROCESSING".equals(currentOrderStatus) && "SHIPPED".equals(newOrderStatus)) ||
            ("SHIPPED".equals(currentOrderStatus) && "DELIVERED".equals(newOrderStatus))) {
            return "PAID";
        }
        
        return currentPaymentStatus;
    }
    
    /**
     * Cancels expired pending orders automatically.
     * Called by scheduled task.
     *
     * @throws Exception if database operation fails
     */
    public void cancelExpiredOrders() throws Exception {
        orderDAO.cancelExpiredOrders();
    }
    
    /**
     * Generates unique invoice number for order.
     *
     * @return invoice number
     */
    private String generateInvoiceNumber() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return "INV-" + sdf.format(new java.util.Date()) + "-" + System.currentTimeMillis();
    }
    
    /**
     * Builds OrderResponse from order, items and payment data.
     *
     * @param order the order
     * @param items the order items
     * @param payment the payment transaction
     * @return OrderResponse object
     */
    private OrderResponse buildOrderResponse(Order order, List<OrderItem> items, Transaction payment) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setInvoiceNumber(order.getInvoiceNumber());
        response.setInvoiceDate(order.getCreatedAt());
        response.setOrderStatus(order.getOrderStatus());
        response.setPaymentStatus(order.getPaymentStatus());
        response.setPaymentDeadline(order.getPaymentDeadline());
        
        Map<String, String> customer = new HashMap<>();
        customer.put("customerId", order.getCustomerId());
        response.setCustomer(customer);
        
        Map<String, Object> shippingAddress = new HashMap<>();
        shippingAddress.put("recipientName", order.getShippingName());
        shippingAddress.put("addressLine", order.getShippingAddressLine());
        shippingAddress.put("city", order.getShippingCity());
        shippingAddress.put("state", order.getShippingState());
        shippingAddress.put("postalCode", order.getShippingPostalCode());
        shippingAddress.put("country", order.getShippingCountry());
        response.setShippingAddress(shippingAddress);
        
        List<Map<String, Object>> itemList = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        
        for (OrderItem item : items) {
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("productId", item.getProductId());
            itemData.put("productName", item.getProductName());
            itemData.put("quantity", item.getQuantity());
            itemData.put("unitPrice", item.getUnitPrice());
            itemData.put("discount", item.getDiscount());
            itemData.put("taxRate", item.getTaxRate());
            itemData.put("subtotal", item.getSubtotal());
            itemList.add(itemData);
            
            subtotal = subtotal.add(item.getSubtotal());
            BigDecimal taxAmount = item.getSubtotal().multiply(item.getTaxRate()).divide(BigDecimal.valueOf(100));
            taxTotal = taxTotal.add(taxAmount);
        }
        
        response.setItems(itemList);
        response.setSubtotal(subtotal);
        response.setTax(taxTotal);
        response.setShipping(BigDecimal.ZERO);
        response.setDiscount(BigDecimal.ZERO);
        response.setTotal(order.getTotalAmount());
        
        if (payment != null) {
            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("transactionId", payment.getTransactionId());
            paymentData.put("method", payment.getTransactionMethod());
            paymentData.put("status", payment.getTransactionStatus());
            paymentData.put("reference", payment.getTransactionReference());
            paymentData.put("processedAt", payment.getProcessedAt());
            response.setPayment(paymentData);
        }
        
        return response;
    }
    
    /**
     * Inner class for order item request in direct order.
     */
    private static class OrderItemRequest {
        private String productId;
        private int quantity;
        
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
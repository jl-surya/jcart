package controller;

import config.AsyncExecutor;
import dto.AddressRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Address;
import service.AddressService;
import service.CustomerService;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * AddressController handles customer address management endpoints.
 * 
 * Includes:
 * - List all addresses for a customer
 * - Get specific address by ID
 * - Get default address
 * - Create new address
 * - Update existing address
 * - Delete address
 * - Set address as default
 */
@WebServlet(value = "/customer/addresses/*", asyncSupported = true)
public class AddressController extends BaseController {
    
    private final AddressService addressService = new AddressService();
    private final CustomerService customerService = new CustomerService();

    /**
     * Handles GET requests for address endpoints.
     * Supports listing all addresses, getting default address, or specific address by ID.
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
            handleGetAddresses(req, resp);
        } else if ("/default".equals(pathInfo)) {
            handleGetDefaultAddress(req, resp);
        } else if (pathInfo.matches("/\\d+")) {
            Long addressId = Long.parseLong(pathInfo.substring(1));
            handleGetAddress(req, resp, addressId);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for address endpoints.
     * Supports create, update, delete, and set default operations.
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
            handleCreateAddress(req, resp, jsonBody);
            return;
        }
        
        if (pathInfo.matches("/\\d+")) {
            Long addressId = Long.parseLong(pathInfo.substring(1));
            
            if ("PATCH".equalsIgnoreCase(method)) {
                handleUpdateAddress(req, resp, addressId, jsonBody);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDeleteAddress(req, resp, addressId);
            } else {
                sendError(resp, "Method not allowed", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
            return;
        }
        
        if (pathInfo.matches("/\\d+/default")) {
            String[] parts = pathInfo.split("/");
            Long addressId = Long.parseLong(parts[1]);
            handleSetDefaultAddress(req, resp, addressId);
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Handles GET /customer/addresses/ - lists all addresses for current customer.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    private void handleGetAddresses(HttpServletRequest req, HttpServletResponse resp) {
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
                    
                    List<Address> addresses = addressService.getAddresses(customer.getCustomerId());
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("addresses", addresses);
                    data.put("total", addresses.size());
                    
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
     * Handles GET /customer/addresses/{id} - retrieves a specific address.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param addressId the address ID
     */
    private void handleGetAddress(HttpServletRequest req, HttpServletResponse resp, Long addressId) {
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
                    
                    Address address = addressService.getAddress(addressId, customer.getCustomerId());
                    
                    sendSuccess(response, address);
                    
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
     * Handles GET /customer/addresses/default - retrieves default address.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    private void handleGetDefaultAddress(HttpServletRequest req, HttpServletResponse resp) {
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
                    
                    Address address = addressService.getDefaultAddress(customer.getCustomerId());
                    
                    sendSuccess(response, address);
                    
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
     * Handles POST /customer/addresses/ - creates a new address.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleCreateAddress(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
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
                    
                    AddressRequest addressRequest = parseAddressRequest(jsonBody);
                    validateAddressRequest(addressRequest);
                    
                    Address address = addressService.createAddress(customer.getCustomerId(), addressRequest);
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("addressId", address.getAddressId());
                    data.put("recipientName", address.getRecipientName());
                    data.put("addressLine", address.getAddressLine());
                    data.put("city", address.getCity());
                    data.put("isDefault", address.isDefault());
                    
                    sendSuccess(response, "Address created successfully", data);
                    
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
     * Handles PATCH /customer/addresses/{id} - updates an existing address.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param addressId the address ID
     * @param jsonBody the request body as JSON string
     */
    private void handleUpdateAddress(HttpServletRequest req, HttpServletResponse resp, 
                                      Long addressId, String jsonBody) {
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
                    
                    AddressRequest addressRequest = parseAddressRequest(jsonBody);
                    
                    addressService.updateAddress(addressId, customer.getCustomerId(), addressRequest);
                    
                    sendSuccess(response, "Address updated successfully", null);
                    
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
     * Handles DELETE /customer/addresses/{id} - deletes an address.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param addressId the address ID
     */
    private void handleDeleteAddress(HttpServletRequest req, HttpServletResponse resp, Long addressId) {
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
                    
                    addressService.deleteAddress(addressId, customer.getCustomerId());
                    
                    sendSuccess(response, "Address deleted successfully", null);
                    
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
     * Handles POST /customer/addresses/{id}/default - sets an address as default.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param addressId the address ID
     */
    private void handleSetDefaultAddress(HttpServletRequest req, HttpServletResponse resp, Long addressId) {
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
                    
                    addressService.setDefaultAddress(addressId, customer.getCustomerId());
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("addressId", addressId);
                    data.put("isDefault", true);
                    
                    sendSuccess(response, "Default address updated successfully", data);
                    
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
     * Parses JSON into AddressRequest object.
     *
     * @param json the JSON string
     * @return populated AddressRequest object
     */
    private AddressRequest parseAddressRequest(String json) {
        AddressRequest req = new AddressRequest();
        req.setRecipientName(JsonUtil.getString(json, "recipientName"));
        req.setAddressLine(JsonUtil.getString(json, "addressLine"));
        req.setCity(JsonUtil.getString(json, "city"));
        req.setState(JsonUtil.getString(json, "state"));
        req.setPostalCode(JsonUtil.getString(json, "postalCode"));
        req.setCountry(JsonUtil.getString(json, "country"));
        req.setPhone(JsonUtil.getString(json, "phone"));
        
        String isDefault = JsonUtil.getString(json, "isDefault");
        if (isDefault != null) {
            req.setIsDefault(Boolean.parseBoolean(isDefault));
        }
        
        return req;
    }
    
    /**
     * Validates address request fields.
     *
     * @param request the address request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateAddressRequest(AddressRequest request) {
        if (request.getRecipientName() == null || request.getRecipientName().trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient name is required");
        }
        if (request.getAddressLine() == null || request.getAddressLine().trim().isEmpty()) {
            throw new IllegalArgumentException("Address line is required");
        }
        if (request.getCity() == null || request.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        if (request.getPostalCode() == null || request.getPostalCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Postal code is required");
        }
        if (request.getCountry() == null || request.getCountry().trim().isEmpty()) {
            throw new IllegalArgumentException("Country is required");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
    }
}
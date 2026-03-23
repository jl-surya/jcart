package controller;

import config.AsyncExecutor;
import dto.CustomerUpdateRequest;
import dto.CustomerLoginRequest;
import dto.CustomerRegisterRequest;
import dto.PasswordChangeRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.Customer;
import model.Session;
import service.CustomerService;
import util.SessionCache;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * CustomerController handles all customer-related API endpoints.
 * 
 * Includes:
 * - Customer registration and login
 * - Profile management (view, update)
 * - Password change functionality
 * - Account deactivation
 * - Session management with async processing
 */
@WebServlet(value = "/customer/*", asyncSupported = true)
public class CustomerController extends BaseController {
    
    private final CustomerService customerService = new CustomerService();

    /**
     * Handles GET requests for customer endpoints.
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
        
        if ("/profile".equals(pathInfo)) {
            handleGetProfile(req, resp);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for customer endpoints.
     * Supports method override via _method parameter for PATCH/DELETE.
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
        
        if ("/register".equals(pathInfo)) {
            handleRegister(req, resp, jsonBody);
            return;
        }
        
        if ("/login".equals(pathInfo)) {
            handleLogin(req, resp, jsonBody);
            return;
        }
        
        String sessionToken = getSessionTokenFromCookie(req);
        if (sessionToken == null || !validateSession(sessionToken)) {
            sendError(resp, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        if ("/logout".equals(pathInfo)) {
            handleLogout(req, resp, sessionToken);
            return;
        }
        
        if ("/profile".equals(pathInfo)) {
            if ("PATCH".equalsIgnoreCase(method)) {
                handleUpdateProfile(req, resp, sessionToken, jsonBody);
            } else {
                sendError(resp, "Method not allowed. Use PATCH", 
                         HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
            return;
        }
        
        if ("/password".equals(pathInfo)) {
            handleChangePassword(req, resp, sessionToken, jsonBody);
            return;
        }
        
        if ("/account".equals(pathInfo) && "DELETE".equalsIgnoreCase(method)) {
            handleDeactivateAccount(req, resp, sessionToken, jsonBody);
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Validates session token against cache.
     *
     * @param sessionToken the session token to validate
     * @return true if session is valid, false otherwise
     */
    private boolean validateSession(String sessionToken) {
        return SessionCache.isValid(sessionToken);
    }
    
    /**
     * Handles GET /profile - retrieves current customer profile.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    private void handleGetProfile(HttpServletRequest req, HttpServletResponse resp) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    if (sessionToken == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("customerId", customer.getCustomerId());
                    data.put("username", customer.getUsername());
                    data.put("email", customer.getEmail());
                    data.put("phone", customer.getPhone());
                    data.put("isActive", customer.isActive());
                    
                    sendSuccess(response, data);
                    
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
     * Handles PATCH /profile - updates customer profile.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param sessionToken the current session token
     * @param jsonBody the request body as JSON string
     */
    private void handleUpdateProfile(HttpServletRequest req, HttpServletResponse resp, 
                                      String sessionToken, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    CustomerUpdateRequest updateReq = parseUpdateRequest(jsonBody);
                    
                    customerService.updateProfile(sessionToken, 
                        updateReq.getUsername(), 
                        updateReq.getEmail(), 
                        updateReq.getPhone());
                    
                    sendSuccess(response, "Profile updated successfully", null);
                    
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
     * Handles POST /register - creates new customer account.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleRegister(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    CustomerRegisterRequest registerReq = parseRegisterRequest(jsonBody);
                    
                    if (!registerReq.getPassword().equals(registerReq.getConfirmPassword())) {
                        throw new IllegalArgumentException("Passwords do not match");
                    }
                    
                    customerService.register(registerReq.getUsername(), 
                        registerReq.getEmail(), 
                        registerReq.getPassword(), 
                        registerReq.getPhone());
                    
                    Session session = customerService.login(registerReq.getUsername(), registerReq.getPassword());
                    
                    setSessionCookie(response, session.getSessionToken());
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("message", "Registration successful");
                    data.put("expiresInHours", session.getRemainingTimeHours());
                    
                    sendSuccess(response, "Registration successful", data);
                    
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
     * Handles POST /login - authenticates customer and creates session.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleLogin(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    CustomerLoginRequest loginReq = parseLoginRequest(jsonBody);
                    Session session = customerService.login(loginReq.getUsernameOrEmail(), loginReq.getPassword());
                    
                    setSessionCookie(response, session.getSessionToken());
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("message", "Login successful");
                    data.put("expiresInHours", session.getRemainingTimeHours());
                    
                    sendSuccess(response, "Login successful", data);
                    
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
     * Handles POST /logout - ends customer session.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param sessionToken the current session token
     */
    private void handleLogout(HttpServletRequest req, HttpServletResponse resp, String sessionToken) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    if (sessionToken != null) {
                        customerService.logout(sessionToken);
                    }
                    
                    clearSessionCookie(response);
                    
                    sendSuccess(response, "Logout successful", null);
                    
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
     * Handles POST /password - changes customer password.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param sessionToken the current session token
     * @param jsonBody the request body as JSON string
     */
    private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp, 
                                       String sessionToken, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    PasswordChangeRequest passwordReq = parsePasswordRequest(jsonBody);
                    
                    if (!passwordReq.getNewPassword().equals(passwordReq.getConfirmPassword())) {
                        throw new IllegalArgumentException("New passwords do not match");
                    }
                    
                    Customer customer = customerService.getCurrentCustomer(sessionToken);
                    if (customer == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    customerService.changePassword(sessionToken, 
                        passwordReq.getOldPassword(), 
                        passwordReq.getNewPassword());
                    
                    sendSuccess(response, "Password changed successfully", null);

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
     * Handles DELETE /account - deactivates customer account.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param sessionToken the current session token
     * @param jsonBody the request body as JSON string
     */
    private void handleDeactivateAccount(HttpServletRequest req, HttpServletResponse resp, 
                                          String sessionToken, String jsonBody) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String password = JsonUtil.getString(jsonBody, "password");
                    String confirmPassword = JsonUtil.getString(jsonBody, "confirmPassword");
                    
                    if (password == null || !password.equals(confirmPassword)) {
                        throw new IllegalArgumentException("Password confirmation does not match");
                    }
                    
                    customerService.deactivateAccount(sessionToken, password);
                    
                    Cookie cookie = new Cookie("SESSION_TOKEN", "");
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                    
                    sendSuccess(response, "Account deactivated successfully", null);
                    
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
     * Parses JSON into CustomerRegisterRequest object.
     *
     * @param json the JSON string
     * @return populated CustomerRegisterRequest object
     */
    private CustomerRegisterRequest parseRegisterRequest(String json) {
        CustomerRegisterRequest req = new CustomerRegisterRequest();
        req.setUsername(JsonUtil.getString(json, "username"));
        req.setEmail(JsonUtil.getString(json, "email"));
        req.setPassword(JsonUtil.getString(json, "password"));
        req.setConfirmPassword(JsonUtil.getString(json, "confirmPassword"));
        req.setPhone(JsonUtil.getString(json, "phone"));
        return req;
    }
    
    /**
     * Parses JSON into CustomerLoginRequest object.
     *
     * @param json the JSON string
     * @return populated CustomerLoginRequest object
     */
    private CustomerLoginRequest parseLoginRequest(String json) {
        CustomerLoginRequest req = new CustomerLoginRequest();
        req.setUsernameOrEmail(JsonUtil.getString(json, "usernameOrEmail"));
        req.setPassword(JsonUtil.getString(json, "password"));
        return req;
    }
    
    /**
     * Parses JSON into CustomerUpdateRequest object.
     *
     * @param json the JSON string
     * @return populated CustomerUpdateRequest object
     */
    private CustomerUpdateRequest parseUpdateRequest(String json) {
        CustomerUpdateRequest req = new CustomerUpdateRequest();
        req.setUsername(JsonUtil.getString(json, "username"));
        req.setEmail(JsonUtil.getString(json, "email"));
        req.setPhone(JsonUtil.getString(json, "phone"));
        return req;
    }
    
    /**
     * Parses JSON into PasswordChangeRequest object.
     *
     * @param json the JSON string
     * @return populated PasswordChangeRequest object
     */
    private PasswordChangeRequest parsePasswordRequest(String json) {
        PasswordChangeRequest req = new PasswordChangeRequest();
        req.setOldPassword(JsonUtil.getString(json, "oldPassword"));
        req.setNewPassword(JsonUtil.getString(json, "newPassword"));
        req.setConfirmPassword(JsonUtil.getString(json, "confirmPassword"));
        return req;
    }
}
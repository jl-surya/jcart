package controller;

import config.AsyncExecutor;
import dto.AdminLoginRequest;
import dto.AdminUpdateRequest;
import dto.PasswordChangeRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.Admin;
import model.Session;
import service.AdminService;
import util.SessionCache;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * AdminController handles admin authentication and profile management.
 * 
 * Includes:
 * - Admin login and logout
 * - Profile management (view, update)
 * - Password change functionality
 * - Forwarding to admin management endpoints
 */
@WebServlet(value = "/admin/*", asyncSupported = true)
public class AdminController extends BaseController {
    
    private final AdminService adminService = new AdminService();

    /**
     * Handles GET requests for admin endpoints.
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
        } else if (pathInfo != null && pathInfo.startsWith("/admins")) {
            req.getRequestDispatcher("/admin/admins" + pathInfo.substring(7)).forward(req, resp);
        } else if (pathInfo != null && pathInfo.startsWith("/customers")) {
            req.getRequestDispatcher("/admin/customers" + pathInfo.substring(9)).forward(req, resp);
        } else {
            sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Handles POST requests for admin endpoints.
     * Supports method override via _method parameter for PATCH.
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
        
        if ("/login".equals(pathInfo)) {
            handleLogin(req, resp, jsonBody);
            return;
        }
        
        if ("/logout".equals(pathInfo)) {
            handleLogout(req, resp);
            return;
        }
    
        String sessionToken = getSessionTokenFromCookie(req);
        if (sessionToken == null || !SessionCache.isValid(sessionToken)) {
            sendError(resp, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
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
        
        if ("/profile/password".equals(pathInfo)) {
            handleChangePassword(req, resp, sessionToken, jsonBody);
            return;
        }
        
        if (pathInfo != null && pathInfo.startsWith("/admins")) {
            req.getRequestDispatcher("/admin/admins" + pathInfo.substring(6)).forward(req, resp);
            return;
        }
        
        if (pathInfo != null && pathInfo.startsWith("/customers")) {
            req.getRequestDispatcher("/admin/customers" + pathInfo.substring(9)).forward(req, resp);
            return;
        }
        
        sendError(resp, "Endpoint not found", HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Handles GET /profile - retrieves current admin profile.
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
                    
                    Admin admin = adminService.getCurrentAdmin(sessionToken);
                    if (admin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("adminId", admin.getAdminId());
                    data.put("username", admin.getUsername());
                    data.put("email", admin.getEmail());
                    data.put("phone", admin.getPhone());
                    data.put("role", admin.getRole());
                    data.put("isActive", admin.isActive());
                    data.put("isSuperAdmin", admin.isSuperAdmin());
                    
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
     * Handles PATCH /profile - updates admin profile.
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
                    
                    AdminUpdateRequest updateReq = parseUpdateRequest(jsonBody);
                    
                    Admin admin = adminService.getCurrentAdmin(sessionToken);
                    if (admin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    adminService.updateOwnProfile(admin.getAdminId(), 
                        updateReq.getUsername(), updateReq.getEmail(), updateReq.getPhone());
                    
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
     * Handles POST /profile/password - changes admin password.
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
                    
                    Admin admin = adminService.getCurrentAdmin(sessionToken);
                    if (admin == null) {
                        sendError(response, "Unauthorized", HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                    
                    adminService.changePassword(admin.getAdminId(), 
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
     * Handles POST /login - authenticates admin and creates session.
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
                    
                    AdminLoginRequest loginReq = parseLoginRequest(jsonBody);
                    Session session = adminService.login(loginReq.getUsernameOrEmail(), loginReq.getPassword());
                    
                    setSessionCookie(response, session.getSessionToken());
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("message", "Admin login successful");
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
     * Handles POST /logout - ends admin session.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     */
    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) {
        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(60000);
        
        try {
            AsyncExecutor.EXECUTOR.submit(() -> {
                try {
                    HttpServletRequest request = (HttpServletRequest) asyncContext.getRequest();
                    HttpServletResponse response = (HttpServletResponse) asyncContext.getResponse();
                    
                    String sessionToken = getSessionTokenFromCookie(request);
                    if (sessionToken != null) {
                        adminService.logout(sessionToken);
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
     * Parses JSON into AdminLoginRequest object.
     *
     * @param json the JSON string
     * @return populated AdminLoginRequest object
     */
    private AdminLoginRequest parseLoginRequest(String json) {
        AdminLoginRequest req = new AdminLoginRequest();
        req.setUsernameOrEmail(JsonUtil.getString(json, "usernameOrEmail"));
        req.setPassword(JsonUtil.getString(json, "password"));
        return req;
    }
    
    /**
     * Parses JSON into AdminUpdateRequest object.
     *
     * @param json the JSON string
     * @return populated AdminUpdateRequest object
     */
    private AdminUpdateRequest parseUpdateRequest(String json) {
        AdminUpdateRequest req = new AdminUpdateRequest();
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
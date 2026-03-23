package controller;

import config.AsyncExecutor;
import dto.AdminRegisterRequest;
import dto.AdminUpdateRequest;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import model.Admin;
import service.AdminService;
import util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

/**
 * AdminManagementController handles admin management operations.
 * 
 * Includes:
 * - List all admins with pagination
 * - View single admin details
 * - Create new admin accounts
 * - Update existing admin profiles
 * - Deactivate admin accounts
 * - Permission-based access control
 */
@WebServlet(value = "/admin/admins/*", asyncSupported = true)
public class AdminManagementController extends BaseController {
    
    private final AdminService adminService = new AdminService();

    /**
     * Handles GET requests for admin management endpoints.
     * Lists admins or retrieves single admin based on path.
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
                    
                    if (pathInfo == null || pathInfo.equals("/")) {
                        if (!adminService.hasPermission(currentAdmin, AdminService.PERM_ADMIN_VIEW)) {
                            sendError(response, "Permission denied. Requires 'admin:view'", 
                                     HttpServletResponse.SC_FORBIDDEN);
                            return;
                        }
                        
                        int page = parseInt(request.getParameter("page"), 1);
                        int size = parseInt(request.getParameter("size"), 20);
                        List<Admin> admins = adminService.getAllAdmins(page, size);
                        int total = adminService.getTotalAdmins();
                        
                        Map<String, Object> data = new HashMap<>();
                        data.put("admins", admins);
                        data.put("page", page);
                        data.put("size", size);
                        data.put("total", total);
                        
                        sendSuccess(response, data);
                    } else {
                        String adminId = pathInfo.substring(1);
                        
                        if (!adminService.hasPermission(currentAdmin, AdminService.PERM_ADMIN_VIEW)) {
                            sendError(response, "Permission denied. Requires 'admin:view'", 
                                     HttpServletResponse.SC_FORBIDDEN);
                            return;
                        }
                        
                        Admin admin = adminService.getAdminById(adminId);
                        if (admin != null) {
                            sendSuccess(response, admin);
                        } else {
                            sendError(response, "Admin not found", HttpServletResponse.SC_NOT_FOUND);
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
     * Handles POST requests for admin management endpoints.
     * Supports create (POST), update (PATCH), and delete (DELETE) operations.
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
        
        if (pathInfo == null || pathInfo.equals("/")) {
            if ("POST".equalsIgnoreCase(method) || method == null) {
                handleCreateAdmin(req, resp, jsonBody);
            } else {
                sendError(resp, "Method not allowed", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } else if (pathInfo.length() > 1) {
            String adminId = pathInfo.substring(1);
            if ("PATCH".equalsIgnoreCase(method)) {
                handleUpdateAdmin(req, resp, adminId, jsonBody);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDeleteAdmin(req, resp, adminId);
            } else {
                sendError(resp, "Method not allowed", HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } else {
            sendError(resp, "Invalid path", HttpServletResponse.SC_BAD_REQUEST);
        }
    }
    
    /**
     * Handles creation of new admin account.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param jsonBody the request body as JSON string
     */
    private void handleCreateAdmin(HttpServletRequest req, HttpServletResponse resp, String jsonBody) {
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
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_ADMIN_CREATE)) {
                        sendError(response, "Permission denied. Requires 'admin:create'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    AdminRegisterRequest registerReq = parseRegisterRequest(jsonBody);
                    
                    if (!registerReq.getPassword().equals(registerReq.getConfirmPassword())) {
                        throw new IllegalArgumentException("Passwords do not match");
                    }
                    
                    Admin admin = adminService.register(
                        registerReq.getUsername(), 
                        registerReq.getEmail(), 
                        registerReq.getPassword(), 
                        registerReq.getPhone(),
                        registerReq.getRole(), 
                        registerReq.getPermissions()
                    );
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("adminId", admin.getAdminId());
                    data.put("username", admin.getUsername());
                    data.put("email", admin.getEmail());
                    data.put("role", admin.getRole());
                    
                    sendSuccess(response, "Admin created successfully", data);
                    
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
     * Handles update of existing admin account.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param adminId the ID of admin to update
     * @param jsonBody the request body as JSON string
     */
    private void handleUpdateAdmin(HttpServletRequest req, HttpServletResponse resp, String adminId, String jsonBody) {
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
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_ADMIN_UPDATE)) {
                        sendError(response, "Permission denied. Requires 'admin:update'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    AdminUpdateRequest updateReq = parseUpdateRequest(jsonBody);
                    
                    Admin updated = adminService.updateAdmin(
                        adminId, 
                        updateReq.getUsername(), 
                        updateReq.getEmail(), 
                        updateReq.getPhone(),
                        updateReq.getRole(), 
                        updateReq.getPermissions(), 
                        updateReq.getIsActive(),
                        currentAdmin
                    );
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("adminId", updated.getAdminId());
                    data.put("username", updated.getUsername());
                    data.put("email", updated.getEmail());
                    data.put("phone", updated.getPhone());
                    data.put("role", updated.getRole());
                    data.put("permissions", updated.getPermissions());
                    data.put("isActive", updated.isActive());
                    
                    sendSuccess(response, "Admin updated successfully", data);
                    
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
     * Handles deactivation of admin account.
     *
     * @param req the HTTP request object
     * @param resp the HTTP response object
     * @param adminId the ID of admin to deactivate
     */
    private void handleDeleteAdmin(HttpServletRequest req, HttpServletResponse resp, String adminId) {
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
                    
                    if (!adminService.hasPermission(currentAdmin, AdminService.PERM_ADMIN_DELETE)) {
                        sendError(response, "Permission denied. Requires 'admin:delete'", 
                                 HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                    
                    Admin adminToDelete = adminService.getAdminById(adminId);
                    if (adminToDelete != null && adminToDelete.isSuperAdmin()) {
                        sendError(response, "Cannot deactivate SUPER_ADMIN account", 
                                 HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    
                    if (adminId.equals(currentAdmin.getAdminId())) {
                        sendError(response, "Cannot deactivate your own account", 
                                 HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    
                    adminService.deactivateAdmin(adminId, currentAdmin);
                    sendSuccess(response, "Admin deactivated successfully", null);
                    
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
     * Parses JSON into AdminRegisterRequest object.
     *
     * @param json the JSON string
     * @return populated AdminRegisterRequest object
     */
    private AdminRegisterRequest parseRegisterRequest(String json) {
        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setUsername(JsonUtil.getString(json, "username"));
        req.setEmail(JsonUtil.getString(json, "email"));
        req.setPassword(JsonUtil.getString(json, "password"));
        req.setConfirmPassword(JsonUtil.getString(json, "confirmPassword"));
        req.setPhone(JsonUtil.getString(json, "phone"));
        req.setRole(JsonUtil.getString(json, "role"));

        String permissionsStr = JsonUtil.getArrayString(json, "permissions");
        if (permissionsStr != null && !permissionsStr.isEmpty()) {
            String cleaned = permissionsStr.substring(1, permissionsStr.length() - 1);
            if (!cleaned.isEmpty()) {
                String[] parts = cleaned.split(",");
                String[] permissions = new String[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    permissions[i] = parts[i].trim().replaceAll("\"", "");
                }
                req.setPermissions(permissions);
            }
        }

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
        req.setRole(JsonUtil.getString(json, "role"));
        
        String permissionsStr = JsonUtil.getArrayString(json, "permissions");
        if (permissionsStr != null && !permissionsStr.isEmpty()) {
            String cleaned = permissionsStr.substring(1, permissionsStr.length() - 1);
            if (!cleaned.isEmpty()) {
                String[] parts = cleaned.split(",");
                String[] permissions = new String[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    permissions[i] = parts[i].trim().replaceAll("\"", "");
                }
                req.setPermissions(permissions);
            }
        }

        String isActiveStr = JsonUtil.getString(json, "isActive");
        if (isActiveStr != null) {
            req.setIsActive(Boolean.parseBoolean(isActiveStr));
        }
        return req;
    }
}
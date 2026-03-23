package controller;

import dto.APIResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.JsonUtil;

import java.io.IOException;

/**
 * BaseController provides common utility methods for all API endpoints.
 * 
 * Includes:
 * - JSON response handling with consistent API structure
 * - Session token management via cookies
 * - Request parsing utilities using JsonUtil
 */
public class BaseController extends HttpServlet {
    
    /**
     * Sends object as JSON response.
     *
     * @param resp the HTTP response object
     * @param data the object to be serialized to JSON
     * @throws IOException if an input or output error occurs
     */
    protected void sendJson(HttpServletResponse resp, Object data) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(JsonUtil.toJson(data));
    }
    
    /**
     * Sends success response with custom message and data.
     *
     * @param resp    the HTTP response object
     * @param message the success message
     * @param data    the response data payload
     * @throws IOException if an input or output error occurs
     */
    protected void sendSuccess(HttpServletResponse resp, String message, Object data) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        APIResponse response = new APIResponse(true, message, data);
        sendJson(resp, response);
    }
    
    /**
     * Sends success response with default message.
     *
     * @param resp the HTTP response object
     * @param data the response data payload
     * @throws IOException if an input or output error occurs
     */
    protected void sendSuccess(HttpServletResponse resp, Object data) throws IOException {
        sendSuccess(resp, "Success", data);
    }
    
    /**
     * Sends error response with message and status code.
     *
     * @param resp       the HTTP response object
     * @param message    the error message
     * @param statusCode the HTTP status code
     * @throws IOException if an input or output error occurs
     */
    protected void sendError(HttpServletResponse resp, String message, int statusCode) throws IOException {
        resp.setStatus(statusCode);
        APIResponse response = new APIResponse(false, message, null);
        sendJson(resp, response);
    }
    
    /**
     * Safely parses string to integer, returns default if parsing fails.
     *
     * @param val the string value to parse
     * @param def the default value to return if parsing fails
     * @return the parsed integer value, or the default value if parsing fails
     */
    protected int parseInt(String val, int def) {
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return def;
        }
    }
    
    /**
     * Retrieves session token from SESSION_TOKEN cookie.
     *
     * @param req the HTTP request object
     * @return the session token value, or null if not found
     */
    protected String getSessionTokenFromCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SESSION_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    
    /**
     * Sets SESSION_TOKEN cookie in response.
     *
     * @param resp         the HTTP response object
     * @param sessionToken the session token value to store
     */
    protected void setSessionCookie(HttpServletResponse resp, String sessionToken) {
        Cookie cookie = new Cookie("SESSION_TOKEN", sessionToken);
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        resp.addCookie(cookie);
    }
    
    /**
     * Clears SESSION_TOKEN cookie.
     *
     * @param resp the HTTP response object
     */
    protected void clearSessionCookie(HttpServletResponse resp) {
        Cookie cookie = new Cookie("SESSION_TOKEN", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        resp.addCookie(cookie);
    }
}
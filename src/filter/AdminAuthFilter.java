package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import util.SessionCache;

import java.io.IOException;

/**
 * AdminAuthFilter handles authentication for admin endpoints.
 * 
 * Includes:
 * - Session token validation from header or cookie
 * - User type verification (ensures ADMIN role)
 * - Bypass authentication for login endpoint
 * - Unauthorized response for invalid or missing tokens
 */
@WebFilter(asyncSupported = true, urlPatterns = "/admin/*")
public class AdminAuthFilter implements Filter {
    
    /**
     * Filters incoming requests and validates admin authentication.
     * Bypasses authentication for login endpoint.
     *
     * @param request the servlet request
     * @param response the servlet response
     * @param chain the filter chain
     * @throws IOException if an I/O error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        if (path.equals("/admin/login")) {
            chain.doFilter(request, response);
            return;
        }
        
        String sessionToken = getSessionToken(req);
        
        if (sessionToken == null || !SessionCache.isValid(sessionToken)) {
            sendUnauthorized(resp);
            return;
        }
        
        String userType = SessionCache.getUserType(sessionToken);
        if (!"ADMIN".equals(userType)) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"success\":false,\"error\":\"Admin access required\"}");
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    /**
     * Extracts session token from header or cookie.
     * Checks X-Session-Token header first, then SESSION_TOKEN cookie.
     *
     * @param req the HTTP request
     * @return session token if found, null otherwise
     */
    private String getSessionToken(HttpServletRequest req) {
        String sessionToken = req.getHeader("X-Session-Token");
        if (sessionToken == null) {
            Cookie[] cookies = req.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("SESSION_TOKEN".equals(cookie.getName())) {
                        sessionToken = cookie.getValue();
                        break;
                    }
                }
            }
        }
        return sessionToken;
    }
    
    /**
     * Sends unauthorized response with JSON error message.
     *
     * @param resp the HTTP response object
     * @throws IOException if an I/O error occurs
     */
    private void sendUnauthorized(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json");
        resp.getWriter().write("{\"success\":false,\"error\":\"Unauthorized\"}");
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}
    
    @Override
    public void destroy() {}
}
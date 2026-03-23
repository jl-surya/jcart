package filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import util.SessionCache;

import java.io.IOException;

/**
 * CustomerAuthFilter handles authentication for customer endpoints.
 * 
 * Includes:
 * - Session token validation from header or cookie
 * - Bypass authentication for public endpoints (login, register)
 * - Unauthorized response for invalid or missing tokens
 */
@WebFilter(asyncSupported = true, urlPatterns = "/customer/*")
public class CustomerAuthFilter implements Filter {
    
    /**
     * Filters incoming requests and validates authentication.
     * Bypasses authentication for login and register endpoints.
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
        
        if (path.equals("/customer/login") || path.equals("/customer/register")) {
            chain.doFilter(request, response);
            return;
        }
        
        String sessionToken = getSessionToken(req);
        
        if (sessionToken == null || !SessionCache.isValid(sessionToken)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"success\":false,\"error\":\"Unauthorized\"}");
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
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}
    
    @Override
    public void destroy() {}
}
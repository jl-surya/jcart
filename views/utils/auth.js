/**
 * Authentication Utilities - Centralized Auth State Management
 * 
 * Includes:
 * 1. Standardized authentication state checking across pages
 * 2. Session data management with localStorage persistence  
 * 3. User redirection for protected pages and actions
 * 4. Centralized logout functionality with session cleanup
 * 5. Cross-page auth state synchronization and validation
 */

(function() {
    'use strict';

    /**
     * Check if user is authenticated
     * @returns {boolean} - True if user is logged in
     */
    function isAuthenticated() {
        return localStorage.getItem('isLoggedIn') === 'true' && localStorage.getItem('user');
    }

    /**
     * Get current user data from localStorage
     * @returns {Object|null} - User object or null if not logged in
     */
    function getCurrentUser() {
        if (!isAuthenticated()) {
            return null;
        }
        
        try {
            var userData = localStorage.getItem('user');
            return userData ? JSON.parse(userData) : null;
        } catch (error) {
            console.error('Error parsing user data:', error);
            return null;
        }
    }

    /**
     * Get user's role
     * @returns {string|null} - User role or null if not logged in
     */
    function getUserRole() {
        var user = getCurrentUser();
        return user ? user.role : null;
    }

    /**
     * Check if user has specific role
     * @param {string} role - Role to check
     * @returns {boolean} - True if user has the specified role
     */
    function hasRole(role) {
        return getUserRole() === role;
    }

    /**
     * Check if user is a customer
     * @returns {boolean} - True if user is a customer
     */
    function isCustomer() {
        return hasRole('CUSTOMER');
    }

    /**
     * Check if user is an admin
     * @returns {boolean} - True if user is an admin
     */
    function isAdmin() {
        return hasRole('ADMIN');
    }

    /**
     * Log out user and clear session data
     */
    function logout() {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        
        window.location.href = '/JCart/views/index.html';
    }

    /**
     * Redirect to appropriate login page
     * @param {string} returnUrl - URL to return to after login
     */
    function redirectToLogin(returnUrl) {
        var loginUrl = '/JCart/views/features/auth/customer/login/';
        
        if (returnUrl) {
            loginUrl += '?returnUrl=' + encodeURIComponent(returnUrl);
        }
        
        window.location.href = loginUrl;
    }

    /**
     * Require authentication - redirect if not logged in
     * @param {string} returnUrl - URL to return to after login
     * @returns {boolean} - True if authenticated, false if redirected
     */
    function requireAuth(returnUrl) {
        if (!isAuthenticated()) {
            redirectToLogin(returnUrl || window.location.pathname);
            return false;
        }
        return true;
    }

    window.authUtils = {
        isAuthenticated: isAuthenticated,
        getCurrentUser: getCurrentUser,
        getUserRole: getUserRole,
        hasRole: hasRole,
        isCustomer: isCustomer,
        isAdmin: isAdmin,
        logout: logout,
        redirectToLogin: redirectToLogin,
        requireAuth: requireAuth
    };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = {
            isAuthenticated: isAuthenticated,
            getCurrentUser: getCurrentUser,
            getUserRole: getUserRole,
            hasRole: hasRole,
            isCustomer: isCustomer,
            isAdmin: isAdmin,
            logout: logout,
            redirectToLogin: redirectToLogin,
            requireAuth: requireAuth
        };
    }

})();

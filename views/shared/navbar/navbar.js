/**
 * Shared Navigation Bar Component
 * 
 * Includes:
 * 1. Authentication state management and dynamic display
 * 2. Real-time cart badge with item count updates
 * 3. User logout functionality with session cleanup
 * 4. Responsive navigation menu with auth-based visibility
 * 5. Auto-refresh cart count and error handling for unauthorized access
 */

(function () {
    'use strict';

    var MAX_RETRIES = 20;        // Max attempts to find navbar elements
    var RETRY_INTERVAL = 100;    // Wait time between retries (ms)
    var REDIRECT_DELAY = 1000;    // Delay before redirect after logout (ms)

    /**
     * Perform logout with confirmation
     * Clears session data and redirects to home page
     */
    async function logout() {
        var confirmed = confirm('Are you sure you want to logout?');
        if (!confirmed) return;

        try {
            // Call logout API endpoint
            var response = await fetch('/JCart/customer/logout', {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' }
            });

            if (response.ok || response.status === 401) {
                clearSession();
                window.showToast('Logged out successfully', 'success');
                setTimeout(function () {
                    window.location.href = '/JCart/views/index.html';
                }, REDIRECT_DELAY);
            } else {
                // API error - still clear session and redirect
                console.error('Logout failed:', response.status);
                clearSession();
                window.location.href = '/JCart/views/index.html';
            }
        } catch (error) {
            console.error('Logout error:', error);
            clearSession();
            window.location.href = '/JCart/views/index.html';
        }
    }

    /**
     * Clear user session data from localStorage
     */
    function clearSession() {
        localStorage.removeItem('isLoggedIn');
        localStorage.removeItem('user');
    }

    /**
     * Initialize navbar auth state with retry logic
     * Shows/hides login/logout buttons based on authentication status
     */
    function initializeNavbar(retryCount) {
        retryCount = retryCount || 0;

        var isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
        var navLoginRegister = document.getElementById('navLoginRegister');
        var navLogout = document.getElementById('navLogout');
        var navCart = document.getElementById('navCart');
        var navOrders = document.getElementById('navOrders');
        var navTransactions = document.getElementById('navTransactions');

        if (!navLoginRegister || !navLogout) {
            if (retryCount < MAX_RETRIES) {
                setTimeout(function () {
                    initializeNavbar(retryCount + 1);
                }, RETRY_INTERVAL);
            }
            return;
        }

        if (isLoggedIn) {
            navLoginRegister.classList.add('hidden');
            navLogout.classList.remove('hidden');
            if (navCart) {
                navCart.classList.remove('hidden');
                loadCartCount();
            }
            if (navOrders) {
                navOrders.classList.remove('hidden');
            }
            if (navTransactions) {
                navTransactions.classList.remove('hidden');
            }
        } else {
            navLoginRegister.classList.remove('hidden');
            navLogout.classList.add('hidden');
            if (navCart) {
                navCart.classList.add('hidden');
            }
            if (navOrders) {
                navOrders.classList.add('hidden');
            }
            if (navTransactions) {
                navTransactions.classList.add('hidden');
            }
        }

        var logoutBtn = document.getElementById('navLogoutBtn');
        if (logoutBtn && !logoutBtn._bound) {
            logoutBtn.addEventListener('click', function (e) {
                e.preventDefault();
                logout();
            });
            logoutBtn._bound = true; // Mark as bound to prevent duplicates
        }

        highlightActiveLink();
    }

    /**
     * Highlight the nav link matching the current page URL
     * Adds 'active' class to current page link
     */
    function highlightActiveLink() {
        var currentPath = window.location.pathname;
        var links = document.querySelectorAll('.navbar .nav-links a');
        
        links.forEach(function (link) {
            var href = link.getAttribute('href');
            if (href && currentPath.indexOf(href) !== -1) {
                link.classList.add('active');
            } else {
                link.classList.remove('active');
            }
        });
    }

    /**
     * Load cart item count from API
     * Updates cart badge in navbar
     * Only loads if user is authenticated
     */
    function loadCartCount() {
        var isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
        if (!isLoggedIn) {
            updateCartBadge(0);
            return;
        }
        
        fetch('/JCart/customer/cart', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
        })
        .then(function (response) {
            if (response.ok) {
                return response.json();
            }
            if (response.status === 401) {
                localStorage.removeItem('isLoggedIn');
                updateCartBadge(0);
                return null;
            }
            throw new Error('Failed to load cart');
        })
        .then(function (data) {
            if (!data) return;
            var result = data.data || data;
            var cartItems = result.items || [];
            updateCartBadge(cartItems.length);
        })
        .catch(function (error) {
            console.error('Error loading cart count:', error);
            updateCartBadge(0);
        });
    }

    /**
     * Update cart badge count
     * @param {number} count - Number of items in cart
     */
    function updateCartBadge(count) {
        var cartBadge = document.getElementById('cartBadge');
        if (cartBadge) {
            cartBadge.textContent = count.toString();
            cartBadge.setAttribute('data-count', count);
            
            if (count > 0) {
                cartBadge.style.display = 'inline-flex';
            } else {
                cartBadge.style.display = 'none';
            }
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        initializeNavbar();
    });

    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) initializeNavbar();
    });

    window.initializeNavbar = initializeNavbar;
    window.logout = logout;
    window.updateCartBadge = updateCartBadge;
    window.loadCartCount = loadCartCount;
})();


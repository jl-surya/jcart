/**
 * Admin Sidebar Component
 * 
 * Includes:
 * 1. Dynamic sidebar loading and initialization
 * 2. Active page highlighting based on current URL
 * 3. Sidebar toggle functionality for responsive design
 * 4. Admin logout with session cleanup
 * 5. Admin profile display from localStorage
 */

(function () {
    'use strict';

    var MAX_RETRIES = 20;
    var RETRY_INTERVAL = 100;
    var REDIRECT_DELAY = 500;

    var PERM_ADMIN_VIEW = 'admins:view';
    var PERM_CUSTOMER_VIEW = 'customers:view';
    var PERM_PRODUCT_VIEW = 'products:view';
    var PERM_ORDER_VIEW = 'orders:view';
    var PERM_TRANSACTION_VIEW = 'transactions:view';

    /**
     * Initialize admin sidebar with retry logic
     * @param {string} activePage - The data-page value to mark as active
     * @param {number} retryCount - Current retry attempt
     */
    function initializeAdminSidebar(activePage, retryCount) {
        retryCount = retryCount || 0;

        var sidebar = document.getElementById('sidebar');
        var logoutBtn = document.getElementById('logoutBtn');

        if (!sidebar || !logoutBtn) {
            if (retryCount < MAX_RETRIES) {
                setTimeout(function () {
                    initializeAdminSidebar(activePage, retryCount + 1);
                }, RETRY_INTERVAL);
            }
            return;
        }

        // Apply permission-based visibility
        applyPermissionBasedVisibility(sidebar);

        if (activePage) {
            var navItems = sidebar.querySelectorAll('.nav-item');
            navItems.forEach(function (item) {
                item.classList.remove('active');
                if (item.getAttribute('data-page') === activePage) {
                    item.classList.add('active');
                }
            });
        }

        // Initialize logout button
        if (!logoutBtn._bound) {
            logoutBtn.addEventListener('click', function (e) {
                e.preventDefault();
                adminLogout();
            });
            logoutBtn._bound = true;
        }

        // Initialize sidebar toggle
        initializeSidebarToggle();

        // Display admin info
        displayAdminInfo();
    }

    /**
     * Apply permission-based visibility to sidebar menu items
     */
    function applyPermissionBasedVisibility(sidebar) {
        var permissions = getAdminPermissions();

        var adminNavItem = sidebar.querySelector('.nav-item[data-page="adminMt"]');
        if (adminNavItem && !hasPermission(permissions, PERM_ADMIN_VIEW)) {
            adminNavItem.style.display = 'none';
        }

        var customerNavItem = sidebar.querySelector('.nav-item[data-page="customerMt"]');
        if (customerNavItem && !hasPermission(permissions, PERM_CUSTOMER_VIEW)) {
            customerNavItem.style.display = 'none';
        }

        var productNavItem = sidebar.querySelector('.nav-item[data-page="productMt"]');
        if (productNavItem && !hasPermission(permissions, PERM_PRODUCT_VIEW)) {
            productNavItem.style.display = 'none';
        }

        var orderNavItem = sidebar.querySelector('.nav-item[data-page="orderMt"]');
        if (orderNavItem && !hasPermission(permissions, PERM_ORDER_VIEW)) {
            orderNavItem.style.display = 'none';
        }

        var transactionNavItem = sidebar.querySelector('.nav-item[data-page="transactionMt"]');
        if (transactionNavItem && !hasPermission(permissions, PERM_TRANSACTION_VIEW)) {
            transactionNavItem.style.display = 'none';
        }

        hideSectionHeadersIfEmpty(sidebar);
    }

    /**
     * Hide section headers if all nav items under them are hidden
     */
    function hideSectionHeadersIfEmpty(sidebar) {
        var sections = ['management', 'store'];
        
        sections.forEach(function(sectionName) {
            var sectionHeader = sidebar.querySelector('.nav-section-title[data-section="' + sectionName + '"]');
            if (!sectionHeader) return;
            
            var sectionItems = sidebar.querySelectorAll('.nav-item[data-section="' + sectionName + '"]');
            var hasVisibleItems = false;
            
            sectionItems.forEach(function(item) {
                if (item.style.display !== 'none') {
                    hasVisibleItems = true;
                }
            });
            
            sectionHeader.style.display = hasVisibleItems ? 'block' : 'none';
        });
    }

    /**
     * Get admin permissions from localStorage
     */
    function getAdminPermissions() {
        try {
            var admin = JSON.parse(localStorage.getItem('admin') || '{}');
            
            if (admin.isSuperAdmin) {
                return ['*'];
            }
            return admin.permissions || [];
        } catch (e) {
            return [];
        }
    }

    /**
     * Check if permissions array includes the required permission
     */
    function hasPermission(permissions, required) {
        if (permissions.includes('*')) return true;
        return permissions.includes(required);
    }

    /**
     * Initialize sidebar toggle for responsive design
     */
    function initializeSidebarToggle() {
        var menuToggle = document.getElementById('menuToggle');
        var sidebar = document.getElementById('sidebar');

        if (menuToggle && sidebar && !menuToggle._bound) {
            menuToggle.addEventListener('click', function () {
                sidebar.classList.toggle('open');
                sidebar.classList.toggle('collapsed');
                document.body.classList.toggle('sidebar-collapsed');
            });
            menuToggle._bound = true;
        }

        // Handle responsive sidebar
        function handleResize() {
            if (window.innerWidth <= 768) {
                if (sidebar) {
                    sidebar.classList.add('collapsed');
                    sidebar.classList.remove('open');
                }
                document.body.classList.add('sidebar-collapsed');
            } else {
                if (sidebar) {
                    sidebar.classList.remove('collapsed');
                    sidebar.classList.remove('open');
                }
                document.body.classList.remove('sidebar-collapsed');
            }
        }

        handleResize();
        window.addEventListener('resize', handleResize);
    }

    /**
     * Display admin name in profile elements
     */
    function displayAdminInfo() {
        var adminData;
        try {
            adminData = JSON.parse(localStorage.getItem('admin') || '{}');
        } catch (e) {
            adminData = {};
        }

        var adminName = adminData.name || 'Admin';

        var adminNameEl = document.getElementById('adminName');
        var profileNameEl = document.getElementById('profileName');

        if (adminNameEl) adminNameEl.textContent = adminName;
        if (profileNameEl) profileNameEl.textContent = adminName;
    }

    /**
     * Perform admin logout
     */
    async function adminLogout() {
        try {
            await fetch('/JCart/admin/logout', {
                method: 'POST',
                credentials: 'include'
            });
        } catch (error) {
            console.error('Logout request error:', error);
        }

        localStorage.removeItem('adminLoggedIn');
        localStorage.removeItem('admin');

        if (window.showToast) {
            window.showToast('Logged out successfully', 'success');
        }

        setTimeout(function () {
            window.location.href = '/JCart/views/features/auth/admin/login/';
        }, REDIRECT_DELAY);
    }

    /**
     * Check admin authentication and redirect if not logged in
     */
    function checkAdminAuth() {
        if (localStorage.getItem('adminLoggedIn') !== 'true') {
            window.location.replace('/JCart/views/features/auth/admin/login/');
            return false;
        }
        return true;
    }

    // Expose functions globally
    window.initializeAdminSidebar = initializeAdminSidebar;
    window.adminLogout = adminLogout;
    window.checkAdminAuth = checkAdminAuth;
    window.displayAdminInfo = displayAdminInfo;

})();

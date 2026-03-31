/**
 * Admin Dashboard Handler
 * Manages dashboard interactions, admin info display, and permission-based quick actions.
 * Sidebar functionality is handled by shared admin-sidebar component.
 */

(function () {
    'use strict';

    var currentPermissions = [];

    document.addEventListener('DOMContentLoaded', function () {
        loadCurrentAdminPermissions();
        applyPermissionBasedUI();
        
        if (window.displayAdminInfo) {
            window.displayAdminInfo();
        } else {
            initializeAdminInfo();
        }
    });

    /**
     * Load current admin permissions from localStorage
     */
    function loadCurrentAdminPermissions() {
        try {
            var admin = JSON.parse(localStorage.getItem('admin') || '{}');
            currentPermissions = admin.permissions || [];
            
            if (admin.isSuperAdmin) {
                currentPermissions = ['*'];
            }
        } catch (e) {
            currentPermissions = [];
        }
    }

    /**
     * Check if current admin has a specific permission
     */
    function hasPermission(permission) {
        if (currentPermissions.includes('*')) return true;
        return currentPermissions.includes(permission);
    }

    /**
     * Apply permission-based UI visibility to quick action cards
     */
    function applyPermissionBasedUI() {
        var actionCards = document.querySelectorAll('.action-card[data-permission]');
        actionCards.forEach(function(card) {
            var permission = card.getAttribute('data-permission');
            if (permission && !hasPermission(permission)) {
                card.style.display = 'none';
            }
        });
    }

    /**
     * Initialize admin information display (fallback if shared component not loaded)
     */
    function initializeAdminInfo() {
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

})();

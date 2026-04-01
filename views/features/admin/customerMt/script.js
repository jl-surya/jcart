/**
 * Customer Management Handler
 * Manages customer viewing, deactivation, and table display with filters and pagination.
 */

(function () {
    'use strict';

    var customersData = [];
    var pagination = null;
    var currentPermissions = [];

    var PERM_CUSTOMER_VIEW = 'customers:view';
    var PERM_CUSTOMER_DELETE = 'customers:delete';

    var searchTerm = '';
    var statusFilter = '';
    var sortBy = '';
    var sortDir = 'desc';

    document.addEventListener('DOMContentLoaded', function () {
        loadCurrentAdminPermissions();
        applyPermissionBasedUI();
        initializePagination();
        initializeEventListeners();
        loadCustomers(1, 15);
    });

    /**
     * Load current admin permissions from localStorage
     */
    function loadCurrentAdminPermissions() {
        try {
            var admin = JSON.parse(localStorage.getItem('admin') || '{}');
            currentPermissions = admin.permissions || [];
            
            // Super admin has all permissions
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
     * Apply permission-based UI visibility
     */
    function applyPermissionBasedUI() {
        if (!hasPermission(PERM_CUSTOMER_VIEW)) {
            showPermissionError();
            return;
        }
    }

    /**
     * Show permission error and redirect
     */
    function showPermissionError() {
        window.showToast('You do not have permission to view customers', 'error');
        setTimeout(function() {
            window.location.href = '/JCart/views/features/admin/dashboard/';
        }, 2000);
    }

    /**
     * Initialize shared pagination component
     */
    function initializePagination() {
        fetch('/JCart/views/shared/pagination/pagination.html')
            .then(function (response) {
                if (!response.ok) throw new Error('Failed to load pagination');
                return response.text();
            })
            .then(function (html) {
                document.getElementById('paginationPlaceholder').innerHTML = html;
                
                pagination = new window.Pagination({
                    onPageChange: function (page, size) {
                        loadCustomers(page, size);
                    }
                });
                pagination.init();
            })
            .catch(function (error) {
                console.error('Error loading pagination:', error);
            });
    }

    /**
     * Initialize all event listeners
     */
    function initializeEventListeners() {
        var retryBtn = document.getElementById('retryBtn');
        if (retryBtn) {
            retryBtn.addEventListener('click', function() {
                loadCustomers(1, 15);
            });
        }

        var applyFiltersBtn = document.getElementById('applyFiltersBtn');
        if (applyFiltersBtn) {
            applyFiltersBtn.addEventListener('click', function () {
                applyFilters();
            });
        }

        var clearFiltersBtn = document.getElementById('clearFiltersBtn');
        if (clearFiltersBtn) {
            clearFiltersBtn.addEventListener('click', function () {
                clearFilters();
            });
        }

        var searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') {
                    applyFilters();
                }
            });
        }

        var deleteModalClose = document.getElementById('deleteModalClose');
        var deleteCancelBtn = document.getElementById('deleteCancelBtn');
        if (deleteModalClose) deleteModalClose.addEventListener('click', closeDeleteModal);
        if (deleteCancelBtn) deleteCancelBtn.addEventListener('click', closeDeleteModal);

        var viewModalClose = document.getElementById('viewModalClose');
        var viewCloseBtn = document.getElementById('viewCloseBtn');
        if (viewModalClose) viewModalClose.addEventListener('click', closeViewModal);
        if (viewCloseBtn) viewCloseBtn.addEventListener('click', closeViewModal);

        var deleteModal = document.getElementById('deleteModal');
        var viewModal = document.getElementById('viewModal');
        if (deleteModal) {
            deleteModal.addEventListener('click', function (e) {
                if (e.target === this) closeDeleteModal();
            });
        }
        if (viewModal) {
            viewModal.addEventListener('click', function (e) {
                if (e.target === this) closeViewModal();
            });
        }
    }

    /**
     * Apply filters and load customers
     */
    function applyFilters() {
        var searchInput = document.getElementById('searchInput');
        var statusFilterEl = document.getElementById('statusFilter');
        var sortByFilterEl = document.getElementById('sortByFilter');
        var sortDirFilterEl = document.getElementById('sortDirFilter');

        searchTerm = searchInput ? searchInput.value.trim() : '';
        statusFilter = statusFilterEl ? statusFilterEl.value : '';
        sortBy = sortByFilterEl ? sortByFilterEl.value : '';
        sortDir = sortDirFilterEl ? sortDirFilterEl.value : 'desc';

        loadCustomers(1, pagination ? pagination.getPageSize() : 15);
    }

    /**
     * Build query string with filters
     */
    function buildQueryString(page, size) {
        var params = [];
        params.push('page=' + page);
        params.push('size=' + size);
        
        if (searchTerm) {
            params.push('search=' + encodeURIComponent(searchTerm));
        }
        if (statusFilter) {
            params.push('status=' + encodeURIComponent(statusFilter));
        }
        if (sortBy) {
            params.push('sortBy=' + encodeURIComponent(sortBy));
        }
        if (sortDir) {
            params.push('sortDir=' + encodeURIComponent(sortDir));
        }
        
        return params.join('&');
    }

    /**
     * Load customers from API with filters
     */
    async function loadCustomers(page, size) {
        showLoadingState();
        
        try {
            var searchRequest = buildSearchRequest(page, size);
            var response = await fetch('/JCart/admin/customers/search', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify(searchRequest)
            });

            if (response.status === 401) {
                window.location.href = '/JCart/views/features/auth/admin/login/';
                return;
            }

            if (response.status === 403) {
                showPermissionError();
                return;
            }

            if (!response.ok) {
                throw new Error('Failed to load customers');
            }

            var data = await response.json();
            var result = data.data || data;
            
            customersData = result.customers || [];
            
            updateStats(result.total, result.activeCount, result.inactiveCount);
            
            if (customersData.length === 0) {
                showEmptyState();
            } else {
                renderCustomerTable();
                showContent();
            }
            
            if (pagination) {
                pagination.update({
                    currentPage: result.page || page,
                    totalPages: result.totalPages || 1,
                    totalItems: result.total || 0,
                    pageSize: result.size || size
                });
            }

        } catch (error) {
            console.error('Error loading customers:', error);
            showErrorState('Failed to load customers. Please try again.');
        }
    }
    
    /**
     * Build search request object
     */
    function buildSearchRequest(page, size) {
        var request = {
            page: page,
            size: size
        };
        
        if (searchTerm) {
            request.search = searchTerm;
        }
        if (statusFilter) {
            request.status = statusFilter;
        }
        if (sortBy) {
            request.sortBy = sortBy;
        }
        if (sortDir) {
            request.sortDir = sortDir;
        }
        
        return request;
    }

    /**
     * Show loading state
     */
    function showLoadingState() {
        hideAllStates();
        var loadingState = document.getElementById('loadingState');
        if (loadingState) loadingState.classList.remove('hidden');
    }

    /**
     * Show error state
     */
    function showErrorState(message) {
        hideAllStates();
        var errorState = document.getElementById('errorState');
        var errorMessage = document.getElementById('errorMessage');
        if (errorState) errorState.classList.remove('hidden');
        if (errorMessage) errorMessage.textContent = message || 'Something went wrong. Please try again.';
    }

    /**
     * Show empty state
     */
    function showEmptyState() {
        hideAllStates();
        var emptyState = document.getElementById('emptyState');
        var emptyMessage = document.getElementById('emptyMessage');
        if (emptyState) emptyState.classList.remove('hidden');
        if (emptyMessage) {
            var hasFilters = searchTerm || statusFilter;
            emptyMessage.textContent = hasFilters 
                ? 'No customers match your filters. Try adjusting the filters.'
                : 'No customers in the system yet.';
        }
    }

    /**
     * Show content
     */
    function showContent() {
        hideAllStates();
        var content = document.getElementById('customersContent');
        if (content) content.classList.remove('hidden');
    }

    /**
     * Hide all states
     */
    function hideAllStates() {
        var states = ['loadingState', 'errorState', 'emptyState'];
        states.forEach(function(id) {
            var el = document.getElementById(id);
            if (el) el.classList.add('hidden');
        });
        var content = document.getElementById('customersContent');
        if (content) content.classList.add('hidden');
    }

    /**
     * Update stats cards
     */
    function updateStats(total, activeCount, inactiveCount) {
        document.getElementById('totalCustomers').textContent = total || 0;
        document.getElementById('activeCustomers').textContent = activeCount || 0;
        document.getElementById('inactiveCustomers').textContent = inactiveCount || 0;
    }

    /**
     * Render customer table
     */
    function renderCustomerTable() {
        var tbody = document.getElementById('customerTableBody');
        var canDelete = hasPermission(PERM_CUSTOMER_DELETE);
        
        if (customersData.length === 0) {
            var emptyMessage = (searchTerm || statusFilter) 
                ? 'No customers match your filters' 
                : 'No customers found';
            tbody.innerHTML = '<tr class="empty-state"><td colspan="5">' +
                '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' +
                '<p>' + emptyMessage + '</p></td></tr>';
            return;
        }

        var html = customersData.map(function (customer) {
            var initials = getInitials(customer.username);
            var joinedDate = formatDate(customer.createdAt);

            return '<tr>' +
                '<td>' +
                    '<div class="customer-info">' +
                        '<div class="customer-avatar">' + initials + '</div>' +
                        '<div class="customer-details">' +
                            '<span class="customer-name">' + escapeHtml(customer.username) + '</span>' +
                            '<span class="customer-email">' + escapeHtml(customer.email) + '</span>' +
                        '</div>' +
                    '</div>' +
                '</td>' +
                '<td>' + escapeHtml(customer.phone || 'N/A') + '</td>' +
                '<td>' + joinedDate + '</td>' +
                '<td>' +
                    '<span class="status-badge ' + (customer.isActive ? 'active' : 'inactive') + '">' +
                        '<span class="status-dot"></span>' +
                        (customer.isActive ? 'Active' : 'Inactive') +
                    '</span>' +
                '</td>' +
                '<td>' +
                    '<div class="actions-cell">' +
                        '<button class="btn-icon view" onclick="viewCustomer(\'' + customer.customerId + '\')" title="View">' +
                            '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' +
                        '</button>' +
                        (canDelete ? 
                            '<button class="btn-icon danger" onclick="confirmDelete(\'' + customer.customerId + '\', \'' + escapeHtml(customer.username) + '\')" title="Deactivate" ' + 
                                (!customer.isActive ? 'disabled' : '') + '>' +
                                '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>' +
                            '</button>' 
                        : '') +
                    '</div>' +
                '</td>' +
            '</tr>';
        }).join('');

        tbody.innerHTML = html;
    }

    /**
     * Clear all filters
     */
    window.clearFilters = function () {
        searchTerm = '';
        statusFilter = '';
        sortBy = '';
        sortDir = 'desc';

        document.getElementById('searchInput').value = '';
        document.getElementById('statusFilter').value = '';
        document.getElementById('sortByFilter').value = '';
        document.getElementById('sortDirFilter').value = 'desc';

        loadCustomers(1, pagination ? pagination.getPageSize() : 15);
    };

    /**
     * Confirm delete/deactivate
     */
    window.confirmDelete = function (customerId, username) {
        document.getElementById('deleteCustomerName').textContent = username;
        document.getElementById('deleteConfirmBtn').onclick = function () {
            deactivateCustomer(customerId);
        };
        document.getElementById('deleteModal').classList.add('active');
    };

    /**
     * Close delete modal
     */
    function closeDeleteModal() {
        document.getElementById('deleteModal').classList.remove('active');
    }

    /**
     * Deactivate customer
     */
    async function deactivateCustomer(customerId) {
        try {
            var response = await fetch('/JCart/admin/customers/' + customerId, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ _method: 'DELETE' }),
                credentials: 'include'
            });

            var data = await response.json();

            if (response.ok) {
                window.showToast('Customer deactivated successfully', 'success');
                closeDeleteModal();
                loadCustomers(pagination ? pagination.getCurrentPage() : 1, pagination ? pagination.getPageSize() : 15);
            } else {
                window.showToast(data.message || 'Failed to deactivate customer', 'error');
            }

        } catch (error) {
            console.error('Deactivate error:', error);
            window.showToast('Network error. Please try again.', 'error');
        }
    }

    /**
     * View customer details in modal
     */
    window.viewCustomer = function (customerId) {
        var customer = customersData.find(function (c) { return c.customerId === customerId; });
        if (!customer) return;

        var initials = getInitials(customer.username);
        var viewAvatar = document.getElementById('viewAvatar');
        viewAvatar.textContent = initials;

        document.getElementById('viewUsername').textContent = customer.username;
        document.getElementById('viewEmail').textContent = customer.email;
        document.getElementById('viewPhone').textContent = customer.phone || 'N/A';
        document.getElementById('viewCustomerId').textContent = customer.customerId;

        var viewStatusBadge = document.getElementById('viewStatusBadge');
        viewStatusBadge.className = 'status-badge ' + (customer.isActive ? 'active' : 'inactive');
        viewStatusBadge.innerHTML = '<span class="status-dot"></span>' + (customer.isActive ? 'Active' : 'Inactive');

        var viewStatus = document.getElementById('viewStatus');
        viewStatus.className = 'status-badge ' + (customer.isActive ? 'active' : 'inactive');
        viewStatus.innerHTML = '<span class="status-dot"></span>' + (customer.isActive ? 'Active' : 'Inactive');

        document.getElementById('viewCreatedAt').textContent = formatDate(customer.createdAt);
        document.getElementById('viewUpdatedAt').textContent = formatDate(customer.updatedAt);

        document.getElementById('viewModal').classList.add('active');
    };

    /**
     * Close view modal
     */
    function closeViewModal() {
        document.getElementById('viewModal').classList.remove('active');
    }

    function getInitials(name) {
        return name.substring(0, 2).toUpperCase();
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function formatDate(dateString) {
        if (!dateString) return 'N/A';
        var date = new Date(dateString);
        return date.toLocaleDateString('en-US', { 
            year: 'numeric', 
            month: 'short', 
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    function showTableError(message) {
        document.getElementById('customerTableBody').innerHTML = 
            '<tr class="empty-state"><td colspan="5">' + escapeHtml(message) + '</td></tr>';
    }

})();

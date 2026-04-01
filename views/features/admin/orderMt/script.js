/**
 * Order Management Handler
 * Manages order listing, filtering, pagination, and status updates.
 */

(function () {
    'use strict';

    var ordersData = [];
    var pagination = null;
    var currentPermissions = [];
    var updatingOrderId = null;
    var updatingOrderInvoice = null;

    var PERM_ORDER_VIEW = 'orders:view';
    var PERM_ORDER_UPDATE = 'orders:update';

    var searchTerm = '';
    var statusFilter = '';
    var paymentStatusFilter = '';
    var fromDate = '';
    var toDate = '';
    var minAmount = null;
    var maxAmount = null;
    var sortBy = 'created_at';
    var sortDir = 'DESC';

    document.addEventListener('DOMContentLoaded', function () {
        loadCurrentAdminPermissions();
        applyPermissionBasedUI();
        initializePagination();
        initializeEventListeners();
        loadOrders(1, 15);
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
            
            var profileName = document.getElementById('profileName');
            if (profileName && admin.name) {
                profileName.textContent = admin.name;
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
        if (!hasPermission(PERM_ORDER_VIEW)) {
            showPermissionError();
            return;
        }
    }

    /**
     * Show permission error and redirect
     */
    function showPermissionError() {
        window.showToast('You do not have permission to view orders', 'error');
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
                        loadOrders(page, size);
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

        var toggleFiltersBtn = document.getElementById('toggleFiltersBtn');
        if (toggleFiltersBtn) {
            toggleFiltersBtn.addEventListener('click', function () {
                var filtersPanel = document.getElementById('filtersPanel');
                if (filtersPanel) {
                    filtersPanel.classList.toggle('show');
                    toggleFiltersBtn.classList.toggle('active');
                }
            });
        }

        // View Modal Close
        var viewModalClose = document.getElementById('viewModalClose');
        var viewCloseBtn = document.getElementById('viewCloseBtn');
        if (viewModalClose) viewModalClose.addEventListener('click', closeViewModal);
        if (viewCloseBtn) viewCloseBtn.addEventListener('click', closeViewModal);

        // Status Modal
        var statusModalClose = document.getElementById('statusModalClose');
        var statusCancelBtn = document.getElementById('statusCancelBtn');
        var statusConfirmBtn = document.getElementById('statusConfirmBtn');
        if (statusModalClose) statusModalClose.addEventListener('click', closeStatusModal);
        if (statusCancelBtn) statusCancelBtn.addEventListener('click', closeStatusModal);
        if (statusConfirmBtn) statusConfirmBtn.addEventListener('click', confirmStatusUpdate);

        // Close modals on overlay click
        var viewModal = document.getElementById('viewModal');
        var statusModal = document.getElementById('statusModal');
        
        if (viewModal) {
            viewModal.addEventListener('click', function (e) {
                if (e.target === this) closeViewModal();
            });
        }
        if (statusModal) {
            statusModal.addEventListener('click', function (e) {
                if (e.target === this) closeStatusModal();
            });
        }

        // Retry button
        var retryBtn = document.getElementById('retryBtn');
        if (retryBtn) {
            retryBtn.addEventListener('click', function() {
                loadOrders(1, 15);
            });
        }

        // Menu toggle for mobile
        var menuToggle = document.getElementById('menuToggle');
        if (menuToggle) {
            menuToggle.addEventListener('click', function () {
                var sidebar = document.getElementById('sidebar');
                if (sidebar) {
                    sidebar.classList.toggle('open');
                }
            });
        }
    }

    /**
     * Apply filters and load orders
     */
    function applyFilters() {
        var searchInputEl = document.getElementById('searchInput');
        var statusFilterEl = document.getElementById('statusFilter');
        var paymentStatusFilterEl = document.getElementById('paymentStatusFilter');
        var fromDateEl = document.getElementById('fromDate');
        var toDateEl = document.getElementById('toDate');
        var minAmountEl = document.getElementById('minAmount');
        var maxAmountEl = document.getElementById('maxAmount');
        var sortByEl = document.getElementById('sortBy');
        var sortDirEl = document.getElementById('sortDir');

        searchTerm = searchInputEl ? searchInputEl.value.trim() : '';
        statusFilter = statusFilterEl ? statusFilterEl.value : '';
        paymentStatusFilter = paymentStatusFilterEl ? paymentStatusFilterEl.value : '';
        fromDate = fromDateEl ? fromDateEl.value : '';
        toDate = toDateEl ? toDateEl.value : '';
        minAmount = minAmountEl && minAmountEl.value ? parseFloat(minAmountEl.value) : null;
        maxAmount = maxAmountEl && maxAmountEl.value ? parseFloat(maxAmountEl.value) : null;
        sortBy = sortByEl ? sortByEl.value : 'created_at';
        sortDir = sortDirEl ? sortDirEl.value : 'DESC';

        updateFilterCount();
        loadOrders(1, pagination ? pagination.getPageSize() : 15);
    }

    /**
     * Update filter count badge
     */
    function updateFilterCount() {
        var count = 0;
        if (statusFilter) count++;
        if (paymentStatusFilter) count++;
        if (fromDate) count++;
        if (toDate) count++;
        if (minAmount !== null) count++;
        if (maxAmount !== null) count++;
        
        var filterCountEl = document.getElementById('filterCount');
        if (filterCountEl) {
            filterCountEl.textContent = count > 0 ? count : '';
            filterCountEl.style.display = count > 0 ? 'inline-flex' : 'none';
        }
    }

    /**
     * Build filter query parameters
     */
    function buildFilterData(page, size) {
        var data = {
            page: page,
            size: size,
            sortBy: sortBy,
            sortDir: sortDir
        };
        
        if (searchTerm) {
            data.keyword = searchTerm;
        }
        if (statusFilter) {
            data.status = statusFilter;
        }
        if (paymentStatusFilter) {
            data.paymentStatus = paymentStatusFilter;
        }
        if (fromDate) {
            data.fromDate = fromDate;
        }
        if (toDate) {
            data.toDate = toDate;
        }
        if (minAmount !== null && !isNaN(minAmount)) {
            data.minAmount = minAmount;
        }
        if (maxAmount !== null && !isNaN(maxAmount)) {
            data.maxAmount = maxAmount;
        }
        
        return data;
    }

    /**
     * Load orders from API with filters
     */
    async function loadOrders(page, size) {
        showLoadingState();
        
        try {
            var filterData = buildFilterData(page, size);
            var response = await fetch('/JCart/admin/orders/search', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(filterData)
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
                throw new Error('Failed to load orders');
            }

            var data = await response.json();
            var result = data.data || data;
            
            ordersData = result.orders || [];
            
            updateStats(
                result.total || 0,
                result.pending || 0,
                result.processing || 0,
                result.shipped || 0,
                result.delivered || 0
            );
            
            if (ordersData.length === 0) {
                showEmptyState();
            } else {
                renderOrderTable();
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
            console.error('Error loading orders:', error);
            showErrorState('Failed to load orders. Please try again.');
        }
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
        if (errorMessage) errorMessage.textContent = message || 'Failed to load orders. Please try again.';
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
            var hasFilters = searchTerm || statusFilter || paymentStatusFilter || 
                fromDate || toDate || minAmount !== null || maxAmount !== null;
            emptyMessage.textContent = hasFilters 
                ? 'No orders match your filters. Try adjusting the filters.'
                : 'No orders in the system yet.';
        }
    }

    /**
     * Show content
     */
    function showContent() {
        hideAllStates();
        var content = document.getElementById('ordersContent');
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
        var content = document.getElementById('ordersContent');
        if (content) content.classList.add('hidden');
    }

    /**
     * Update stats cards
     */
    function updateStats(total, pending, processing, shipped, delivered) {
        document.getElementById('totalOrders').textContent = total || 0;
        document.getElementById('pendingOrders').textContent = pending || 0;
        document.getElementById('processingOrders').textContent = processing || 0;
        document.getElementById('shippedOrders').textContent = shipped || 0;
        document.getElementById('deliveredOrders').textContent = delivered || 0;
    }

    /**
     * Render order table
     */
    function renderOrderTable() {
        var tbody = document.getElementById('orderTableBody');
        var canUpdate = hasPermission(PERM_ORDER_UPDATE);

        var html = ordersData.map(function (order) {
            var orderStatusClass = getOrderStatusClass(order.orderStatus);
            var paymentStatusClass = getPaymentStatusClass(order.paymentStatus);

            var actionButtons = '<button class="btn-icon view" onclick="viewOrder(' + order.orderId + ')" title="View Details">' +
                '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' +
            '</button>';

            if (canUpdate) {
                actionButtons += '<button class="btn-icon edit" onclick="openStatusModal(' + order.orderId + ', \'' + escapeHtml(order.invoiceNumber) + '\', \'' + escapeHtml(order.orderStatus) + '\')" title="Update Status">' +
                    '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>' +
                '</button>';
            }

            var customerName = 'Unknown';
            var customerEmail = '';
            
            if (order.customer && order.customer.customerId) {
                customerName = order.customer.name || ('ID: ' + order.customer.customerId);
                customerEmail = order.customer.email || '';
            }

            return '<tr>' +
                '<td>' +
                    '<div class="order-info">' +
                        '<div class="order-avatar">' +
                            '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>' +
                        '</div>' +
                        '<div class="order-details">' +
                            '<span class="order-invoice">' + escapeHtml(order.invoiceNumber || 'N/A') + '</span>' +
                            '<span class="order-id">#' + order.orderId + '</span>' +
                        '</div>' +
                    '</div>' +
                '</td>' +
                '<td>' +
                    '<div class="customer-cell">' +
                        '<span class="customer-name">' + escapeHtml(customerName) + '</span>' +
                        (customerEmail ? '<span class="customer-email">' + escapeHtml(customerEmail) + '</span>' : '') +
                    '</div>' +
                '</td>' +
                '<td><span class="date-value">' + formatDate(order.createdAt || order.invoiceDate) + '</span></td>' +
                '<td><span class="amount-value">₹' + formatPrice(order.totalAmount || order.total) + '</span></td>' +
                '<td><span class="payment-badge ' + paymentStatusClass + '">' + escapeHtml(order.paymentStatus || 'Unknown') + '</span></td>' +
                '<td><span class="status-badge ' + orderStatusClass + '"><span class="status-dot"></span>' + escapeHtml(order.orderStatus || 'Unknown') + '</span></td>' +
                '<td><div class="actions-cell">' + actionButtons + '</div></td>' +
            '</tr>';
        }).join('');

        tbody.innerHTML = html;
    }

    /**
     * Get order status CSS class
     */
    function getOrderStatusClass(status) {
        if (!status) return 'unknown';
        switch (status.toLowerCase()) {
            case 'pending': return 'pending';
            case 'processing': return 'processing';
            case 'shipped': return 'shipped';
            case 'delivered': return 'delivered';
            case 'cancelled': return 'cancelled';
            default: return 'unknown';
        }
    }

    /**
     * Get payment status CSS class
     */
    function getPaymentStatusClass(status) {
        if (!status) return 'unknown';
        switch (status.toLowerCase()) {
            case 'pending': return 'payment-pending';
            case 'paid': return 'payment-completed';
            case 'failed': return 'payment-failed';
            case 'refunding': return 'payment-refunding';
            case 'refunded': return 'payment-refunded';
            case 'rejected': return 'payment-rejected';
            default: return 'unknown';
        }
    }

    /**
     * Format price with 2 decimal places
     */
    function formatPrice(price) {
        if (price === null || price === undefined) return '0.00';
        return parseFloat(price).toFixed(2);
    }

    /**
     * Format date for display
     */
    function formatDate(dateStr) {
        if (!dateStr) return '--';
        try {
            var date = new Date(dateStr);
            return date.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric'
            });
        } catch (e) {
            return dateStr;
        }
    }

    /**
     * Format datetime for display
     */
    function formatDateTime(dateStr) {
        if (!dateStr) return '--';
        try {
            var date = new Date(dateStr);
            return date.toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            return dateStr;
        }
    }

    /**
     * Escape HTML to prevent XSS
     */
    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Clear all filters
     */
    window.clearFilters = function () {
        searchTerm = '';
        statusFilter = '';
        paymentStatusFilter = '';
        fromDate = '';
        toDate = '';
        minAmount = null;
        maxAmount = null;
        sortBy = 'created_at';
        sortDir = 'DESC';

        var searchInputEl = document.getElementById('searchInput');
        var statusFilterEl = document.getElementById('statusFilter');
        var paymentStatusFilterEl = document.getElementById('paymentStatusFilter');
        var fromDateEl = document.getElementById('fromDate');
        var toDateEl = document.getElementById('toDate');
        var minAmountEl = document.getElementById('minAmount');
        var maxAmountEl = document.getElementById('maxAmount');
        var sortByEl = document.getElementById('sortBy');
        var sortDirEl = document.getElementById('sortDir');

        if (searchInputEl) searchInputEl.value = '';
        if (statusFilterEl) statusFilterEl.value = '';
        if (paymentStatusFilterEl) paymentStatusFilterEl.value = '';
        if (fromDateEl) fromDateEl.value = '';
        if (toDateEl) toDateEl.value = '';
        if (minAmountEl) minAmountEl.value = '';
        if (maxAmountEl) maxAmountEl.value = '';
        if (sortByEl) sortByEl.value = 'created_at';
        if (sortDirEl) sortDirEl.value = 'DESC';

        updateFilterCount();
        loadOrders(1, pagination ? pagination.getPageSize() : 15);
    };

    /**
     * View order details
     */
    window.viewOrder = async function (orderId) {
        try {
            var response = await fetch('/JCart/admin/orders/' + orderId, {
                method: 'GET',
                credentials: 'include'
            });

            if (response.status === 401) {
                window.location.href = '/JCart/views/features/auth/admin/login/';
                return;
            }

            if (!response.ok) {
                throw new Error('Failed to load order details');
            }

            var data = await response.json();
            var order = data.data || data;

            populateViewModal(order);
            document.getElementById('viewModal').classList.add('active');

        } catch (error) {
            console.error('Error loading order:', error);
            window.showToast('Failed to load order details', 'error');
        }
    };

    /**
     * Populate view modal with order data
     */
    function populateViewModal(order) {
        document.getElementById('viewInvoiceNumber').textContent = order.invoiceNumber || 'N/A';
        document.getElementById('viewOrderDate').textContent = formatDateTime(order.invoiceDate);

        var orderStatusEl = document.getElementById('viewOrderStatus');
        orderStatusEl.textContent = order.orderStatus || 'Unknown';
        orderStatusEl.className = 'status-badge ' + getOrderStatusClass(order.orderStatus);

        var paymentStatusEl = document.getElementById('viewPaymentStatus');
        paymentStatusEl.textContent = order.paymentStatus || 'Unknown';
        paymentStatusEl.className = 'payment-badge ' + getPaymentStatusClass(order.paymentStatus);

        // Customer info
        var customer = order.customer || {};
        document.getElementById('viewCustomerName').textContent = customer.name || 'Unknown';
        document.getElementById('viewCustomerEmail').textContent = customer.email || '--';
        document.getElementById('viewCustomerId').textContent = customer.customerId ? 'ID: ' + customer.customerId : '';

        // Shipping address
        var shipping = order.shippingAddress || {};
        var addressHtml = '';
        if (shipping.name) addressHtml += '<strong>' + escapeHtml(shipping.name) + '</strong><br>';
        if (shipping.addressLine) addressHtml += escapeHtml(shipping.addressLine) + '<br>';
        if (shipping.city || shipping.state || shipping.postalCode) {
            addressHtml += escapeHtml([shipping.city, shipping.state, shipping.postalCode].filter(Boolean).join(', ')) + '<br>';
        }
        if (shipping.country) addressHtml += escapeHtml(shipping.country);
        document.getElementById('viewShippingAddress').innerHTML = addressHtml || '<p>No shipping address</p>';

        // Order items
        var items = order.items || [];
        var itemsHtml = '';
        if (items.length > 0) {
            itemsHtml = items.map(function (item) {
                return '<tr>' +
                    '<td>' +
                        '<div class="item-info">' +
                            '<span class="item-name">' + escapeHtml(item.productName || 'Unknown Product') + '</span>' +
                            '<span class="item-id">' + escapeHtml(item.productId || '') + '</span>' +
                        '</div>' +
                    '</td>' +
                    '<td>₹' + formatPrice(item.price || item.unitPrice) + '</td>' +
                    '<td>' + (item.quantity || 1) + '</td>' +
                    '<td>₹' + formatPrice(item.subtotal || (item.price * item.quantity)) + '</td>' +
                '</tr>';
            }).join('');
        } else {
            itemsHtml = '<tr><td colspan="4" class="no-items">No items in this order</td></tr>';
        }
        document.getElementById('viewOrderItems').innerHTML = itemsHtml;

        // Order summary
        document.getElementById('viewSubtotal').textContent = '₹' + formatPrice(order.subtotal);
        document.getElementById('viewTax').textContent = '₹' + formatPrice(order.tax);
        document.getElementById('viewShipping').textContent = '₹' + formatPrice(order.shipping);
        
        var discountRow = document.getElementById('discountRow');
        if (order.discount && parseFloat(order.discount) > 0) {
            document.getElementById('viewDiscount').textContent = '-₹' + formatPrice(order.discount);
            discountRow.style.display = 'flex';
        } else {
            discountRow.style.display = 'none';
        }
        
        document.getElementById('viewTotal').textContent = '₹' + formatPrice(order.total);

        // Transactions
        var payments = order.payments || [];
        var transactionsHtml = '';
        if (payments.length > 0) {
            transactionsHtml = payments.map(function (payment) {
                return '<div class="transaction-item">' +
                    '<div class="transaction-header">' +
                        '<span class="transaction-id">' + escapeHtml(payment.transactionId || payment.paymentId || 'N/A') + '</span>' +
                        '<span class="transaction-status ' + getPaymentStatusClass(payment.status) + '">' + escapeHtml(payment.status || 'Unknown') + '</span>' +
                    '</div>' +
                    '<div class="transaction-details">' +
                        '<span>Method: ' + escapeHtml(payment.method || payment.paymentMethod || 'N/A') + '</span>' +
                        '<span>Amount: ₹' + formatPrice(payment.amount) + '</span>' +
                        '<span>Date: ' + formatDateTime(payment.createdAt || payment.date) + '</span>' +
                    '</div>' +
                '</div>';
            }).join('');
        } else {
            transactionsHtml = '<p class="no-transactions">No transactions found</p>';
        }
        document.getElementById('viewTransactions').innerHTML = transactionsHtml;

        // Timestamps
        document.getElementById('viewCreatedAt').textContent = formatDateTime(order.invoiceDate);
        document.getElementById('viewUpdatedAt').textContent = formatDateTime(order.updatedAt) || '--';

        var paymentDeadlineItem = document.getElementById('paymentDeadlineItem');
        if (order.paymentDeadline) {
            document.getElementById('viewPaymentDeadline').textContent = formatDateTime(order.paymentDeadline);
            paymentDeadlineItem.style.display = 'flex';
        } else {
            paymentDeadlineItem.style.display = 'none';
        }
    }

    /**
     * Close view modal
     */
    function closeViewModal() {
        document.getElementById('viewModal').classList.remove('active');
    }

    /**
     * Order status transition rules
     */
    var ALLOWED_ORDER_STATUS_TRANSITIONS = {
        'PENDING': ['PROCESSING', 'CANCELLED'],
        'PROCESSING': ['SHIPPED', 'CANCELLED'],
        'SHIPPED': ['DELIVERED'],
        'DELIVERED': [],
        'CANCELLED': []
    };

    /**
     * Open status update modal
     */
    window.openStatusModal = function (orderId, invoiceNumber, currentStatus) {
        updatingOrderId = orderId;
        updatingOrderInvoice = invoiceNumber;

        document.getElementById('statusOrderInvoice').textContent = invoiceNumber;
        
        var currentStatusBadge = document.getElementById('currentStatusBadge');
        currentStatusBadge.textContent = currentStatus;
        currentStatusBadge.className = 'status-badge ' + getOrderStatusClass(currentStatus);

        var currentStatusUpper = currentStatus.toUpperCase();
        var allowedTransitions = ALLOWED_ORDER_STATUS_TRANSITIONS[currentStatusUpper] || [];
        
        var newStatusSelect = document.getElementById('newStatus');
        newStatusSelect.innerHTML = '';
        
        if (allowedTransitions.length === 0) {
            var option = document.createElement('option');
            option.value = currentStatusUpper;
            option.textContent = formatStatusDisplay(currentStatus);
            option.disabled = true;
            newStatusSelect.appendChild(option);
        } else {
            allowedTransitions.forEach(function(status) {
                var option = document.createElement('option');
                option.value = status;
                option.textContent = formatStatusDisplay(status);
                newStatusSelect.appendChild(option);
            });
            newStatusSelect.value = allowedTransitions[0];
        }

        document.getElementById('statusModal').classList.add('active');
    };

    /**
     * Format status for display (capitalize first letter)
     */
    function formatStatusDisplay(status) {
        return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
    }

    /**
     * Close status modal
     */
    function closeStatusModal() {
        document.getElementById('statusModal').classList.remove('active');
        updatingOrderId = null;
        updatingOrderInvoice = null;
    }

    /**
     * Confirm status update
     */
    async function confirmStatusUpdate() {
        if (!updatingOrderId) return;

        var newStatus = document.getElementById('newStatus').value;
        var confirmBtn = document.getElementById('statusConfirmBtn');
        
        confirmBtn.disabled = true;
        confirmBtn.innerHTML = '<span>Updating...</span>';

        try {
            var response = await fetch('/JCart/admin/orders/' + updatingOrderId + '/status', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    _method: 'PATCH',
                    status: newStatus
                }),
                credentials: 'include'
            });

            if (response.status === 401) {
                window.location.href = '/JCart/views/features/auth/admin/login/';
                return;
            }

            if (response.status === 403) {
                window.showToast('You do not have permission to update orders', 'error');
                return;
            }

            if (!response.ok) {
                var errorData = await response.json();
                throw new Error(errorData.message || 'Failed to update order status');
            }

            window.showToast('Order status updated successfully', 'success');
            closeStatusModal();
            loadOrders(pagination ? pagination.getCurrentPage() : 1, pagination ? pagination.getPageSize() : 15);

        } catch (error) {
            console.error('Error updating order status:', error);
            window.showToast(error.message || 'Failed to update order status', 'error');
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.innerHTML = '<span>Update Status</span>';
        }
    }

})();

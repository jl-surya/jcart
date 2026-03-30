/**
 * Customer Orders List Page Handler
 * Manages order history display with filters and pagination
 */

(function () {
    'use strict';

    var ORDER_API_BASE = '/JCart/customer/orders';

    var state = {
        orders: [],
        isLoading: false,
        filters: {
            status: '',
            fromDate: '',
            toDate: '',
            minAmount: '',
            maxAmount: '',
            sortBy: 'created_at',
            sortDir: 'DESC'
        },
        pagination: {
            currentPage: 1,
            totalPages: 1,
            totalItems: 0,
            pageSize: 15
        }
    };

    var elements = {};
    var paginationInstance = null;

    /**
     * Initialize the orders list page
     */
    function init() {
        if (!isLoggedIn()) {
            redirectToLogin();
            return;
        }

        cacheElements();
        setupEventListeners();
        loadNavbarAndFooter();
        initPagination();
        loadOrders();
    }

    /**
     * Check if user is logged in
     * @returns {boolean} - True if logged in
     */
    function isLoggedIn() {
        return localStorage.getItem('isLoggedIn') === 'true' && localStorage.getItem('user');
    }

    /**
     * Redirect to login page
     */
    function redirectToLogin() {
        if (window.showToast) {
            window.showToast('Please log in to view your orders', 'error');
        }
        setTimeout(function () {
            window.location.href = '/JCart/views/features/auth/customer/login/?returnUrl=' + 
                encodeURIComponent(window.location.pathname);
        }, 1500);
    }

    /**
     * Cache DOM elements
     */
    function cacheElements() {
        elements = {
            loadingState: document.getElementById('loadingState'),
            errorState: document.getElementById('errorState'),
            emptyState: document.getElementById('emptyState'),
            ordersContent: document.getElementById('ordersContent'),
            errorMessage: document.getElementById('errorMessage'),
            emptyMessage: document.getElementById('emptyMessage'),
            retryBtn: document.getElementById('retryBtn'),
            
            // Filters
            filterStatus: document.getElementById('filterStatus'),
            filterFromDate: document.getElementById('filterFromDate'),
            filterToDate: document.getElementById('filterToDate'),
            filterMinAmount: document.getElementById('filterMinAmount'),
            filterMaxAmount: document.getElementById('filterMaxAmount'),
            sortBy: document.getElementById('sortBy'),
            filtersContainer: document.querySelector('.filters-container'),
            mobileFilterToggle: document.getElementById('mobileFilterToggle'),
            clearFiltersBtn: document.getElementById('clearFiltersBtn'),
            applyFiltersBtn: document.getElementById('applyFiltersBtn'),
            
            // Orders
            ordersTableBody: document.getElementById('ordersTableBody'),
            ordersCards: document.getElementById('ordersCards')
        };
    }

    /**
     * Setup event listeners
     */
    function setupEventListeners() {
        if (elements.retryBtn) {
            elements.retryBtn.addEventListener('click', loadOrders);
        }

        if (elements.applyFiltersBtn) {
            elements.applyFiltersBtn.addEventListener('click', applyFilters);
        }

        if (elements.clearFiltersBtn) {
            elements.clearFiltersBtn.addEventListener('click', clearFilters);
        }

        if (elements.mobileFilterToggle) {
            elements.mobileFilterToggle.addEventListener('click', function () {
                elements.filtersContainer.classList.toggle('show');
            });
        }

        // Enter key on filter inputs
        var filterInputs = document.querySelectorAll('.filter-group input, .filter-group select');
        filterInputs.forEach(function (input) {
            input.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') {
                    applyFilters();
                }
            });
        });
    }

    /**
     * Load navbar and footer
     */
    function loadNavbarAndFooter() {
        var navbarContainer = document.getElementById('navbar');
        if (navbarContainer) {
            fetch('/JCart/views/shared/navbar/navbar.html')
                .then(function (response) { return response.text(); })
                .then(function (html) {
                    navbarContainer.innerHTML = html;
                    if (window.initializeNavbar) window.initializeNavbar();
                })
                .catch(function (error) {
                    console.error('Error loading navbar:', error);
                });
        }

        var footerContainer = document.getElementById('footer');
        if (footerContainer) {
            fetch('/JCart/views/shared/footer/footer.html')
                .then(function (response) { return response.text(); })
                .then(function (html) {
                    footerContainer.innerHTML = html;
                })
                .catch(function (error) {
                    console.error('Error loading footer:', error);
                });
        }
    }

    /**
     * Initialize pagination component
     */
    function initPagination() {
        if (typeof Pagination !== 'undefined') {
            paginationInstance = new Pagination({
                onPageChange: function (page, pageSize) {
                    state.pagination.currentPage = page;
                    state.pagination.pageSize = pageSize;
                    loadOrders();
                }
            });
            paginationInstance.init();
        }
    }

    /**
     * Apply filters and reload orders
     */
    function applyFilters() {
        state.filters.status = elements.filterStatus ? elements.filterStatus.value : '';
        state.filters.fromDate = elements.filterFromDate ? elements.filterFromDate.value : '';
        state.filters.toDate = elements.filterToDate ? elements.filterToDate.value : '';
        state.filters.minAmount = elements.filterMinAmount ? elements.filterMinAmount.value : '';
        state.filters.maxAmount = elements.filterMaxAmount ? elements.filterMaxAmount.value : '';

        var sortValue = elements.sortBy ? elements.sortBy.value : 'created_at';
        if (sortValue.endsWith('_asc')) {
            state.filters.sortBy = sortValue.replace('_asc', '');
            state.filters.sortDir = 'ASC';
        } else {
            state.filters.sortBy = sortValue;
            state.filters.sortDir = 'DESC';
        }

        state.pagination.currentPage = 1;
        loadOrders();

        // Hide mobile filters
        if (elements.filtersContainer) {
            elements.filtersContainer.classList.remove('show');
        }
    }

    /**
     * Clear all filters
     */
    function clearFilters() {
        if (elements.filterStatus) elements.filterStatus.value = '';
        if (elements.filterFromDate) elements.filterFromDate.value = '';
        if (elements.filterToDate) elements.filterToDate.value = '';
        if (elements.filterMinAmount) elements.filterMinAmount.value = '';
        if (elements.filterMaxAmount) elements.filterMaxAmount.value = '';
        if (elements.sortBy) elements.sortBy.value = 'created_at';

        state.filters = {
            status: '',
            fromDate: '',
            toDate: '',
            minAmount: '',
            maxAmount: '',
            sortBy: 'created_at',
            sortDir: 'DESC'
        };

        state.pagination.currentPage = 1;
        loadOrders();
    }

    /**
     * Load orders from API
     */
    function loadOrders() {
        if (state.isLoading) return;

        state.isLoading = true;
        showLoading();

        var params = new URLSearchParams();
        params.append('page', state.pagination.currentPage);
        params.append('size', state.pagination.pageSize);
        params.append('sortBy', state.filters.sortBy);
        params.append('sortDir', state.filters.sortDir);

        if (state.filters.status) params.append('status', state.filters.status);
        if (state.filters.fromDate) params.append('fromDate', state.filters.fromDate);
        if (state.filters.toDate) params.append('toDate', state.filters.toDate);
        if (state.filters.minAmount) params.append('minAmount', state.filters.minAmount);
        if (state.filters.maxAmount) params.append('maxAmount', state.filters.maxAmount);

        fetch(ORDER_API_BASE + '?' + params.toString(), {
            method: 'GET',
            credentials: 'include'
        })
        .then(function (response) {
            if (response.status === 401) {
                redirectToLogin();
                return;
            }
            if (!response.ok) {
                throw new Error('Failed to load orders');
            }
            return response.json();
        })
        .then(function (data) {
            var result = data.data || data;
            state.orders = result.orders || [];
            state.pagination.totalItems = result.total || 0;
            state.pagination.totalPages = result.totalPages || Math.ceil(state.pagination.totalItems / state.pagination.pageSize) || 1;
            state.pagination.currentPage = result.currentPage || state.pagination.currentPage;

            renderOrders();
        })
        .catch(function (error) {
            console.error('Error loading orders:', error);
            showError(error.message || 'Unable to load orders. Please try again.');
        })
        .finally(function () {
            state.isLoading = false;
        });
    }

    /**
     * Render orders list
     */
    function renderOrders() {
        hideAllStates();

        if (state.orders.length === 0) {
            var hasFilters = state.filters.status || state.filters.fromDate || state.filters.toDate ||
                            state.filters.minAmount || state.filters.maxAmount;
            if (elements.emptyMessage) {
                elements.emptyMessage.textContent = hasFilters 
                    ? 'No orders match your filters. Try adjusting the filters.'
                    : 'You haven\'t placed any orders yet. Start shopping to see your orders here.';
            }
            showEmpty();
            return;
        }

        renderOrdersTable();
        renderOrdersCards();
        updatePagination();
        showContent();
    }

    /**
     * Render orders in table format
     */
    function renderOrdersTable() {
        if (!elements.ordersTableBody) return;

        var html = '';
        state.orders.forEach(function (order) {
            html += renderOrderRow(order);
        });

        elements.ordersTableBody.innerHTML = html;
        attachOrderEventListeners();
    }

    /**
     * Render single order row
     */
    function renderOrderRow(order) {
        var orderId = order.orderId || order.invoiceNumber || 'N/A';
        var orderDate = formatDate(order.invoiceDate || order.createdAt);
        var items = order.items || [];
        var total = parseFloat(order.total || 0);
        var orderStatus = order.orderStatus || 'PENDING';
        var paymentStatus = order.paymentStatus || 'PENDING';
        var paymentMethod = order.payment ? order.payment.method : (order.paymentMethod || 'N/A');

        var firstItem = items.length > 0 ? items[0] : null;
        var itemsPreview = firstItem ? (firstItem.productName || 'Item') : 'No items';
        var moreItems = items.length > 1 ? ' +' + (items.length - 1) + ' more' : '';

        var canCancel = orderStatus === 'PENDING' || orderStatus === 'CONFIRMED' || orderStatus === 'PROCESSING';

        return '<tr>' +
            '<td><span class="order-id">#' + escapeHtml(orderId) + '</span></td>' +
            '<td><span class="order-date">' + orderDate + '</span></td>' +
            '<td>' +
                '<div class="order-items-preview">' +
                    '<span>' + escapeHtml(itemsPreview) + '</span>' +
                    (moreItems ? '<span class="order-items-count">' + moreItems + '</span>' : '') +
                '</div>' +
            '</td>' +
            '<td><span class="order-total">₹' + formatPrice(total) + '</span></td>' +
            '<td>' + renderStatusBadge(orderStatus) + '</td>' +
            '<td>' +
                '<div class="payment-status">' +
                    '<span class="payment-method">' + escapeHtml(paymentMethod) + '</span>' +
                    renderPaymentBadge(paymentStatus) +
                '</div>' +
            '</td>' +
            '<td>' +
                '<div class="order-actions">' +
                    '<a href="/JCart/views/features/orders/customer/detail/?id=' + encodeURIComponent(orderId) + '" class="btn-view">' +
                        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' +
                        'View' +
                    '</a>' +
                    (canCancel ? '<button type="button" class="btn-cancel" data-order-id="' + orderId + '">' +
                        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>' +
                        'Cancel' +
                    '</button>' : '') +
                '</div>' +
            '</td>' +
        '</tr>';
    }

    /**
     * Render orders in card format (mobile)
     */
    function renderOrdersCards() {
        if (!elements.ordersCards) return;

        var html = '';
        state.orders.forEach(function (order) {
            html += renderOrderCard(order);
        });

        elements.ordersCards.innerHTML = html;
        attachOrderEventListeners();
    }

    /**
     * Render single order card
     */
    function renderOrderCard(order) {
        var orderId = order.orderId || order.invoiceNumber || 'N/A';
        var orderDate = formatDate(order.invoiceDate || order.createdAt);
        var items = order.items || [];
        var total = parseFloat(order.total || 0);
        var orderStatus = order.orderStatus || 'PENDING';
        var paymentStatus = order.paymentStatus || 'PENDING';

        var canCancel = orderStatus === 'PENDING' || orderStatus === 'CONFIRMED' || orderStatus === 'PROCESSING';

        return '<div class="order-card">' +
            '<div class="order-card-header">' +
                '<span class="order-card-id">#' + escapeHtml(orderId) + '</span>' +
                '<span class="order-card-date">' + orderDate + '</span>' +
            '</div>' +
            '<div class="order-card-body">' +
                '<div class="order-card-row">' +
                    '<span class="order-card-label">Items</span>' +
                    '<span>' + items.length + ' item(s)</span>' +
                '</div>' +
                '<div class="order-card-row">' +
                    '<span class="order-card-label">Total</span>' +
                    '<span class="order-total">₹' + formatPrice(total) + '</span>' +
                '</div>' +
                '<div class="order-card-row">' +
                    '<span class="order-card-label">Status</span>' +
                    renderStatusBadge(orderStatus) +
                '</div>' +
                '<div class="order-card-row">' +
                    '<span class="order-card-label">Payment</span>' +
                    renderPaymentBadge(paymentStatus) +
                '</div>' +
            '</div>' +
            '<div class="order-card-footer">' +
                '<a href="/JCart/views/features/orders/customer/detail/?id=' + encodeURIComponent(orderId) + '" class="btn-view">' +
                    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' +
                    'View Details' +
                '</a>' +
                (canCancel ? '<button type="button" class="btn-cancel" data-order-id="' + orderId + '">Cancel</button>' : '') +
            '</div>' +
        '</div>';
    }

    /**
     * Render status badge
     */
    function renderStatusBadge(status) {
        var statusMap = {
            'PENDING': { class: 'status-pending', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>', label: 'Pending' },
            'CONFIRMED': { class: 'status-confirmed', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>', label: 'Confirmed' },
            'PROCESSING': { class: 'status-processing', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>', label: 'Processing' },
            'SHIPPED': { class: 'status-shipped', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>', label: 'Shipped' },
            'DELIVERED': { class: 'status-delivered', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>', label: 'Delivered' },
            'CANCELLED': { class: 'status-cancelled', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>', label: 'Cancelled' }
        };

        var statusInfo = statusMap[status] || statusMap['PENDING'];
        return '<span class="status-badge ' + statusInfo.class + '">' + statusInfo.icon + statusInfo.label + '</span>';
    }

    /**
     * Render payment status badge
     */
    function renderPaymentBadge(status) {
        var badgeClass = 'payment-pending';
        var label = status || 'Pending';

        if (status === 'PAID') {
            badgeClass = 'payment-paid';
            label = 'Paid';
        } else if (status === 'FAILED') {
            badgeClass = 'payment-failed';
            label = 'Failed';
        } else if (status === 'REFUNDING') {
            badgeClass = 'payment-pending';
            label = 'Refunding';
        } else if (status === 'REFUNDED') {
            badgeClass = 'payment-refunded';
            label = 'Refunded';
        } else if (status === 'REJECTED') {
            badgeClass = 'payment-failed';
            label = 'Rejected';
        } else if (status === 'PENDING') {
            badgeClass = 'payment-pending';
            label = 'Pending';
        }

        return '<span class="payment-badge ' + badgeClass + '">' + label + '</span>';
    }

    /**
     * Attach event listeners to order actions
     */
    function attachOrderEventListeners() {
        document.querySelectorAll('.btn-cancel').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var orderId = this.dataset.orderId;
                handleCancelOrder(orderId);
            });
        });
    }

    /**
     * Handle order cancellation
     */
    function handleCancelOrder(orderId) {
        if (!confirm('Are you sure you want to cancel this order?')) {
            return;
        }

        fetch(ORDER_API_BASE + '/' + orderId + '/cancel', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
        })
        .then(function (response) {
            if (response.status === 401) {
                redirectToLogin();
                return;
            }
            if (!response.ok) {
                return response.json().then(function (data) {
                    throw new Error(data.message || 'Failed to cancel order');
                });
            }
            return response.json();
        })
        .then(function () {
            if (window.showToast) {
                window.showToast('Order cancelled successfully', 'success');
            }
            loadOrders();
        })
        .catch(function (error) {
            console.error('Error cancelling order:', error);
            if (window.showToast) {
                window.showToast(error.message || 'Failed to cancel order', 'error');
            }
        });
    }

    /**
     * Update pagination
     */
    function updatePagination() {
        if (paginationInstance) {
            paginationInstance.options.actualItemsCount = state.orders.length;
            paginationInstance.update({
                currentPage: state.pagination.currentPage,
                totalPages: state.pagination.totalPages,
                totalItems: state.pagination.totalItems,
                pageSize: state.pagination.pageSize
            });
        }
    }

    /**
     * UI State helpers
     */
    function hideAllStates() {
        if (elements.loadingState) elements.loadingState.classList.add('hidden');
        if (elements.errorState) elements.errorState.classList.add('hidden');
        if (elements.emptyState) elements.emptyState.classList.add('hidden');
        if (elements.ordersContent) elements.ordersContent.classList.add('hidden');
    }

    function showLoading() {
        hideAllStates();
        if (elements.loadingState) elements.loadingState.classList.remove('hidden');
    }

    function showError(message) {
        hideAllStates();
        if (elements.errorMessage) elements.errorMessage.textContent = message;
        if (elements.errorState) elements.errorState.classList.remove('hidden');
    }

    function showEmpty() {
        hideAllStates();
        if (elements.emptyState) elements.emptyState.classList.remove('hidden');
    }

    function showContent() {
        hideAllStates();
        if (elements.ordersContent) elements.ordersContent.classList.remove('hidden');
    }

    /**
     * Format date string
     */
    function formatDate(dateStr) {
        if (!dateStr) return 'N/A';
        try {
            var date = new Date(dateStr);
            return date.toLocaleDateString('en-IN', {
                day: '2-digit',
                month: 'short',
                year: 'numeric'
            });
        } catch (e) {
            return dateStr;
        }
    }

    /**
     * Format price
     */
    function formatPrice(price) {
        return parseFloat(price || 0).toFixed(2);
    }

    /**
     * Escape HTML
     */
    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // Initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();

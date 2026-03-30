/**
 * Customer Transactions Page Handler
 * Manages transaction history display with filters, pagination, and detail modal
 */

(function () {
    'use strict';

    var TRANSACTION_API_BASE = '/JCart/customer/transactions';

    var state = {
        transactions: [],
        isLoading: false,
        selectedTransaction: null,
        filters: {
            type: '',
            status: '',
            fromDate: '',
            toDate: '',
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
     * Initialize the transactions page
     */
    function init() {
        if (!isLoggedIn()) {
            redirectToLogin();
            return;
        }

        cacheElements();
        setupEventListeners();
        loadNavbar();
        initPagination();
        loadTransactions();
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
            window.showToast('Please log in to view your transactions', 'error');
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
            transactionsContent: document.getElementById('transactionsContent'),
            errorMessage: document.getElementById('errorMessage'),
            emptyMessage: document.getElementById('emptyMessage'),
            retryBtn: document.getElementById('retryBtn'),
            
            // Filters
            filterType: document.getElementById('filterType'),
            filterStatus: document.getElementById('filterStatus'),
            filterFromDate: document.getElementById('filterFromDate'),
            filterToDate: document.getElementById('filterToDate'),
            sortBy: document.getElementById('sortBy'),
            filtersContainer: document.querySelector('.filters-container'),
            mobileFilterToggle: document.getElementById('mobileFilterToggle'),
            clearFiltersBtn: document.getElementById('clearFiltersBtn'),
            applyFiltersBtn: document.getElementById('applyFiltersBtn'),
            
            // Transactions
            transactionsTableBody: document.getElementById('transactionsTableBody'),
            transactionsCards: document.getElementById('transactionsCards'),
            
            // Modal
            transactionModal: document.getElementById('transactionModal'),
            modalBody: document.getElementById('modalBody'),
            closeModalBtn: document.getElementById('closeModalBtn'),
            closeModalFooterBtn: document.getElementById('closeModalFooterBtn'),
            viewOrderBtn: document.getElementById('viewOrderBtn')
        };
    }

    /**
     * Setup event listeners
     */
    function setupEventListeners() {
        if (elements.retryBtn) {
            elements.retryBtn.addEventListener('click', loadTransactions);
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

        // Modal event listeners
        if (elements.closeModalBtn) {
            elements.closeModalBtn.addEventListener('click', closeModal);
        }
        if (elements.closeModalFooterBtn) {
            elements.closeModalFooterBtn.addEventListener('click', closeModal);
        }
        if (elements.transactionModal) {
            elements.transactionModal.addEventListener('click', function (e) {
                if (e.target === elements.transactionModal) {
                    closeModal();
                }
            });
        }

        // Escape key to close modal
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !elements.transactionModal.classList.contains('hidden')) {
                closeModal();
            }
        });
    }

    /**
     * Load navbar
     */
    function loadNavbar() {
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
                    loadTransactions();
                }
            });
            paginationInstance.init();
        }
    }

    /**
     * Apply filters and reload transactions
     */
    function applyFilters() {
        state.filters.type = elements.filterType ? elements.filterType.value : '';
        state.filters.status = elements.filterStatus ? elements.filterStatus.value : '';
        state.filters.fromDate = elements.filterFromDate ? elements.filterFromDate.value : '';
        state.filters.toDate = elements.filterToDate ? elements.filterToDate.value : '';

        var sortValue = elements.sortBy ? elements.sortBy.value : 'created_at';
        if (sortValue.endsWith('_asc')) {
            state.filters.sortBy = sortValue.replace('_asc', '');
            state.filters.sortDir = 'ASC';
        } else {
            state.filters.sortBy = sortValue;
            state.filters.sortDir = 'DESC';
        }

        state.pagination.currentPage = 1;
        loadTransactions();

        // Hide mobile filters
        if (elements.filtersContainer) {
            elements.filtersContainer.classList.remove('show');
        }
    }

    /**
     * Clear all filters
     */
    function clearFilters() {
        if (elements.filterType) elements.filterType.value = '';
        if (elements.filterStatus) elements.filterStatus.value = '';
        if (elements.filterFromDate) elements.filterFromDate.value = '';
        if (elements.filterToDate) elements.filterToDate.value = '';
        if (elements.sortBy) elements.sortBy.value = 'created_at';

        state.filters = {
            type: '',
            status: '',
            fromDate: '',
            toDate: '',
            sortBy: 'created_at',
            sortDir: 'DESC'
        };

        state.pagination.currentPage = 1;
        loadTransactions();
    }

    /**
     * Load transactions from API
     */
    function loadTransactions() {
        if (state.isLoading) return;

        state.isLoading = true;
        showLoading();

        var params = new URLSearchParams();
        params.append('page', state.pagination.currentPage);
        params.append('size', state.pagination.pageSize);
        params.append('sortBy', state.filters.sortBy);
        params.append('sortDir', state.filters.sortDir);

        if (state.filters.type) params.append('type', state.filters.type);
        if (state.filters.status) params.append('status', state.filters.status);
        if (state.filters.fromDate) params.append('fromDate', state.filters.fromDate);
        if (state.filters.toDate) params.append('toDate', state.filters.toDate);

        fetch(TRANSACTION_API_BASE + '?' + params.toString(), {
            method: 'GET',
            credentials: 'include'
        })
        .then(function (response) {
            if (response.status === 401) {
                redirectToLogin();
                return;
            }
            if (!response.ok) {
                throw new Error('Failed to load transactions');
            }
            return response.json();
        })
        .then(function (data) {
            var result = data.data || data;
            state.transactions = result.transactions || [];
            state.pagination.totalItems = result.total || 0;
            state.pagination.totalPages = result.totalPages || Math.ceil(state.pagination.totalItems / state.pagination.pageSize) || 1;
            state.pagination.currentPage = result.currentPage || state.pagination.currentPage;

            renderTransactions();
        })
        .catch(function (error) {
            console.error('Error loading transactions:', error);
            showError(error.message || 'Unable to load transactions. Please try again.');
        })
        .finally(function () {
            state.isLoading = false;
        });
    }

    /**
     * Render transactions list
     */
    function renderTransactions() {
        hideAllStates();

        if (state.transactions.length === 0) {
            var hasFilters = state.filters.type || state.filters.status || 
                            state.filters.fromDate || state.filters.toDate;
            if (elements.emptyMessage) {
                elements.emptyMessage.textContent = hasFilters 
                    ? 'No transactions match your filters. Try adjusting the filters.'
                    : 'You don\'t have any transactions yet. Place an order to see your transactions here.';
            }
            showEmpty();
            return;
        }

        renderTransactionsTable();
        renderTransactionsCards();
        updatePagination();
        showContent();
    }

    /**
     * Render transactions in table format
     */
    function renderTransactionsTable() {
        if (!elements.transactionsTableBody) return;

        var html = '';
        state.transactions.forEach(function (txn) {
            html += renderTransactionRow(txn);
        });

        elements.transactionsTableBody.innerHTML = html;
        attachTransactionEventListeners();
    }

    /**
     * Render single transaction row
     */
    function renderTransactionRow(txn) {
        var txnId = txn.transactionId || 'N/A';
        var orderId = txn.orderId || 'N/A';
        var type = txn.transactionType || 'PAYMENT';
        var method = txn.transactionMethod || 'N/A';
        var amount = parseFloat(txn.amount || 0);
        var status = txn.transactionStatus || 'PENDING';
        var date = formatDate(txn.createdAt);
        var reference = txn.transactionReference || '';

        var isRefund = type === 'REFUND';

        return '<tr>' +
            '<td><span class="txn-id">' + escapeHtml(reference || '#' + txnId) + '</span></td>' +
            '<td><a href="/JCart/views/features/orders/customer/detail/?id=' + encodeURIComponent(orderId) + '" class="order-link">#' + escapeHtml(orderId) + '</a></td>' +
            '<td>' + renderTypeBadge(type) + '</td>' +
            '<td>' + renderPaymentMethod(method) + '</td>' +
            '<td><span class="txn-amount ' + (isRefund ? 'credit' : 'debit') + '">' + (isRefund ? '+' : '') + '₹' + formatPrice(amount) + '</span></td>' +
            '<td>' + renderStatusBadge(status) + '</td>' +
            '<td><span class="txn-date">' + date + '</span></td>' +
            '<td>' +
                '<div class="txn-actions">' +
                    '<button type="button" class="btn-view" data-txn-id="' + txnId + '">' +
                        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' +
                        'View' +
                    '</button>' +
                '</div>' +
            '</td>' +
        '</tr>';
    }

    /**
     * Render transactions in card format (mobile)
     */
    function renderTransactionsCards() {
        if (!elements.transactionsCards) return;

        var html = '';
        state.transactions.forEach(function (txn) {
            html += renderTransactionCard(txn);
        });

        elements.transactionsCards.innerHTML = html;
        attachTransactionEventListeners();
    }

    /**
     * Render single transaction card
     */
    function renderTransactionCard(txn) {
        var txnId = txn.transactionId || 'N/A';
        var orderId = txn.orderId || 'N/A';
        var type = txn.transactionType || 'PAYMENT';
        var method = txn.transactionMethod || 'N/A';
        var amount = parseFloat(txn.amount || 0);
        var status = txn.transactionStatus || 'PENDING';
        var date = formatDate(txn.createdAt);
        var reference = txn.transactionReference || '';

        var isRefund = type === 'REFUND';

        return '<div class="txn-card">' +
            '<div class="txn-card-header">' +
                '<span class="txn-card-id">' + escapeHtml(reference || '#' + txnId) + '</span>' +
                '<span class="txn-card-date">' + date + '</span>' +
            '</div>' +
            '<div class="txn-card-body">' +
                '<div class="txn-card-row">' +
                    '<span class="txn-card-label">Order</span>' +
                    '<a href="/JCart/views/features/orders/customer/detail/?id=' + encodeURIComponent(orderId) + '" class="order-link">#' + escapeHtml(orderId) + '</a>' +
                '</div>' +
                '<div class="txn-card-row">' +
                    '<span class="txn-card-label">Type</span>' +
                    renderTypeBadge(type) +
                '</div>' +
                '<div class="txn-card-row">' +
                    '<span class="txn-card-label">Amount</span>' +
                    '<span class="txn-amount ' + (isRefund ? 'credit' : 'debit') + '">' + (isRefund ? '+' : '') + '₹' + formatPrice(amount) + '</span>' +
                '</div>' +
                '<div class="txn-card-row">' +
                    '<span class="txn-card-label">Status</span>' +
                    renderStatusBadge(status) +
                '</div>' +
            '</div>' +
            '<div class="txn-card-footer">' +
                '<button type="button" class="btn-view" data-txn-id="' + txnId + '">' +
                    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' +
                    'View Details' +
                '</button>' +
            '</div>' +
        '</div>';
    }

    /**
     * Render type badge
     */
    function renderTypeBadge(type) {
        var typeClass = type === 'REFUND' ? 'type-refund' : 'type-payment';
        var icon = type === 'REFUND' 
            ? '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>'
            : '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>';
        var label = type === 'REFUND' ? 'Refund' : 'Payment';

        return '<span class="type-badge ' + typeClass + '">' + icon + label + '</span>';
    }

    /**
     * Render payment method
     */
    function renderPaymentMethod(method) {
        var icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>';
        
        if (method === 'UPI') {
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>';
        } else if (method === 'CARD') {
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>';
        } else if (method === 'NETBANKING') {
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>';
        }

        var methodLabel = method;
        if (method === 'UPI') methodLabel = 'UPI';
        else if (method === 'CARD') methodLabel = 'Card';
        else if (method === 'NETBANKING') methodLabel = 'Net Banking';

        return '<span class="payment-method">' + icon + escapeHtml(methodLabel) + '</span>';
    }

    /**
     * Render status badge
     */
    function renderStatusBadge(status) {
        var statusMap = {
            'PENDING': { class: 'status-pending', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>', label: 'Pending' },
            'PAID': { class: 'status-paid', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>', label: 'Paid' },
            'FAILED': { class: 'status-failed', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>', label: 'Failed' },
            'REFUNDED': { class: 'status-refunded', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/></svg>', label: 'Refunded' },
            'REJECTED': { class: 'status-rejected', icon: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>', label: 'Rejected' }
        };

        var statusInfo = statusMap[status] || statusMap['PENDING'];
        return '<span class="status-badge ' + statusInfo.class + '">' + statusInfo.icon + statusInfo.label + '</span>';
    }

    /**
     * Attach event listeners to transaction actions
     */
    function attachTransactionEventListeners() {
        document.querySelectorAll('.btn-view[data-txn-id]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var txnId = this.dataset.txnId;
                viewTransactionDetails(txnId);
            });
        });
    }

    /**
     * View transaction details in modal
     */
    function viewTransactionDetails(txnId) {
        var txn = state.transactions.find(function (t) {
            return String(t.transactionId) === String(txnId);
        });

        if (!txn) {
            if (window.showToast) {
                window.showToast('Transaction not found', 'error');
            }
            return;
        }

        state.selectedTransaction = txn;
        renderTransactionModal(txn);
        openModal();
    }

    /**
     * Render transaction details in modal
     */
    function renderTransactionModal(txn) {
        var type = txn.transactionType || 'PAYMENT';
        var isRefund = type === 'REFUND';
        var amount = parseFloat(txn.amount || 0);

        var html = '';

        // Transaction Info Section
        html += '<div class="detail-section">';
        html += '<h3 class="detail-section-title">Transaction Information</h3>';
        html += '<div class="detail-grid">';
        html += '<div class="detail-item">';
        html += '<span class="detail-label">Transaction ID</span>';
        html += '<span class="detail-value highlight">' + escapeHtml(txn.transactionReference || '#' + txn.transactionId) + '</span>';
        html += '</div>';
        html += '<div class="detail-item">';
        html += '<span class="detail-label">Order ID</span>';
        html += '<span class="detail-value">#' + escapeHtml(txn.orderId || 'N/A') + '</span>';
        html += '</div>';
        html += '<div class="detail-item">';
        html += '<span class="detail-label">Type</span>';
        html += '<span class="detail-value">' + renderTypeBadge(type) + '</span>';
        html += '</div>';
        html += '<div class="detail-item">';
        html += '<span class="detail-label">Status</span>';
        html += '<span class="detail-value">' + renderStatusBadge(txn.transactionStatus || 'PENDING') + '</span>';
        html += '</div>';
        html += '<div class="detail-item full-width">';
        html += '<span class="detail-label">Amount</span>';
        html += '<span class="detail-value amount ' + (isRefund ? 'credit' : 'debit') + '">' + (isRefund ? '+' : '') + '₹' + formatPrice(amount) + '</span>';
        html += '</div>';
        html += '</div>';
        html += '</div>';

        // Payment Method Section
        html += '<div class="detail-section">';
        html += '<h3 class="detail-section-title">Payment Method</h3>';
        html += '<div class="detail-grid">';
        html += '<div class="detail-item">';
        html += '<span class="detail-label">Method</span>';
        html += '<span class="detail-value">' + renderPaymentMethod(txn.transactionMethod || 'N/A') + '</span>';
        html += '</div>';
        html += '<div class="detail-item">';
        html += '<span class="detail-label">Processed By</span>';
        html += '<span class="detail-value">' + escapeHtml(txn.processedByType || 'System') + '</span>';
        html += '</div>';
        html += '</div>';
        html += '</div>';

        // Refund Reason (if applicable)
        if (isRefund && txn.refundReason) {
            html += '<div class="detail-section">';
            html += '<h3 class="detail-section-title">Refund Reason</h3>';
            html += '<div class="detail-grid">';
            html += '<div class="detail-item full-width">';
            html += '<span class="detail-value">' + escapeHtml(txn.refundReason) + '</span>';
            html += '</div>';
            html += '</div>';
            html += '</div>';
        }

        // Timeline Section
        html += '<div class="detail-section">';
        html += '<h3 class="detail-section-title">Timeline</h3>';
        html += '<div class="timeline">';
        
        // Created
        html += '<div class="timeline-item active">';
        html += '<div class="timeline-dot"></div>';
        html += '<div class="timeline-content">';
        html += '<span class="timeline-label">Transaction Created</span>';
        html += '<span class="timeline-date">' + formatDateTime(txn.createdAt) + '</span>';
        html += '</div>';
        html += '</div>';

        // Processed
        if (txn.processedAt) {
            html += '<div class="timeline-item active">';
            html += '<div class="timeline-dot"></div>';
            html += '<div class="timeline-content">';
            html += '<span class="timeline-label">Transaction Processed</span>';
            html += '<span class="timeline-date">' + formatDateTime(txn.processedAt) + '</span>';
            html += '</div>';
            html += '</div>';
        }

        // Verified (for refunds)
        if (txn.verifiedAt) {
            var verifiedStatus = txn.transactionStatus === 'REFUNDED' ? 'Approved' : 'Verified';
            if (txn.transactionStatus === 'REJECTED') {
                verifiedStatus = 'Rejected';
            }
            html += '<div class="timeline-item active">';
            html += '<div class="timeline-dot"></div>';
            html += '<div class="timeline-content">';
            html += '<span class="timeline-label">' + verifiedStatus + '</span>';
            html += '<span class="timeline-date">' + formatDateTime(txn.verifiedAt) + '</span>';
            html += '</div>';
            html += '</div>';
        }

        html += '</div>';
        html += '</div>';

        elements.modalBody.innerHTML = html;

        // Update View Order button
        if (elements.viewOrderBtn) {
            elements.viewOrderBtn.href = '/JCart/views/features/orders/customer/detail/?id=' + encodeURIComponent(txn.orderId || '');
        }
    }

    /**
     * Open modal
     */
    function openModal() {
        if (elements.transactionModal) {
            elements.transactionModal.classList.remove('hidden');
            document.body.style.overflow = 'hidden';
        }
    }

    /**
     * Close modal
     */
    function closeModal() {
        if (elements.transactionModal) {
            elements.transactionModal.classList.add('hidden');
            document.body.style.overflow = '';
            state.selectedTransaction = null;
        }
    }

    /**
     * Update pagination
     */
    function updatePagination() {
        if (paginationInstance) {
            paginationInstance.options.actualItemsCount = state.transactions.length;
            paginationInstance.update({
                currentPage: state.pagination.currentPage,
                totalPages: state.pagination.totalPages,
                totalItems: state.pagination.totalItems,
                pageSize: state.pagination.pageSize
            });
        }
    }

    /**
     * Show loading state
     */
    function showLoading() {
        hideAllStates();
        if (elements.loadingState) elements.loadingState.classList.remove('hidden');
    }

    /**
     * Show error state
     */
    function showError(message) {
        hideAllStates();
        if (elements.errorMessage) elements.errorMessage.textContent = message;
        if (elements.errorState) elements.errorState.classList.remove('hidden');
    }

    /**
     * Show empty state
     */
    function showEmpty() {
        hideAllStates();
        if (elements.emptyState) elements.emptyState.classList.remove('hidden');
    }

    /**
     * Show content
     */
    function showContent() {
        hideAllStates();
        if (elements.transactionsContent) elements.transactionsContent.classList.remove('hidden');
    }

    /**
     * Hide all states
     */
    function hideAllStates() {
        if (elements.loadingState) elements.loadingState.classList.add('hidden');
        if (elements.errorState) elements.errorState.classList.add('hidden');
        if (elements.emptyState) elements.emptyState.classList.add('hidden');
        if (elements.transactionsContent) elements.transactionsContent.classList.add('hidden');
    }

    /**
     * Format date
     */
    function formatDate(dateStr) {
        if (!dateStr) return 'N/A';
        var date = new Date(dateStr);
        if (isNaN(date.getTime())) return 'N/A';
        
        var options = { year: 'numeric', month: 'short', day: 'numeric' };
        return date.toLocaleDateString('en-IN', options);
    }

    /**
     * Format date and time
     */
    function formatDateTime(dateStr) {
        if (!dateStr) return 'N/A';
        var date = new Date(dateStr);
        if (isNaN(date.getTime())) return 'N/A';
        
        var options = { 
            year: 'numeric', 
            month: 'short', 
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        };
        return date.toLocaleDateString('en-IN', options);
    }

    /**
     * Format price
     */
    function formatPrice(price) {
        return parseFloat(price).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
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

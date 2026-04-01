/**
 * Transaction Management Handler
 * Manages transaction listing, filtering, pagination, and refund actions.
 */

(function () {
    'use strict';

    var transactionsData = [];
    var pagination = null;
    var currentPermissions = [];
    var processingTransactionId = null;
    var processingTransactionRef = null;
    var processingTransactionAmount = null;
    var processingRefundReason = null;

    var PERM_TRANSACTION_VIEW = 'transactions:view';
    var PERM_TRANSACTION_UPDATE = 'transactions:update';

    var searchTerm = '';
    var typeFilter = '';
    var statusFilter = '';
    var methodFilter = '';
    var orderIdFilter = null;
    var fromDate = '';
    var toDate = '';
    var sortBy = 'created_at';
    var sortDir = 'DESC';

    document.addEventListener('DOMContentLoaded', function () {
        loadCurrentAdminPermissions();
        applyPermissionBasedUI();
        initializePagination();
        initializeEventListeners();
        loadTransactions(1, 15);
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
        if (!hasPermission(PERM_TRANSACTION_VIEW)) {
            showPermissionError();
            return;
        }
    }

    /**
     * Show permission error and redirect
     */
    function showPermissionError() {
        window.showToast('You do not have permission to view transactions', 'error');
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
                        loadTransactions(page, size);
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

        // Refund Modal
        var refundModalClose = document.getElementById('refundModalClose');
        var refundCancelBtn = document.getElementById('refundCancelBtn');
        var approveRefundBtn = document.getElementById('approveRefundBtn');
        var rejectRefundBtn = document.getElementById('rejectRefundBtn');
        
        if (refundModalClose) refundModalClose.addEventListener('click', closeRefundModal);
        if (refundCancelBtn) refundCancelBtn.addEventListener('click', closeRefundModal);
        if (approveRefundBtn) approveRefundBtn.addEventListener('click', function() { processRefundAction('APPROVE'); });
        if (rejectRefundBtn) rejectRefundBtn.addEventListener('click', function() { processRefundAction('REJECT'); });

        // Close modals on overlay click
        var viewModal = document.getElementById('viewModal');
        var refundModal = document.getElementById('refundModal');
        
        if (viewModal) {
            viewModal.addEventListener('click', function (e) {
                if (e.target === this) closeViewModal();
            });
        }
        if (refundModal) {
            refundModal.addEventListener('click', function (e) {
                if (e.target === this) closeRefundModal();
            });
        }

        // Retry button
        var retryBtn = document.getElementById('retryBtn');
        if (retryBtn) {
            retryBtn.addEventListener('click', function() {
                loadTransactions(1, 15);
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
     * Apply filters and load transactions
     */
    function applyFilters() {
        var searchInputEl = document.getElementById('searchInput');
        var typeFilterEl = document.getElementById('typeFilter');
        var statusFilterEl = document.getElementById('statusFilter');
        var methodFilterEl = document.getElementById('methodFilter');
        var orderIdFilterEl = document.getElementById('orderIdFilter');
        var fromDateEl = document.getElementById('fromDate');
        var toDateEl = document.getElementById('toDate');
        var sortByEl = document.getElementById('sortBy');
        var sortDirEl = document.getElementById('sortDir');

        searchTerm = searchInputEl ? searchInputEl.value.trim() : '';
        typeFilter = typeFilterEl ? typeFilterEl.value : '';
        statusFilter = statusFilterEl ? statusFilterEl.value : '';
        methodFilter = methodFilterEl ? methodFilterEl.value : '';
        orderIdFilter = orderIdFilterEl && orderIdFilterEl.value ? parseInt(orderIdFilterEl.value) : null;
        fromDate = fromDateEl ? fromDateEl.value : '';
        toDate = toDateEl ? toDateEl.value : '';
        sortBy = sortByEl ? sortByEl.value : 'created_at';
        sortDir = sortDirEl ? sortDirEl.value : 'DESC';

        updateFilterCount();
        loadTransactions(1, pagination ? pagination.getPageSize() : 15);
    }

    /**
     * Update filter count badge
     */
    function updateFilterCount() {
        var count = 0;
        if (typeFilter) count++;
        if (statusFilter) count++;
        if (methodFilter) count++;
        if (orderIdFilter) count++;
        if (fromDate) count++;
        if (toDate) count++;
        
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
        if (typeFilter) {
            data.type = typeFilter;
        }
        if (statusFilter) {
            data.status = statusFilter;
        }
        if (methodFilter) {
            data.paymentMethod = methodFilter;
        }
        if (orderIdFilter) {
            data.orderId = orderIdFilter;
        }
        if (fromDate) {
            data.fromDate = fromDate;
        }
        if (toDate) {
            data.toDate = toDate;
        }
        
        return data;
    }

    /**
     * Load transactions from API with filters
     */
    async function loadTransactions(page, size) {
        showLoadingState();
        
        try {
            var filterData = buildFilterData(page, size);
            var response = await fetch('/JCart/admin/transactions/search', {
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
                throw new Error('Failed to load transactions');
            }

            var data = await response.json();
            var result = data.data || data;
            
            transactionsData = result.transactions || [];
            
            updateStats(
                result.total || 0,
                result.payments || 0,
                result.refunds || 0,
                result.pendingRefunds || 0
            );
            
            if (transactionsData.length === 0) {
                showEmptyState();
            } else {
                renderTransactionTable();
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
            console.error('Error loading transactions:', error);
            showErrorState('Failed to load transactions. Please try again.');
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
        if (errorMessage) errorMessage.textContent = message || 'Failed to load transactions. Please try again.';
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
            var hasFilters = searchTerm || typeFilter || statusFilter || methodFilter ||
                orderIdFilter || fromDate || toDate;
            emptyMessage.textContent = hasFilters 
                ? 'No transactions match your filters. Try adjusting the filters.'
                : 'No transactions in the system yet.';
        }
    }

    /**
     * Show content
     */
    function showContent() {
        hideAllStates();
        var content = document.getElementById('transactionsContent');
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
        var content = document.getElementById('transactionsContent');
        if (content) content.classList.add('hidden');
    }

    /**
     * Update stats cards
     */
    function updateStats(total, payments, refunds, pendingRefunds) {
        document.getElementById('totalTransactions').textContent = total || 0;
        document.getElementById('totalPayments').textContent = payments || 0;
        document.getElementById('totalRefunds').textContent = refunds || 0;
        document.getElementById('pendingRefunds').textContent = pendingRefunds || 0;
    }

    /**
     * Render transaction table
     */
    function renderTransactionTable() {
        var tbody = document.getElementById('transactionTableBody');
        var canUpdate = hasPermission(PERM_TRANSACTION_UPDATE);

        var html = transactionsData.map(function (transaction) {
            var typeClass = getTypeClass(transaction.transactionType);
            var statusClass = getStatusClass(transaction.transactionStatus);
            var isPendingRefund = transaction.transactionType === 'REFUND' && 
                                  transaction.transactionStatus.toUpperCase() === 'PENDING';

            var actionButtons = '<button class="btn-icon view" onclick="viewTransaction(' + transaction.transactionId + ')" title="View Details">' +
                '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' +
            '</button>';

            if (canUpdate && isPendingRefund) {
                actionButtons += '<button class="btn-icon refund" onclick="openRefundModal(' + transaction.transactionId + ', \'' + escapeHtml(transaction.transactionReference || '') + '\', ' + (transaction.amount || 0) + ', \'' + escapeHtml(transaction.refundReason || '') + '\')" title="Process Refund">' +
                    '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>' +
                '</button>';
            }

            return '<tr class="' + (isPendingRefund ? 'pending-refund-row' : '') + '">' +
                '<td>' +
                    '<div class="transaction-info">' +
                        '<div class="transaction-avatar ' + typeClass + '">' +
                            (transaction.transactionType === 'REFUND' 
                                ? '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>'
                                : '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2" ry="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>') +
                        '</div>' +
                        '<div class="transaction-details">' +
                            '<span class="transaction-ref">' + escapeHtml(transaction.transactionReference || 'N/A') + '</span>' +
                            '<span class="transaction-id">#' + transaction.transactionId + '</span>' +
                        '</div>' +
                    '</div>' +
                '</td>' +
                '<td><span class="order-link" onclick="viewOrder(' + transaction.orderId + ')">#' + transaction.orderId + '</span></td>' +
                '<td><span class="type-badge ' + typeClass + '">' + escapeHtml(transaction.transactionType || 'Unknown') + '</span></td>' +
                '<td><span class="amount-value ' + (transaction.transactionType === 'REFUND' ? 'refund-amount' : '') + '">' + (transaction.transactionType === 'REFUND' ? '-' : '') + '₹' + formatPrice(transaction.amount) + '</span></td>' +
                '<td><span class="method-value">' + escapeHtml(transaction.transactionMethod || 'N/A') + '</span></td>' +
                '<td><span class="status-badge ' + statusClass + '"><span class="status-dot"></span>' + escapeHtml(transaction.transactionStatus || 'Unknown') + '</span></td>' +
                '<td><div class="actions-cell">' + actionButtons + '</div></td>' +
            '</tr>';
        }).join('');

        tbody.innerHTML = html;
    }

    /**
     * Get transaction type CSS class
     */
    function getTypeClass(type) {
        if (!type) return 'unknown';
        switch (type.toUpperCase()) {
            case 'PAYMENT': return 'type-payment';
            case 'REFUND': return 'type-refund';
            default: return 'unknown';
        }
    }

    /**
     * Get status CSS class
     */
    function getStatusClass(status) {
        if (!status) return 'unknown';
        switch (status.toLowerCase()) {
            case 'pending': return 'status-pending';
            case 'paid': return 'status-completed';
            case 'failed': return 'status-failed';
            case 'refunded': return 'status-approved';
            case 'rejected': return 'status-rejected';
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
        typeFilter = '';
        statusFilter = '';
        methodFilter = '';
        orderIdFilter = null;
        fromDate = '';
        toDate = '';
        sortBy = 'created_at';
        sortDir = 'DESC';

        var searchInputEl = document.getElementById('searchInput');
        var typeFilterEl = document.getElementById('typeFilter');
        var statusFilterEl = document.getElementById('statusFilter');
        var methodFilterEl = document.getElementById('methodFilter');
        var orderIdFilterEl = document.getElementById('orderIdFilter');
        var fromDateEl = document.getElementById('fromDate');
        var toDateEl = document.getElementById('toDate');
        var sortByEl = document.getElementById('sortBy');
        var sortDirEl = document.getElementById('sortDir');

        if (searchInputEl) searchInputEl.value = '';
        if (typeFilterEl) typeFilterEl.value = '';
        if (statusFilterEl) statusFilterEl.value = '';
        if (methodFilterEl) methodFilterEl.value = '';
        if (orderIdFilterEl) orderIdFilterEl.value = '';
        if (fromDateEl) fromDateEl.value = '';
        if (toDateEl) toDateEl.value = '';
        if (sortByEl) sortByEl.value = 'created_at';
        if (sortDirEl) sortDirEl.value = 'DESC';

        updateFilterCount();
        loadTransactions(1, pagination ? pagination.getPageSize() : 15);
    };

    /**
     * View transaction details
     */
    window.viewTransaction = async function (transactionId) {
        try {
            var response = await fetch('/JCart/admin/transactions/' + transactionId, {
                method: 'GET',
                credentials: 'include'
            });

            if (response.status === 401) {
                window.location.href = '/JCart/views/features/auth/admin/login/';
                return;
            }

            if (!response.ok) {
                throw new Error('Failed to load transaction details');
            }

            var data = await response.json();
            var transaction = data.data || data;

            populateViewModal(transaction);
            document.getElementById('viewModal').classList.add('active');

        } catch (error) {
            console.error('Error loading transaction:', error);
            window.showToast('Failed to load transaction details', 'error');
        }
    };

    /**
     * Populate view modal with transaction data
     */
    function populateViewModal(transaction) {
        document.getElementById('viewTransactionRef').textContent = transaction.transactionReference || 'N/A';
        document.getElementById('viewTransactionDate').textContent = formatDateTime(transaction.createdAt);

        var typeBadge = document.getElementById('viewTypeBadge');
        typeBadge.textContent = transaction.transactionType || 'Unknown';
        typeBadge.className = 'type-badge ' + getTypeClass(transaction.transactionType);

        var statusBadge = document.getElementById('viewStatusBadge');
        statusBadge.textContent = transaction.transactionStatus || 'Unknown';
        statusBadge.className = 'status-badge ' + getStatusClass(transaction.transactionStatus);

        var transactionBadge = document.getElementById('viewTransactionBadge');
        transactionBadge.className = 'transaction-badge ' + getTypeClass(transaction.transactionType);

        // Transaction info
        document.getElementById('viewTransactionId').textContent = '#' + transaction.transactionId;
        document.getElementById('viewReference').textContent = transaction.transactionReference || '--';
        document.getElementById('viewType').textContent = transaction.transactionType || '--';
        document.getElementById('viewMethod').textContent = transaction.transactionMethod || '--';
        document.getElementById('viewAmount').textContent = (transaction.transactionType === 'REFUND' ? '-' : '') + '₹' + formatPrice(transaction.amount);
        document.getElementById('viewStatus').textContent = transaction.transactionStatus || '--';

        // Order info
        document.getElementById('viewOrderId').textContent = '#' + (transaction.orderId || '--');
        document.getElementById('viewInvoiceNumber').textContent = transaction.invoiceNumber || '--';
        document.getElementById('viewOrderTotal').textContent = transaction.orderTotal ? '₹' + formatPrice(transaction.orderTotal) : '--';
        document.getElementById('viewOrderStatus').textContent = transaction.orderStatus || '--';

        // Customer info
        document.getElementById('viewCustomerName').textContent = transaction.customerName || 'Unknown';
        document.getElementById('viewCustomerEmail').textContent = transaction.customerEmail || '--';
        document.getElementById('viewCustomerId').textContent = transaction.customerId ? 'ID: ' + transaction.customerId : '';

        // Refund reason
        var refundReasonSection = document.getElementById('refundReasonSection');
        if (transaction.transactionType === 'REFUND' && transaction.refundReason) {
            document.getElementById('viewRefundReason').textContent = transaction.refundReason;
            refundReasonSection.style.display = 'block';
        } else {
            refundReasonSection.style.display = 'none';
        }

        // Processing info
        document.getElementById('viewProcessedBy').textContent = transaction.processedBy || '--';
        document.getElementById('viewProcessedAt').textContent = formatDateTime(transaction.processedAt) || '--';
        document.getElementById('viewVerifiedBy').textContent = transaction.verifiedBy || '--';
        document.getElementById('viewVerifiedAt').textContent = formatDateTime(transaction.verifiedAt) || '--';

        // Timestamps
        document.getElementById('viewCreatedAt').textContent = formatDateTime(transaction.createdAt);
    }

    /**
     * Close view modal
     */
    function closeViewModal() {
        document.getElementById('viewModal').classList.remove('active');
    }

    /**
     * View order details (redirect to order management)
     */
    window.viewOrder = function (orderId) {
        window.location.href = '/JCart/views/features/admin/orderMt/?viewOrder=' + orderId;
    };

    /**
     * Open refund action modal
     */
    window.openRefundModal = function (transactionId, transactionRef, amount, refundReason) {
        processingTransactionId = transactionId;
        processingTransactionRef = transactionRef;
        processingTransactionAmount = amount;
        processingRefundReason = refundReason;

        document.getElementById('refundTransactionRef').textContent = transactionRef || 'N/A';
        document.getElementById('refundAmount').textContent = '₹' + formatPrice(amount);
        document.getElementById('refundReasonText').textContent = refundReason || 'No reason provided';
        document.getElementById('actionReason').value = '';

        document.getElementById('refundModal').classList.add('active');
    };

    /**
     * Close refund modal
     */
    function closeRefundModal() {
        document.getElementById('refundModal').classList.remove('active');
        processingTransactionId = null;
        processingTransactionRef = null;
        processingTransactionAmount = null;
        processingRefundReason = null;
    }

    /**
     * Process refund action (approve/reject)
     */
    async function processRefundAction(action) {
        if (!processingTransactionId) return;

        var reason = document.getElementById('actionReason').value.trim();
        
        if (action === 'REJECT' && !reason) {
            window.showToast('Please provide a reason for rejection', 'warning');
            return;
        }

        var approveBtn = document.getElementById('approveRefundBtn');
        var rejectBtn = document.getElementById('rejectRefundBtn');
        
        approveBtn.disabled = true;
        rejectBtn.disabled = true;

        var activeBtn = action === 'APPROVE' ? approveBtn : rejectBtn;
        var originalText = activeBtn.innerHTML;
        activeBtn.innerHTML = '<span>Processing...</span>';

        try {
            var response = await fetch('/JCart/admin/transactions/' + processingTransactionId + '/action', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    _method: 'POST',
                    action: action,
                    reason: reason
                }),
                credentials: 'include'
            });

            if (response.status === 401) {
                window.location.href = '/JCart/views/features/auth/admin/login/';
                return;
            }

            if (response.status === 403) {
                window.showToast('You do not have permission to process refunds', 'error');
                return;
            }

            if (!response.ok) {
                var errorData = await response.json();
                throw new Error(errorData.message || 'Failed to process refund');
            }

            var message = action === 'APPROVE' 
                ? 'Refund approved successfully. The refund process has been initiated.'
                : 'Refund request rejected.';
            window.showToast(message, 'success');
            closeRefundModal();
            loadTransactions(pagination ? pagination.getCurrentPage() : 1, pagination ? pagination.getPageSize() : 15);

        } catch (error) {
            console.error('Error processing refund:', error);
            window.showToast(error.message || 'Failed to process refund', 'error');
        } finally {
            approveBtn.disabled = false;
            rejectBtn.disabled = false;
            activeBtn.innerHTML = originalText;
        }
    }

})();

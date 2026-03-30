/**
 * Customer Order Detail Page Handler
 * Displays complete order information with status tracking
 */

(function () {
    'use strict';

    var ORDER_API_BASE = '/JCart/customer/orders';
    var PLACEHOLDER_IMAGE = '/JCart/views/assets/image.svg';

    var state = {
        orderId: null,
        order: null,
        isLoading: false
    };

    var elements = {};

    // Status order for progress tracking
    var STATUS_ORDER = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'];

    /**
     * Initialize the order detail page
     */
    function init() {
        if (!isLoggedIn()) {
            redirectToLogin();
            return;
        }

        state.orderId = getOrderIdFromUrl();
        if (!state.orderId) {
            showError('Order ID not provided');
            return;
        }

        cacheElements();
        setupEventListeners();
        loadNavbarAndFooter();
        loadOrderDetails();
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
            window.showToast('Please log in to view order details', 'error');
        }
        setTimeout(function () {
            window.location.href = '/JCart/views/features/auth/customer/login/?returnUrl=' + 
                encodeURIComponent(window.location.pathname + window.location.search);
        }, 1500);
    }

    /**
     * Get order ID from URL parameters
     * @returns {string|null} - Order ID or null
     */
    function getOrderIdFromUrl() {
        var params = new URLSearchParams(window.location.search);
        return params.get('id');
    }

    /**
     * Cache DOM elements
     */
    function cacheElements() {
        elements = {
            loadingState: document.getElementById('loadingState'),
            errorState: document.getElementById('errorState'),
            orderContent: document.getElementById('orderContent'),
            errorMessage: document.getElementById('errorMessage'),
            retryBtn: document.getElementById('retryBtn'),
            
            // Header
            orderNumber: document.getElementById('orderNumber'),
            orderStatus: document.getElementById('orderStatus'),
            orderDate: document.getElementById('orderDate'),
            cancelOrderBtn: document.getElementById('cancelOrderBtn'),
            
            // Progress
            orderProgressSection: document.getElementById('orderProgressSection'),
            
            // Items
            itemCount: document.getElementById('itemCount'),
            orderItemsList: document.getElementById('orderItemsList'),
            
            // Address
            shippingAddress: document.getElementById('shippingAddress'),
            
            // Summary
            summarySubtotal: document.getElementById('summarySubtotal'),
            summaryTax: document.getElementById('summaryTax'),
            summaryShipping: document.getElementById('summaryShipping'),
            discountRow: document.getElementById('discountRow'),
            summaryDiscount: document.getElementById('summaryDiscount'),
            summaryTotal: document.getElementById('summaryTotal'),
            
            // Payment
            paymentMethod: document.getElementById('paymentMethod'),
            paymentStatus: document.getElementById('paymentStatus'),
            paymentDeadlineRow: document.getElementById('paymentDeadlineRow'),
            paymentDeadline: document.getElementById('paymentDeadline'),
            
            // Payment History
            paymentHistoryList: document.getElementById('paymentHistoryList'),
            
            // Customer
            customerName: document.getElementById('customerName'),
            customerEmail: document.getElementById('customerEmail'),
            
            // Modal
            cancelModal: document.getElementById('cancelModal'),
            cancelModalNo: document.getElementById('cancelModalNo'),
            cancelModalYes: document.getElementById('cancelModalYes')
        };
    }

    /**
     * Setup event listeners
     */
    function setupEventListeners() {
        if (elements.retryBtn) {
            elements.retryBtn.addEventListener('click', loadOrderDetails);
        }

        if (elements.cancelOrderBtn) {
            elements.cancelOrderBtn.addEventListener('click', showCancelModal);
        }

        if (elements.cancelModalNo) {
            elements.cancelModalNo.addEventListener('click', hideCancelModal);
        }

        if (elements.cancelModalYes) {
            elements.cancelModalYes.addEventListener('click', handleCancelOrder);
        }

        // Close modal on overlay click
        if (elements.cancelModal) {
            elements.cancelModal.querySelector('.modal-overlay').addEventListener('click', hideCancelModal);
        }
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
     * Load order details from API
     */
    function loadOrderDetails() {
        if (state.isLoading) return;

        state.isLoading = true;
        showLoading();

        fetch(ORDER_API_BASE + '/' + encodeURIComponent(state.orderId), {
            method: 'GET',
            credentials: 'include'
        })
        .then(function (response) {
            if (response.status === 401) {
                redirectToLogin();
                return;
            }
            if (response.status === 404) {
                throw new Error('Order not found');
            }
            if (!response.ok) {
                throw new Error('Failed to load order details');
            }
            return response.json();
        })
        .then(function (data) {
            state.order = data.data || data;
            renderOrderDetails();
        })
        .catch(function (error) {
            console.error('Error loading order:', error);
            showError(error.message || 'Unable to load order details. Please try again.');
        })
        .finally(function () {
            state.isLoading = false;
        });
    }

    /**
     * Render order details
     */
    function renderOrderDetails() {
        hideAllStates();

        var order = state.order;
        if (!order) {
            showError('Order data not available');
            return;
        }

        renderHeader(order);
        renderProgress(order.orderStatus);
        renderItems(order.items || []);
        renderAddress(order.shippingAddress);
        renderSummary(order);
        renderPaymentInfo(order);
        renderPaymentHistory(order.payments || []);
        renderCustomerInfo(order.customer);
        showContent();
    }

    /**
     * Render order header
     */
    function renderHeader(order) {
        var orderNumber = order.orderId || order.invoiceNumber || 'N/A';
        var orderStatus = order.orderStatus || 'PENDING';
        var orderDate = formatDateTime(order.invoiceDate || order.createdAt);

        if (elements.orderNumber) {
            elements.orderNumber.textContent = '#' + orderNumber;
        }

        if (elements.orderStatus) {
            elements.orderStatus.className = 'status-badge ' + getStatusClass(orderStatus);
            elements.orderStatus.innerHTML = getStatusIcon(orderStatus) + getStatusLabel(orderStatus);
        }

        if (elements.orderDate) {
            elements.orderDate.textContent = orderDate;
        }

        // Show cancel button only for pending, confirmed, or processing orders (not shipped/delivered)
        if (elements.cancelOrderBtn) {
            if (orderStatus === 'PENDING' || orderStatus === 'CONFIRMED' || orderStatus === 'PROCESSING') {
                elements.cancelOrderBtn.classList.remove('hidden');
            } else {
                elements.cancelOrderBtn.classList.add('hidden');
            }
        }
    }

    /**
     * Render progress timeline
     */
    function renderProgress(currentStatus) {
        if (!elements.orderProgressSection) return;

        // Hide progress for cancelled orders
        if (currentStatus === 'CANCELLED') {
            elements.orderProgressSection.classList.add('hidden');
            return;
        }

        elements.orderProgressSection.classList.remove('hidden');

        var currentIndex = STATUS_ORDER.indexOf(currentStatus);
        if (currentIndex === -1) currentIndex = 0;

        var steps = document.querySelectorAll('.progress-step');
        var lines = document.querySelectorAll('.progress-line');

        steps.forEach(function (step, index) {
            step.classList.remove('completed', 'current');
            
            if (index < currentIndex) {
                step.classList.add('completed');
            } else if (index === currentIndex) {
                step.classList.add('current');
            }
        });

        lines.forEach(function (line, index) {
            line.classList.remove('completed');
            
            if (index < currentIndex) {
                line.classList.add('completed');
            }
        });
    }

    /**
     * Render order items
     */
    function renderItems(items) {
        if (elements.itemCount) {
            elements.itemCount.textContent = items.length;
        }

        if (!elements.orderItemsList) return;

        var html = '';
        items.forEach(function (item) {
            var productId = item.productId || '';
            var productName = item.productName || 'Unknown Product';
            var quantity = parseInt(item.quantity || 1);
            var unitPrice = parseFloat(item.unitPrice || item.price || 0);
            var discount = parseFloat(item.discount || 0);
            var hasDiscount = discount > 0;
            var finalPrice = unitPrice * (1 - discount / 100);
            var subtotal = finalPrice * quantity;
            
            var productLink = productId ? '/JCart/views/features/products/customer/detail/index.html?id=' + encodeURIComponent(productId) : '#';

            html += '<div class="order-item">' +
                '<div class="item-image">' +
                    '<a href="' + productLink + '">' +
                        '<img src="' + PLACEHOLDER_IMAGE + '" alt="' + escapeHtml(productName) + '">' +
                    '</a>' +
                '</div>' +
                '<div class="item-details">' +
                    '<div class="item-name">' +
                        '<a href="' + productLink + '" class="item-link">' + escapeHtml(productName) + '</a>' +
                    '</div>' +
                    '<div class="item-meta">' +
                        '<span>Qty: ' + quantity + '</span>' +
                    '</div>' +
                '</div>' +
                '<div class="item-price">' +
                    '<div class="item-unit-price">' +
                        (hasDiscount ? '<span class="item-price-original">₹' + formatPrice(unitPrice) + '</span>' : '') +
                        '<span class="item-price-final">₹' + formatPrice(finalPrice) + '</span>' +
                        (hasDiscount ? '<span class="item-discount">' + discount + '% OFF</span>' : '') +
                        ' × ' + quantity +
                    '</div>' +
                    '<div class="item-subtotal">₹' + formatPrice(subtotal) + '</div>' +
                '</div>' +
            '</div>';
        });

        elements.orderItemsList.innerHTML = html;
    }

    /**
     * Render shipping address
     */
    function renderAddress(address) {
        if (!elements.shippingAddress || !address) return;

        var recipientName = address.recipientName || '';
        var addressLine = address.addressLine || '';
        var city = address.city || '';
        var stateVal = address.state || '';
        var postalCode = address.postalCode || '';
        var country = address.country || '';
        var phone = address.phone;
        
        // If phone not in address, try to get from customer
        if (!phone && state.order && state.order.customer) {
            phone = state.order.customer.phone;
        }

        var fullAddress = [addressLine, city, stateVal, postalCode, country]
            .filter(function (part) { return part; })
            .join(', ');

        elements.shippingAddress.innerHTML = 
            '<div class="address-name">' + escapeHtml(recipientName) + '</div>' +
            '<div class="address-text">' + escapeHtml(fullAddress) + '</div>' +
            (phone ? '<div class="address-phone">Phone: ' + escapeHtml(phone) + '</div>' : '');
    }

    /**
     * Render order summary
     */
    function renderSummary(order) {
        var tax = parseFloat(order.tax || 0);
        var shipping = parseFloat(order.shipping || 0);

        // Compute original subtotal (before discount) and discount amount from items
        var originalSubtotal = 0;
        var computedDiscount = 0;

        if (Array.isArray(order.items) && order.items.length > 0) {
            order.items.forEach(function (item) {
                var unitPrice = parseFloat(item.unitPrice || item.price || 0);
                var discountPct = parseFloat(item.discount || 0);
                var quantity = parseInt(item.quantity || 1);

                // Add original price * quantity to subtotal
                originalSubtotal += unitPrice * quantity;

                // Calculate discount amount
                if (discountPct > 0 && unitPrice > 0 && quantity > 0) {
                    var perUnitDiscount = unitPrice * (discountPct / 100);
                    computedDiscount += perUnitDiscount * quantity;
                }
            });
        }

        // Use backend discount if available, otherwise use computed
        var discount = 0;
        if (order.discount !== null && order.discount !== undefined && !isNaN(parseFloat(order.discount))) {
            discount = parseFloat(order.discount);
        } else {
            discount = computedDiscount;
        }

        // If we couldn't compute from items, use backend subtotal and add discount back
        var displaySubtotal = originalSubtotal > 0 
            ? originalSubtotal 
            : parseFloat(order.subtotal || 0) + discount;

        // Total: use backend value if available, otherwise calculate
        // Formula: originalSubtotal - discount + tax + shipping
        var total = parseFloat(order.total || (displaySubtotal - discount + tax + shipping));

        if (elements.summarySubtotal) {
            elements.summarySubtotal.textContent = '₹' + formatPrice(displaySubtotal);
        }
        if (elements.summaryTax) {
            elements.summaryTax.textContent = '₹' + formatPrice(tax);
        }
        if (elements.summaryShipping) {
            elements.summaryShipping.textContent = shipping > 0 ? '₹' + formatPrice(shipping) : 'Free';
        }
        if (elements.discountRow && elements.summaryDiscount) {
            if (discount > 0) {
                elements.discountRow.classList.remove('hidden');
                elements.summaryDiscount.textContent = '-₹' + formatPrice(discount);
            } else {
                elements.discountRow.classList.add('hidden');
            }
        }
        if (elements.summaryTotal) {
            elements.summaryTotal.innerHTML = '<strong>₹' + formatPrice(total) + '</strong>';
        }
    }

    /**
     * Render payment information
     */
    function renderPaymentInfo(order) {
        // Get primary payment (first PAYMENT type transaction) from payments array
        var payments = order.payments || [];
        var primaryPayment = null;
        
        for (var i = 0; i < payments.length; i++) {
            if (payments[i].transactionType === 'PAYMENT') {
                primaryPayment = payments[i];
                break;
            }
        }
        
        // If no PAYMENT type found, use first payment or empty object
        if (!primaryPayment && payments.length > 0) {
            primaryPayment = payments[0];
        }
        
        var paymentMethod = (primaryPayment && primaryPayment.paymentMethod) || order.paymentMethod || 'N/A';
        var paymentStatus = order.paymentStatus || 'PENDING';
        var paymentDeadline = order.paymentDeadline;

        if (elements.paymentMethod) {
            elements.paymentMethod.textContent = formatPaymentMethod(paymentMethod);
        }

        if (elements.paymentStatus) {
            elements.paymentStatus.className = 'payment-badge ' + getPaymentStatusClass(paymentStatus);
            elements.paymentStatus.textContent = formatPaymentStatus(paymentStatus);
        }

        if (elements.paymentDeadlineRow && elements.paymentDeadline) {
            if (paymentDeadline && paymentStatus === 'PENDING') {
                elements.paymentDeadlineRow.classList.remove('hidden');
                elements.paymentDeadline.textContent = formatDateTime(paymentDeadline);
            } else {
                elements.paymentDeadlineRow.classList.add('hidden');
            }
        }
    }

    /**
     * Render payment history
     */
    function renderPaymentHistory(payments) {
        if (!elements.paymentHistoryList) return;

        if (!payments || payments.length === 0) {
            elements.paymentHistoryList.innerHTML = '<p style="color: var(--text-secondary); text-align: center; padding: 1rem;">No payment transactions found.</p>';
            return;
        }

        // Sort by date descending (most recent first)
        var sortedPayments = payments.slice().sort(function (a, b) {
            var dateA = new Date(a.transactionDate || a.createdAt);
            var dateB = new Date(b.transactionDate || b.createdAt);
            return dateB - dateA;
        });

        var html = '';
        sortedPayments.forEach(function (payment) {
            var transactionType = payment.transactionType || 'PAYMENT';
            var status = payment.status || payment.paymentStatus || 'PENDING';
            var amount = parseFloat(payment.amount || 0);
            var date = payment.transactionDate || payment.createdAt;
            var method = payment.paymentMethod || payment.method || 'N/A';
            var transactionId = payment.transactionId || 'N/A';

            html += '<div class="payment-history-item">' +
                '<div class="payment-history-header">' +
                    '<span class="payment-history-type">' + escapeHtml(transactionType) + '</span>' +
                    '<span class="payment-history-date">' + formatDateTime(date) + '</span>' +
                '</div>' +
                '<div class="payment-history-body">' +
                    '<div class="payment-history-field">' +
                        '<span class="payment-history-label">Amount</span>' +
                        '<span class="payment-history-value">₹' + formatPrice(amount) + '</span>' +
                    '</div>' +
                    '<div class="payment-history-field">' +
                        '<span class="payment-history-label">Status</span>' +
                        '<span class="payment-badge ' + getPaymentStatusClass(status) + '">' + formatPaymentStatus(status) + '</span>' +
                    '</div>' +
                    '<div class="payment-history-field">' +
                        '<span class="payment-history-label">Method</span>' +
                        '<span class="payment-history-value">' + formatPaymentMethod(method) + '</span>' +
                    '</div>' +
                    '<div class="payment-history-field">' +
                        '<span class="payment-history-label">Transaction ID</span>' +
                        '<span class="payment-history-value">' + escapeHtml(String(transactionId)) + '</span>' +
                    '</div>' +
                '</div>' +
            '</div>';
        });

        elements.paymentHistoryList.innerHTML = html;
    }

    /**
     * Render customer information
     */
    function renderCustomerInfo(customer) {
        // Try to get customer info from response, or fallback to localStorage
        var customerData = customer || {};
        var userData = null;
        
        try {
            userData = JSON.parse(localStorage.getItem('user') || '{}');
        } catch (e) {
            console.error('Error parsing user data:', e);
        }
        
        if (elements.customerName) {
            var name = customerData.name || customerData.username || 
                      (userData && (userData.name || userData.username)) || 'N/A';
            elements.customerName.textContent = name;
        }
        if (elements.customerEmail) {
            var email = customerData.email || 
                       (userData && userData.email) || 'N/A';
            elements.customerEmail.textContent = email;
        }
    }

    /**
     * Show cancel confirmation modal
     */
    function showCancelModal() {
        if (elements.cancelModal) {
            elements.cancelModal.classList.remove('hidden');
        }
    }

    /**
     * Hide cancel confirmation modal
     */
    function hideCancelModal() {
        if (elements.cancelModal) {
            elements.cancelModal.classList.add('hidden');
        }
    }

    /**
     * Handle order cancellation
     */
    function handleCancelOrder() {
        hideCancelModal();

        fetch(ORDER_API_BASE + '/' + state.orderId + '/cancel', {
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
            loadOrderDetails();
        })
        .catch(function (error) {
            console.error('Error cancelling order:', error);
            if (window.showToast) {
                window.showToast(error.message || 'Failed to cancel order', 'error');
            }
        });
    }

    /**
     * Get status class for badge
     */
    function getStatusClass(status) {
        var classes = {
            'PENDING': 'status-pending',
            'CONFIRMED': 'status-confirmed',
            'PROCESSING': 'status-processing',
            'SHIPPED': 'status-shipped',
            'DELIVERED': 'status-delivered',
            'CANCELLED': 'status-cancelled'
        };
        return classes[status] || 'status-pending';
    }

    /**
     * Get status icon
     */
    function getStatusIcon(status) {
        var icons = {
            'PENDING': '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
            'CONFIRMED': '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>',
            'PROCESSING': '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>',
            'SHIPPED': '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>',
            'DELIVERED': '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
            'CANCELLED': '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>'
        };
        return icons[status] || icons['PENDING'];
    }

    /**
     * Get status label
     */
    function getStatusLabel(status) {
        var labels = {
            'PENDING': 'Pending',
            'CONFIRMED': 'Confirmed',
            'PROCESSING': 'Processing',
            'SHIPPED': 'Shipped',
            'DELIVERED': 'Delivered',
            'CANCELLED': 'Cancelled'
        };
        return labels[status] || status;
    }

    /**
     * Get payment status class
     */
    function getPaymentStatusClass(status) {
        if (status === 'PAID') return 'payment-paid';
        if (status === 'FAILED') return 'payment-failed';
        if (status === 'REFUNDING') return 'payment-pending';
        if (status === 'REFUNDED') return 'payment-refunded';
        if (status === 'REJECTED') return 'payment-failed';
        if (status === 'PENDING') return 'payment-pending';
        return 'payment-pending';
    }

    /**
     * Format payment status
     */
    function formatPaymentStatus(status) {
        if (status === 'PAID') return 'Paid';
        if (status === 'FAILED') return 'Failed';
        if (status === 'REFUNDING') return 'Refunding';
        if (status === 'REFUNDED') return 'Refunded';
        if (status === 'REJECTED') return 'Rejected';
        if (status === 'PENDING') return 'Pending';
        return 'Pending';
    }

    /**
     * Format payment method
     */
    function formatPaymentMethod(method) {
        var methods = {
            'COD': 'Cash on Delivery',
            'ONLINE': 'Online Payment',
            'UPI': 'UPI',
            'CARD': 'Credit/Debit Card',
            'NETBANKING': 'Net Banking'
        };
        return methods[method] || method;
    }

    /**
     * UI State helpers
     */
    function hideAllStates() {
        if (elements.loadingState) elements.loadingState.classList.add('hidden');
        if (elements.errorState) elements.errorState.classList.add('hidden');
        if (elements.orderContent) elements.orderContent.classList.add('hidden');
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

    function showContent() {
        hideAllStates();
        if (elements.orderContent) elements.orderContent.classList.remove('hidden');
    }

    /**
     * Format date and time
     */
    function formatDateTime(dateStr) {
        if (!dateStr) return 'N/A';
        try {
            var date = new Date(dateStr);
            return date.toLocaleDateString('en-IN', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
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

/**
 * Shopping Cart Page Handler
 * Manages cart items, quantities, and checkout flow with backend integration
 */

(function () {
    'use strict';

    var CART_API_BASE = '/JCart/customer/cart';
    var PLACEHOLDER_IMAGE = '/JCart/views/assets/image.svg';

    var state = {
        cartItems: [],
        isLoading: false,
        totals: {
            subtotal: 0,
            tax: 0,
            shipping: 0,
            total: 0
        }
    };

    var elements = {};

    /**
     * Initialize the cart page
     */
    function init() {
        if (!isLoggedIn()) {
            redirectToLogin();
            return;
        }

        cacheElements();
        setupEventListeners();
        loadNavbarAndFooter();
        loadCart();
    }

    /**
     * Check if user is logged in
     * @returns {boolean} - True if logged in, false otherwise
     */
    function isLoggedIn() {
        return localStorage.getItem('isLoggedIn') === 'true' && localStorage.getItem('user');
    }

    /**
     * Redirect to login with message
     */
    function redirectToLogin() {
        window.showToast('Please log in to view your cart', 'error');
        setTimeout(function () {
            window.location.href = '/JCart/views/features/auth/customer/login/';
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
            cartContent: document.getElementById('cartContent'),
            errorMessage: document.getElementById('errorMessage'),
            retryBtn: document.getElementById('retryBtn'),
            cartItems: document.getElementById('cartItems'),
            clearCartBtn: document.getElementById('clearCartBtn'),
            checkoutBtn: document.getElementById('checkoutBtn'),
            itemCount: document.getElementById('itemCount'),
            summaryItemCount: document.getElementById('summaryItemCount'),
            subtotalAmount: document.getElementById('subtotalAmount'),
            discountRow: document.getElementById('discountRow'),
            discountAmount: document.getElementById('discountAmount'),
            taxAmount: document.getElementById('taxAmount'),
            shippingAmount: document.getElementById('shippingAmount'),
            totalAmount: document.getElementById('totalAmount')
        };
    }

    /**
     * Setup event listeners
     */
    function setupEventListeners() {
        if (elements.retryBtn) {
            elements.retryBtn.addEventListener('click', loadCart);
        }

        if (elements.clearCartBtn) {
            elements.clearCartBtn.addEventListener('click', handleClearCart);
        }

        if (elements.checkoutBtn) {
            elements.checkoutBtn.addEventListener('click', handleCheckout);
        }
    }

    /**
     * Load navbar and footer components
     */
    function loadNavbarAndFooter() {
        var navbarContainer = document.getElementById('navbar');
        if (navbarContainer) {
            fetch('/JCart/views/shared/navbar/navbar.html')
                .then(function (response) { return response.text(); })
                .then(function (html) {
                    navbarContainer.innerHTML = html;
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
     * Load cart items from backend
     */
    function loadCart() {
        if (state.isLoading) return;

        state.isLoading = true;
        showLoading();

        fetch(CART_API_BASE, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
        })
        .then(function (response) {
            if (response.status === 401) {
                redirectToLogin();
                return;
            }
            if (!response.ok) {
                throw new Error('Failed to load cart');
            }
            return response.json();
        })
        .then(function (data) {
            var result = data.data || data;
            state.cartItems = result.items || [];
            calculateTotals();
            renderCart();
        })
        .catch(function (error) {
            console.error('Error loading cart:', error);
            showError(error.message || 'Unable to load cart. Please try again.');
        })
        .finally(function () {
            state.isLoading = false;
        });
    }

    /**
     * Calculate cart totals using subtotals from API
     */
    function calculateTotals() {
        var originalSubtotal = 0;
        var totalDiscount = 0;
        
        state.cartItems.forEach(function (item) {
            var itemPrice = parseFloat(item.price || 0);
            var itemDiscount = parseFloat(item.discount || 0);
            var quantity = parseInt(item.quantity || 0);
            
            // Calculate original subtotal (before discount)
            var itemOriginalTotal = itemPrice * quantity;
            originalSubtotal += itemOriginalTotal;
            
            // Calculate discount amount
            var discountAmount = itemPrice * quantity * (itemDiscount / 100);
            totalDiscount += discountAmount;
        });

        // Discounted subtotal for tax/shipping calculation
        var discountedSubtotal = originalSubtotal - totalDiscount;
        
        var tax = discountedSubtotal * 0.08; // 8% tax
        var shipping = discountedSubtotal > 100 ? 0 : (discountedSubtotal > 0 ? 10 : 0); // Free shipping over ₹100
        var total = discountedSubtotal + tax + shipping;

        state.totals = {
            subtotal: originalSubtotal,  // Original subtotal before discount
            discount: totalDiscount,
            tax: tax,
            shipping: shipping,
            total: total
        };
    }

    /**
     * Render cart interface based on current state
     */
    function renderCart() {
        hideAllStates();

        if (state.cartItems.length === 0) {
            showEmpty();
            return;
        }

        renderCartItems();
        renderSummary();
        showContent();
    }

    /**
     * Render cart items list
     */
    function renderCartItems() {
        if (!elements.cartItems) return;

        var html = '';
        state.cartItems.forEach(function (item) {
            html += renderCartItem(item);
        });

        elements.cartItems.innerHTML = html;
        attachItemEventListeners();
        updateItemCounts();
    }

    /**
     * Render individual cart item
     * @param {Object} item - Cart item data
     * @returns {string} - HTML string for cart item
     */
    function renderCartItem(item) {
        var itemPrice = parseFloat(item.price || 0);
        var itemDiscount = parseFloat(item.discount || 0);
        var quantity = parseInt(item.quantity || 0);
        var stockLevel = parseInt(item.availableStock || 999);
        
        var discountAmount = itemPrice * (itemDiscount / 100);
        var finalPrice = itemPrice - discountAmount;
        var subtotal = finalPrice * quantity;

        var hasDiscount = itemDiscount > 0;
        var isInStock = stockLevel > 0;
        var isLowStock = stockLevel > 0 && stockLevel <= 5;

        var stockClass = isInStock ? (isLowStock ? 'stock-low' : 'stock-in') : 'stock-out';
        var stockIcon = isInStock ? 
            (isLowStock ? 
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' :
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>'
            ) : 
            '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>';

        var stockText = isInStock ? 
            (isLowStock ? 'Only ' + stockLevel + ' left' : 'In Stock') : 
            'Out of Stock';

        return '<div class="cart-item" data-product-id="' + escapeHtml(item.productId) + '">' +
            '<div class="item-image">' +
                '<img src="' + PLACEHOLDER_IMAGE + '" alt="' + escapeHtml(item.productName || 'Product') + '">' +
            '</div>' +
            
            '<div class="item-details">' +
                '<a href="/JCart/views/features/products/customer/detail/?id=' + encodeURIComponent(item.productId) + '" class="item-name">' +
                    escapeHtml(item.productName || 'Unknown Product') +
                '</a>' +
                '<div class="item-info">' +
                    (hasDiscount ? '<span class="item-price-original">₹' + formatPrice(itemPrice) + '</span>' : '') +
                    '<span class="item-price">₹' + formatPrice(finalPrice) + '</span>' +
                    (hasDiscount ? '<span class="item-discount">' + itemDiscount + '% OFF</span>' : '') +
                '</div>' +
                '<div class="item-stock ' + stockClass + '">' +
                    '<span class="stock-icon">' + stockIcon + '</span>' +
                    stockText +
                '</div>' +
            '</div>' +
            
            '<div class="quantity-controls">' +
                '<button type="button" class="quantity-btn decrease-qty" data-product-id="' + escapeHtml(item.productId) + '"' + (quantity <= 1 ? ' disabled' : '') + '>' +
                    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/></svg>' +
                '</button>' +
                '<input type="number" class="quantity-input" value="' + quantity + '" min="1" max="50" data-product-id="' + escapeHtml(item.productId) + '">' +
                '<button type="button" class="quantity-btn increase-qty" data-product-id="' + escapeHtml(item.productId) + '"' + (quantity >= 50 || quantity >= stockLevel ? ' disabled' : '') + '>' +
                    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>' +
                '</button>' +
            '</div>' +
            
            '<div class="item-actions">' +
                '<div class="item-subtotal">₹' + formatPrice(subtotal) + '</div>' +
                '<button type="button" class="remove-item-btn" data-product-id="' + escapeHtml(item.productId) + '" title="Remove item">' +
                    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"/><path d="m19 6-1.5 14a2 2 0 0 1-2 1.8H8.5a2 2 0 0 1-2-1.8L5 6"/><path d="M10 11v6"/><path d="m14 11v6"/></svg>' +
                '</button>' +
            '</div>' +
        '</div>';
    }

    /**
     * Attach event listeners to cart item controls
     */
    function attachItemEventListeners() {
        document.querySelectorAll('.decrease-qty').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var productId = this.dataset.productId;
                var currentItem = state.cartItems.find(function (item) {
                    return item.productId === productId;
                });
                if (currentItem && currentItem.quantity > 1) {
                    updateQuantity(productId, currentItem.quantity - 1);
                }
            });
        });

        document.querySelectorAll('.increase-qty').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var productId = this.dataset.productId;
                var currentItem = state.cartItems.find(function (item) {
                    return item.productId === productId;
                });
                var stockLevel = currentItem ? (currentItem.stockLevel || currentItem.stock_level || 999) : 999;
                if (currentItem && currentItem.quantity < 50 && currentItem.quantity < stockLevel) {
                    updateQuantity(productId, currentItem.quantity + 1);
                }
            });
        });

        document.querySelectorAll('.quantity-input').forEach(function (input) {
            input.addEventListener('change', function () {
                var productId = this.dataset.productId;
                var newQuantity = parseInt(this.value);
                var currentItem = state.cartItems.find(function (item) {
                    return item.productId === productId;
                });
                var stockLevel = currentItem ? (currentItem.stockLevel || currentItem.stock_level || 999) : 999;
                
                if (isNaN(newQuantity) || newQuantity < 1) {
                    newQuantity = 1;
                } else if (newQuantity > 50) {
                    newQuantity = 50;
                } else if (newQuantity > stockLevel) {
                    newQuantity = stockLevel;
                }
                
                this.value = newQuantity;
                updateQuantity(productId, newQuantity);
            });
        });

        document.querySelectorAll('.remove-item-btn').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var productId = this.dataset.productId;
                removeItem(productId);
            });
        });
    }

    /**
     * Update item quantity
     * @param {string} productId - Product ID
     * @param {number} quantity - New quantity
     */
    function updateQuantity(productId, quantity) {
        var payload = {
            _method: 'PATCH',
            quantity: quantity
        };

        fetch(CART_API_BASE + '/' + encodeURIComponent(productId), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        })
        .then(function (response) {
            if (response.status === 401) {
                redirectToLogin();
                return;
            }
            if (!response.ok) {
                throw new Error('Failed to update quantity');
            }
            return response.json();
        })
        .then(function () {
            loadCart(); // Reload to get updated state
            if (window.loadCartCount) {
                window.loadCartCount();
            }
            window.showToast('Cart updated successfully', 'success');
        })
        .catch(function (error) {
            console.error('Error updating quantity:', error);
            window.showToast(error.message || 'Failed to update cart', 'error');
        });
    }

    /**
     * Remove item from cart
     * @param {string} productId - Product ID to remove
     */
    function removeItem(productId) {
        if (!confirm('Are you sure you want to remove this item from your cart?')) {
            return;
        }

        var payload = { _method: 'DELETE' };

        fetch(CART_API_BASE + '/' + encodeURIComponent(productId), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        })
        .then(function (response) {
            if (response.status === 401) {
                redirectToLogin();
                return;
            }
            if (!response.ok) {
                throw new Error('Failed to remove item');
            }
            return response.json();
        })
        .then(function () {
            loadCart(); // Reload to get updated state
            if (window.loadCartCount) {
                window.loadCartCount();
            }
            window.showToast('Item removed from cart', 'success');
        })
        .catch(function (error) {
            console.error('Error removing item:', error);
            window.showToast(error.message || 'Failed to remove item', 'error');
        });
    }

    /**
     * Handle clear entire cart
     */
    function handleClearCart() {
        if (!confirm('Are you sure you want to clear your entire cart? This action cannot be undone.')) {
            return;
        }

        fetch(CART_API_BASE + '/clear', {
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
                throw new Error('Failed to clear cart');
            }
            return response.json();
        })
        .then(function () {
            state.cartItems = [];
            calculateTotals();
            renderCart();
            if (window.loadCartCount) {
                window.loadCartCount();
            }
            window.showToast('Cart cleared successfully', 'success');
        })
        .catch(function (error) {
            console.error('Error clearing cart:', error);
            window.showToast(error.message || 'Failed to clear cart', 'error');
        });
    }

    /**
     * Handle checkout process
     */
    function handleCheckout() {
        if (!state.cartItems || state.cartItems.length === 0) {
            window.showToast('Your cart is empty', 'warning');
            return;
        }
        // Redirect to checkout page
        window.location.href = '/JCart/views/features/orders/customer/checkout/';
    }

    /**
     * Render cart summary
     */
    function renderSummary() {
        if (elements.subtotalAmount) {
            elements.subtotalAmount.textContent = '₹' + formatPrice(state.totals.subtotal);
        }
        
        // Show/hide discount row
        if (elements.discountRow && elements.discountAmount) {
            if (state.totals.discount > 0) {
                elements.discountRow.style.display = 'flex';
                elements.discountAmount.textContent = '-₹' + formatPrice(state.totals.discount);
            } else {
                elements.discountRow.style.display = 'none';
            }
        }
        
        if (elements.taxAmount) {
            elements.taxAmount.textContent = '₹' + formatPrice(state.totals.tax);
        }
        if (elements.shippingAmount) {
            elements.shippingAmount.textContent = state.totals.shipping === 0 ? 'Free' : '₹' + formatPrice(state.totals.shipping);
        }
        if (elements.totalAmount) {
            elements.totalAmount.innerHTML = '<strong>₹' + formatPrice(state.totals.total) + '</strong>';
        }
    }

    /**
     * Update item count displays
     */
    function updateItemCounts() {
        var itemCount = state.cartItems.length;
        if (elements.itemCount) {
            elements.itemCount.textContent = itemCount;
        }
        if (elements.summaryItemCount) {
            elements.summaryItemCount.textContent = itemCount;
        }
    }

    /**
     * Show loading state
     */
    function showLoading() {
        hideAllStates();
        if (elements.loadingState) {
            elements.loadingState.classList.remove('hidden');
        }
    }

    /**
     * Show error state with message
     * @param {string} message - Error message to display
     */
    function showError(message) {
        hideAllStates();
        if (elements.errorMessage) {
            elements.errorMessage.textContent = message;
        }
        if (elements.errorState) {
            elements.errorState.classList.remove('hidden');
        }
    }

    /**
     * Show empty cart state
     */
    function showEmpty() {
        hideAllStates();
        if (elements.emptyState) {
            elements.emptyState.classList.remove('hidden');
        }
    }

    /**
     * Show cart content
     */
    function showContent() {
        hideAllStates();
        if (elements.cartContent) {
            elements.cartContent.classList.remove('hidden');
        }
    }

    /**
     * Hide all UI states
     */
    function hideAllStates() {
        var states = [elements.loadingState, elements.errorState, elements.emptyState, elements.cartContent];
        states.forEach(function (el) {
            if (el) el.classList.add('hidden');
        });
    }

    /**
     * Format price with two decimal places
     * @param {number} price - Price to format
     * @returns {string} - Formatted price string
     */
    function formatPrice(price) {
        return parseFloat(price || 0).toFixed(2);
    }

    /**
     * Escape HTML special characters
     * @param {string} text - Text to escape
     * @returns {string} - Escaped HTML string
     */
    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    document.addEventListener('DOMContentLoaded', init);

    window.cartManager = {
        loadCart: loadCart,
        getCartSummary: function() {
            return {
                itemCount: state.cartItems.reduce(function(sum, item) {
                    return sum + (item.quantity || 0);
                }, 0),
                total: state.totals.total
            };
        }
    };
})();

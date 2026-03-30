/**
 * Checkout Page Handler
 * Manages checkout flow with address selection and order placement
 */

(function () {
    'use strict';

    var CART_API_BASE = '/JCart/customer/cart';
    var ADDRESS_API_BASE = '/JCart/customer/addresses';
    var ORDER_API_BASE = '/JCart/customer/orders';
    var PLACEHOLDER_IMAGE = '/JCart/views/assets/image.svg';

    var state = {
        checkoutMode: 'cart', // 'cart' or 'direct'
        cartItems: [],
        directBuyProduct: null,
        addresses: [],
        selectedAddressId: null,
        selectedAddressType: 'saved',
        selectedPaymentMethod: 'UPI',
        isLoading: false,
        totals: {
            subtotal: 0,
            tax: 0,
            shipping: 0,
            discount: 0,
            total: 0
        }
    };

    var elements = {};

    /**
     * Initialize the checkout page
     */
    function init() {
        if (!isLoggedIn()) {
            redirectToLogin();
            return;
        }

        // Check checkout mode from URL
        var urlParams = new URLSearchParams(window.location.search);
        var mode = urlParams.get('mode');
        
        if (mode === 'direct') {
            state.checkoutMode = 'direct';
            var directBuyData = sessionStorage.getItem('directBuyProduct');
            if (directBuyData) {
                try {
                    state.directBuyProduct = JSON.parse(directBuyData);
                } catch (e) {
                    console.error('Error parsing direct buy data:', e);
                }
            }
            
            // Clear the session storage after loading
            sessionStorage.removeItem('directBuyProduct');
            
            if (!state.directBuyProduct) {
                // No product data, redirect to products
                window.location.href = '/JCart/views/features/products/customer/search/';
                return;
            }
        }

        cacheElements();
        setupEventListeners();
        loadNavbarAndFooter();
        loadCheckoutData();
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
            window.showToast('Please log in to continue checkout', 'error');
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
            checkoutContent: document.getElementById('checkoutContent'),
            errorMessage: document.getElementById('errorMessage'),
            retryBtn: document.getElementById('retryBtn'),
            
            // Address elements
            addressTypeSaved: document.getElementById('addressTypeSaved'),
            addressTypeOnetime: document.getElementById('addressTypeOnetime'),
            savedAddressSection: document.getElementById('savedAddressSection'),
            onetimeAddressSection: document.getElementById('onetimeAddressSection'),
            addressList: document.getElementById('addressList'),
            noAddressMessage: document.getElementById('noAddressMessage'),
            addNewAddressBtn: document.getElementById('addNewAddressBtn'),
            onetimeAddressForm: document.getElementById('onetimeAddressForm'),
            
            // Order items
            orderItems: document.getElementById('orderItems'),
            itemCount: document.getElementById('itemCount'),
            
            // Summary elements
            summaryItemCount: document.getElementById('summaryItemCount'),
            subtotalAmount: document.getElementById('subtotalAmount'),
            taxAmount: document.getElementById('taxAmount'),
            shippingAmount: document.getElementById('shippingAmount'),
            discountRow: document.getElementById('discountRow'),
            discountAmount: document.getElementById('discountAmount'),
            totalAmount: document.getElementById('totalAmount'),
            
            // Address preview
            addressPreviewContent: document.getElementById('addressPreviewContent'),
            
            // Actions
            placeOrderBtn: document.getElementById('placeOrderBtn'),
            
            // Modals
            placingOrderModal: document.getElementById('placingOrderModal'),
            orderSuccessModal: document.getElementById('orderSuccessModal'),
            successOrderId: document.getElementById('successOrderId'),
            viewOrderDetailBtn: document.getElementById('viewOrderDetailBtn')
        };
    }

    /**
     * Setup event listeners
     */
    function setupEventListeners() {
        if (elements.retryBtn) {
            elements.retryBtn.addEventListener('click', loadCheckoutData);
        }

        // Address type selection
        document.querySelectorAll('input[name="addressType"]').forEach(function (radio) {
            radio.addEventListener('change', handleAddressTypeChange);
        });

        // Payment method selection
        document.querySelectorAll('input[name="paymentMethod"]').forEach(function (radio) {
            radio.addEventListener('change', function () {
                state.selectedPaymentMethod = this.value;
            });
        });

        if (elements.addNewAddressBtn) {
            elements.addNewAddressBtn.addEventListener('click', function () {
                elements.addressTypeOnetime.checked = true;
                handleAddressTypeChange();
            });
        }

        // One-time address form validation
        if (elements.onetimeAddressForm) {
            elements.onetimeAddressForm.addEventListener('input', validateAndUpdatePreview);
        }

        if (elements.placeOrderBtn) {
            elements.placeOrderBtn.addEventListener('click', handlePlaceOrder);
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
     * Load cart and addresses for checkout
     */
    function loadCheckoutData() {
        if (state.isLoading) return;

        state.isLoading = true;
        showLoading();

        if (state.checkoutMode === 'direct') {
            // Direct buy mode - only load addresses
            fetch(ADDRESS_API_BASE, { credentials: 'include' })
                .then(function (r) { return r.json(); })
                .then(function (result) {
                    var addressResult = result.data || result;
                    state.addresses = addressResult.addresses || [];
                    
                    // Convert direct buy product to cart-like format
                    state.cartItems = [{
                        productId: state.directBuyProduct.productId,
                        productName: state.directBuyProduct.name,
                        name: state.directBuyProduct.name,
                        price: state.directBuyProduct.price,
                        imageUrl: state.directBuyProduct.imageUrl,
                        quantity: state.directBuyProduct.quantity,
                        discount: state.directBuyProduct.discount || 0
                    }];
                    
                    calculateTotals();
                    renderCheckout();
                })
                .catch(function (error) {
                    console.error('Error loading addresses:', error);
                    showError(error.message || 'Unable to load checkout. Please try again.');
                })
                .finally(function () {
                    state.isLoading = false;
                });
        } else {
            // Cart mode - load cart and addresses
            Promise.all([
                fetch(CART_API_BASE, { credentials: 'include' }).then(function (r) { return r.json(); }),
                fetch(ADDRESS_API_BASE, { credentials: 'include' }).then(function (r) { return r.json(); })
            ])
            .then(function (results) {
                var cartResult = results[0].data || results[0];
                var addressResult = results[1].data || results[1];

                state.cartItems = cartResult.items || [];
                state.addresses = addressResult.addresses || [];

                if (state.cartItems.length === 0) {
                    showEmpty();
                    return;
                }

                calculateTotals();
                renderCheckout();
            })
            .catch(function (error) {
                console.error('Error loading checkout data:', error);
                showError(error.message || 'Unable to load checkout. Please try again.');
            })
            .finally(function () {
                state.isLoading = false;
            });
        }
    }

    /**
     * Calculate order totals
     */
    function calculateTotals() {
        var originalSubtotal = 0;
        var totalDiscount = 0;
        
        state.cartItems.forEach(function (item) {
            var itemPrice = parseFloat(item.price || 0);
            var itemDiscount = parseFloat(item.discount || 0);
            var quantity = parseInt(item.quantity || 0);
            
            // Calculate original subtotal (before discount)
            originalSubtotal += itemPrice * quantity;
            
            // Calculate discount amount
            var discountAmount = itemPrice * quantity * (itemDiscount / 100);
            totalDiscount += discountAmount;
        });

        // Discounted subtotal for tax/shipping calculation
        var discountedSubtotal = originalSubtotal - totalDiscount;
        
        var tax = discountedSubtotal * 0.08;
        var shipping = discountedSubtotal > 500 ? 0 : (discountedSubtotal > 0 ? 50 : 0);
        var total = discountedSubtotal + tax + shipping;

        state.totals = {
            subtotal: originalSubtotal,  // Original subtotal before discount
            tax: tax,
            shipping: shipping,
            discount: totalDiscount,
            total: total
        };
    }

    /**
     * Render checkout interface
     */
    function renderCheckout() {
        hideAllStates();

        renderOrderItems();
        renderAddresses();
        renderSummary();
        updateAddressPreview();
        validatePlaceOrderButton();
        showContent();
    }

    /**
     * Render order items list
     */
    function renderOrderItems() {
        if (!elements.orderItems) return;

        var html = '';
        var isDirectMode = state.checkoutMode === 'direct';
        
        state.cartItems.forEach(function (item, index) {
            var itemPrice = parseFloat(item.price || 0);
            var itemDiscount = parseFloat(item.discount || 0);
            var quantity = parseInt(item.quantity || 0);
            var finalPrice = itemPrice * (1 - itemDiscount / 100);
            var itemId = item.cartItemId || index;
            var hasDiscount = itemDiscount > 0;
            
            // Quantity display - remove duplicate discount badge since it's now in price section
            var quantityControl = '<div class="order-item-quantity-controls">' +
                '<label style="font-size: 0.85rem; color: #6c757d; margin-bottom: 0.25rem;">Quantity:</label>' +
                '<div style="display: flex; align-items: center; gap: 0.5rem;">' +
                    '<button type="button" class="qty-btn" onclick="changeItemQuantity(' + itemId + ', -1)" aria-label="Decrease quantity">' +
                        '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"/></svg>' +
                    '</button>' +
                    '<input type="number" id="itemQty_' + itemId + '" value="' + quantity + '" min="1" max="50" readonly class="qty-input">' +
                    '<button type="button" class="qty-btn" onclick="changeItemQuantity(' + itemId + ', 1)" aria-label="Increase quantity">' +
                        '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>' +
                    '</button>' +
                '</div>' +
            '</div>';

            // Price display with unit price and total
            var priceDisplay = '<div class="order-item-price">';
            if (hasDiscount) {
                // Show original unit price (strikethrough) and discounted unit price with discount badge
                priceDisplay += '<div style="font-size: 0.9rem; margin-bottom: 0.25rem;">';
                priceDisplay += '<span style="text-decoration: line-through; color: #999;">₹' + formatPrice(itemPrice) + '</span> ';
                priceDisplay += '<span style="color: var(--primary-color); font-weight: 600;">₹' + formatPrice(finalPrice) + '</span> ';
                priceDisplay += '<span style="color: var(--success-color); font-size: 0.8rem;">' + itemDiscount + '% OFF</span>';
                priceDisplay += ' × ' + quantity;
                priceDisplay += '</div>';
                priceDisplay += '<div style="font-weight: 600; color: var(--text-color);">₹' + formatPrice(finalPrice * quantity) + '</div>';
            } else {
                priceDisplay += '<div style="font-size: 0.9rem; margin-bottom: 0.25rem;">₹' + formatPrice(finalPrice) + ' × ' + quantity + '</div>';
                priceDisplay += '<div style="font-weight: 600;">₹' + formatPrice(finalPrice * quantity) + '</div>';
            }
            priceDisplay += '</div>';

            html += '<div class="order-item">' +
                '<div class="order-item-image">' +
                    '<img src="' + PLACEHOLDER_IMAGE + '" alt="' + escapeHtml(item.productName || 'Product') + '">' +
                '</div>' +
                '<div class="order-item-details">' +
                    '<div class="order-item-name">' + escapeHtml(item.productName || 'Unknown Product') + '</div>' +
                    quantityControl +
                '</div>' +
                priceDisplay +
            '</div>';
        });

        elements.orderItems.innerHTML = html;

        var totalQuantity = state.cartItems.reduce(function (sum, item) {
            return sum + parseInt(item.quantity || 0);
        }, 0);

        if (elements.itemCount) {
            elements.itemCount.textContent = totalQuantity;
        }
        
        // Update edit cart link for direct mode
        var editCartLink = document.querySelector('.edit-cart-link');
        if (editCartLink) {
            if (isDirectMode) {
                editCartLink.style.display = 'none';
            } else {
                editCartLink.style.display = '';
            }
        }
    }

    /**
     * Render saved addresses
     */
    function renderAddresses() {
        if (!elements.addressList) return;

        if (state.addresses.length === 0) {
            elements.addressList.classList.add('hidden');
            elements.noAddressMessage.classList.remove('hidden');
            
            // Auto-switch to one-time address if no saved addresses
            elements.addressTypeOnetime.checked = true;
            handleAddressTypeChange();
            return;
        }

        elements.addressList.classList.remove('hidden');
        elements.noAddressMessage.classList.add('hidden');

        // Find default address
        var defaultAddress = state.addresses.find(function (addr) { return addr.isDefault; });
        if (defaultAddress) {
            state.selectedAddressId = defaultAddress.addressId;
        } else if (state.addresses.length > 0) {
            state.selectedAddressId = state.addresses[0].addressId;
        }

        var html = '';
        state.addresses.forEach(function (address) {
            var isSelected = address.addressId === state.selectedAddressId;
            
            html += '<label class="address-card' + (isSelected ? ' selected' : '') + '" data-address-id="' + address.addressId + '">' +
                '<input type="radio" name="selectedAddress" value="' + address.addressId + '"' + (isSelected ? ' checked' : '') + '>' +
                '<div class="address-card-content">' +
                    '<span class="address-radio"></span>' +
                    '<div class="address-details">' +
                        '<div class="address-name">' +
                            escapeHtml(address.recipientName || 'Unknown') +
                            (address.isDefault ? '<span class="default-badge">Default</span>' : '') +
                        '</div>' +
                        '<div class="address-text">' +
                            escapeHtml(address.addressLine || '') + ', ' +
                            escapeHtml(address.city || '') + ', ' +
                            escapeHtml(address.state || '') + ' - ' +
                            escapeHtml(address.postalCode || '') +
                        '</div>' +
                        '<div class="address-phone">' + escapeHtml(address.phone || '') + '</div>' +
                    '</div>' +
                '</div>' +
            '</label>';
        });

        elements.addressList.innerHTML = html;

        // Attach event listeners
        document.querySelectorAll('.address-card').forEach(function (card) {
            card.addEventListener('click', function () {
                var addressId = parseInt(this.dataset.addressId);
                selectAddress(addressId);
            });
        });
    }

    /**
     * Select an address
     */
    function selectAddress(addressId) {
        state.selectedAddressId = addressId;
        state.selectedAddressType = 'saved';

        document.querySelectorAll('.address-card').forEach(function (card) {
            if (parseInt(card.dataset.addressId) === addressId) {
                card.classList.add('selected');
                card.querySelector('input').checked = true;
            } else {
                card.classList.remove('selected');
            }
        });

        updateAddressPreview();
        validatePlaceOrderButton();
    }

    /**
     * Handle address type change
     */
    function handleAddressTypeChange() {
        var isOnetime = elements.addressTypeOnetime.checked;
        state.selectedAddressType = isOnetime ? 'onetime' : 'saved';

        if (isOnetime) {
            elements.savedAddressSection.classList.add('hidden');
            elements.onetimeAddressSection.classList.remove('hidden');
        } else {
            elements.savedAddressSection.classList.remove('hidden');
            elements.onetimeAddressSection.classList.add('hidden');
        }

        updateAddressPreview();
        validatePlaceOrderButton();
    }

    /**
     * Validate form and update preview
     */
    function validateAndUpdatePreview() {
        updateAddressPreview();
        validatePlaceOrderButton();
    }

    /**
     * Update address preview in summary
     */
    function updateAddressPreview() {
        if (!elements.addressPreviewContent) return;

        if (state.selectedAddressType === 'saved') {
            var address = state.addresses.find(function (addr) {
                return addr.addressId === state.selectedAddressId;
            });

            if (address) {
                elements.addressPreviewContent.innerHTML = 
                    '<div class="recipient-name">' + escapeHtml(address.recipientName || '') + '</div>' +
                    '<div>' + escapeHtml(address.addressLine || '') + '</div>' +
                    '<div>' + escapeHtml(address.city || '') + ', ' + escapeHtml(address.state || '') + ' - ' + escapeHtml(address.postalCode || '') + '</div>' +
                    '<div>' + escapeHtml(address.phone || '') + '</div>';
            } else {
                elements.addressPreviewContent.innerHTML = '<p class="no-address-selected">No address selected</p>';
            }
        } else {
            var form = elements.onetimeAddressForm;
            var recipientName = form.recipientName.value.trim();
            var addressLine = form.addressLine.value.trim();
            var city = form.city.value.trim();
            var stateVal = form.state.value.trim();
            var postalCode = form.postalCode.value.trim();
            var phone = form.phone.value.trim();

            if (recipientName || addressLine) {
                elements.addressPreviewContent.innerHTML = 
                    '<div class="recipient-name">' + escapeHtml(recipientName) + '</div>' +
                    (addressLine ? '<div>' + escapeHtml(addressLine) + '</div>' : '') +
                    (city || stateVal || postalCode ? '<div>' + escapeHtml(city) + (stateVal ? ', ' + escapeHtml(stateVal) : '') + (postalCode ? ' - ' + escapeHtml(postalCode) : '') + '</div>' : '') +
                    (phone ? '<div>' + escapeHtml(phone) + '</div>' : '');
            } else {
                elements.addressPreviewContent.innerHTML = '<p class="no-address-selected">Enter delivery address</p>';
            }
        }
    }

    /**
     * Validate place order button
     */
    function validatePlaceOrderButton() {
        if (!elements.placeOrderBtn) return;

        var isValid = false;

        if (state.selectedAddressType === 'saved') {
            isValid = state.selectedAddressId !== null;
        } else {
            var form = elements.onetimeAddressForm;
            isValid = form.recipientName.value.trim() !== '' &&
                      form.addressLine.value.trim() !== '' &&
                      form.city.value.trim() !== '' &&
                      form.state.value.trim() !== '' &&
                      form.postalCode.value.trim() !== '' &&
                      form.phone.value.trim() !== '';
        }

        elements.placeOrderBtn.disabled = !isValid || state.cartItems.length === 0;
    }

    /**
     * Render order summary
     */
    function renderSummary() {
        var totalQuantity = state.cartItems.reduce(function (sum, item) {
            return sum + parseInt(item.quantity || 0);
        }, 0);

        if (elements.summaryItemCount) {
            elements.summaryItemCount.textContent = totalQuantity;
        }
        if (elements.subtotalAmount) {
            elements.subtotalAmount.textContent = '₹' + formatPrice(state.totals.subtotal);
        }
        if (elements.taxAmount) {
            elements.taxAmount.textContent = '₹' + formatPrice(state.totals.tax);
        }
        if (elements.shippingAmount) {
            elements.shippingAmount.textContent = state.totals.shipping > 0 ? '₹' + formatPrice(state.totals.shipping) : 'Free';
        }
        if (elements.discountRow && elements.discountAmount) {
            if (state.totals.discount > 0) {
                elements.discountRow.classList.remove('hidden');
                elements.discountAmount.textContent = '-₹' + formatPrice(state.totals.discount);
            } else {
                elements.discountRow.classList.add('hidden');
            }
        }
        if (elements.totalAmount) {
            elements.totalAmount.innerHTML = '<strong>₹' + formatPrice(state.totals.total) + '</strong>';
        }
    }

    /**
     * Handle place order
     */
    function handlePlaceOrder() {
        if (elements.placeOrderBtn.disabled) return;

        var orderData = {
            paymentMethod: state.selectedPaymentMethod
        };

        if (state.selectedAddressType === 'saved') {
            orderData.addressType = 'SAVED';
            orderData.addressId = state.selectedAddressId;
        } else {
            var form = elements.onetimeAddressForm;
            orderData.addressType = 'NEW';
            orderData.oneTimeAddress = {
                recipientName: form.recipientName.value.trim(),
                addressLine: form.addressLine.value.trim(),
                city: form.city.value.trim(),
                state: form.state.value.trim(),
                postalCode: form.postalCode.value.trim(),
                country: form.country.value.trim() || 'India',
                phone: form.phone.value.trim()
            };
        }

        showPlacingOrderModal();

        var endpoint, method;
        
        if (state.checkoutMode === 'direct') {
            // Direct buy mode - use /orders/direct endpoint
            endpoint = ORDER_API_BASE + '/direct';
            orderData.productId = state.directBuyProduct.productId;
            orderData.quantity = state.directBuyProduct.quantity;
        } else {
            // Cart mode - use /orders/cart endpoint
            endpoint = ORDER_API_BASE + '/cart';
        }

        fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(orderData)
        })
        .then(function (response) {
            if (response.status === 401) {
                redirectToLogin();
                return;
            }
            if (!response.ok) {
                return response.json().then(function (data) {
                    throw new Error(data.message || 'Failed to place order');
                });
            }
            return response.json();
        })
        .then(function (data) {
            hidePlacingOrderModal();
            var orderResponse = data.data || data;
            showOrderSuccess(orderResponse);
            
            // Clear cart count in navbar (only for cart mode)
            if (state.checkoutMode === 'cart' && window.loadCartCount) {
                window.loadCartCount();
            }
        })
        .catch(function (error) {
            hidePlacingOrderModal();
            console.error('Error placing order:', error);
            if (window.showToast) {
                window.showToast(error.message || 'Failed to place order. Please try again.', 'error');
            }
        });
    }

    /**
     * Show placing order modal
     */
    function showPlacingOrderModal() {
        if (elements.placingOrderModal) {
            elements.placingOrderModal.classList.remove('hidden');
        }
    }

    /**
     * Hide placing order modal
     */
    function hidePlacingOrderModal() {
        if (elements.placingOrderModal) {
            elements.placingOrderModal.classList.add('hidden');
        }
    }

    /**
     * Show order success modal
     */
    function showOrderSuccess(orderResponse) {
        if (elements.orderSuccessModal) {
            var orderId = orderResponse.orderId || orderResponse.invoiceNumber || 'N/A';
            if (elements.successOrderId) {
                elements.successOrderId.textContent = orderId;
            }
            if (elements.viewOrderDetailBtn) {
                elements.viewOrderDetailBtn.href = '/JCart/views/features/orders/customer/detail/?id=' + orderId;
            }
            elements.orderSuccessModal.classList.remove('hidden');
        }
    }

    /**
     * UI State helpers
     */
    function hideAllStates() {
        if (elements.loadingState) elements.loadingState.classList.add('hidden');
        if (elements.errorState) elements.errorState.classList.add('hidden');
        if (elements.emptyState) elements.emptyState.classList.add('hidden');
        if (elements.checkoutContent) elements.checkoutContent.classList.add('hidden');
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
        if (elements.checkoutContent) elements.checkoutContent.classList.remove('hidden');
    }

    /**
     * Format price with 2 decimal places
     */
    function formatPrice(price) {
        return parseFloat(price || 0).toFixed(2);
    }

    /**
     * Change quantity for any item (direct or cart mode)
     */
    window.changeItemQuantity = function(itemId, delta) {
        var itemIndex = state.checkoutMode === 'direct' ? 0 : itemId;
        if (!state.cartItems[itemIndex]) return;
        
        var qtyInput = document.getElementById('itemQty_' + itemId);
        if (!qtyInput) return;
        
        var currentQty = parseInt(qtyInput.value || 1);
        var newQty = currentQty + delta;
        
        if (newQty < 1) newQty = 1;
        if (newQty > 50) newQty = 50;
        
        // Update state
        state.cartItems[itemIndex].quantity = newQty;
        if (state.checkoutMode === 'direct' && state.directBuyProduct) {
            state.directBuyProduct.quantity = newQty;
        }
        
        // For cart mode, update cart via API
        if (state.checkoutMode === 'cart') {
            updateCartItemQuantity(state.cartItems[itemIndex].productId, newQty);
        }
        
        // Update display
        qtyInput.value = newQty;
        
        // Recalculate totals and re-render
        calculateTotals();
        renderOrderItems();
        renderSummary();
    };
    
    /**
     * Update cart item quantity via API
     */
    function updateCartItemQuantity(productId, quantity) {
        fetch(CART_API_BASE, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                productId: productId,
                quantity: quantity
            })
        })
        .then(function (response) {
            if (response.ok) {
                if (window.loadCartCount) {
                    window.loadCartCount();
                }
            }
        })
        .catch(function (error) {
            console.error('Error updating cart quantity:', error);
        });
    }

    /**
     * Escape HTML characters
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

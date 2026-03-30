/**
 * Product Detail Page Handler
 * Handles product detail display, add to cart, and buy now functionality
 */

(function () {
    'use strict';

    var state = {
        product: null,
        quantity: 1,
        isLoading: false,
        maxQuantity: 50
    };

    var elements = {};

    var PLACEHOLDER_IMAGE = '/JCart/views/assets/image.svg';

    /**
     * Initialize the product detail page
     */
    function init() {
        cacheElements();
        setupEventListeners();
        loadProduct();
    }

    /**
     * Cache DOM elements
     */
    function cacheElements() {
        elements = {
            loadingState: document.getElementById('loadingState'),
            errorState: document.getElementById('errorState'),
            errorMessage: document.getElementById('errorMessage'),
            productDetail: document.getElementById('productDetail'),
            breadcrumbProduct: document.getElementById('breadcrumbProduct'),
            productImage: document.getElementById('productImage'),
            productBadge: document.getElementById('productBadge'),
            productCategory: document.getElementById('productCategory'),
            productName: document.getElementById('productName'),
            productId: document.getElementById('productId'),
            currentPrice: document.getElementById('currentPrice'),
            originalPrice: document.getElementById('originalPrice'),
            discountBadge: document.getElementById('discountBadge'),
            taxInfo: document.getElementById('taxInfo'),
            stockStatus: document.getElementById('stockStatus'),
            shippingInfo: document.getElementById('shippingInfo'),
            productGender: document.getElementById('productGender'),
            productAgeGroup: document.getElementById('productAgeGroup'),
            productSeasonality: document.getElementById('productSeasonality'),
            productLocation: document.getElementById('productLocation'),
            attrGender: document.getElementById('attrGender'),
            attrAgeGroup: document.getElementById('attrAgeGroup'),
            attrSeasonality: document.getElementById('attrSeasonality'),
            attrLocation: document.getElementById('attrLocation'),
            quantitySelector: document.getElementById('quantitySelector'),
            quantity: document.getElementById('quantity'),
            decreaseQty: document.getElementById('decreaseQty'),
            increaseQty: document.getElementById('increaseQty'),
            addToCartBtn: document.getElementById('addToCartBtn'),
            buyNowBtn: document.getElementById('buyNowBtn'),
            outOfStockMessage: document.getElementById('outOfStockMessage')
        };
    }

    /**
     * Set up event listeners
     */
    function setupEventListeners() {
        if (elements.decreaseQty) {
            elements.decreaseQty.addEventListener('click', function () {
                if (state.quantity > 1) {
                    state.quantity--;
                    updateQuantityDisplay();
                }
            });
        }

        if (elements.increaseQty) {
            elements.increaseQty.addEventListener('click', function () {
                var maxQty = Math.min(state.maxQuantity, state.product ? state.product.stockLevel : state.maxQuantity);
                if (state.quantity < maxQty) {
                    state.quantity++;
                    updateQuantityDisplay();
                }
            });
        }

        if (elements.addToCartBtn) {
            elements.addToCartBtn.addEventListener('click', handleAddToCart);
        }

        if (elements.buyNowBtn) {
            elements.buyNowBtn.addEventListener('click', handleBuyNow);
        }
    }

    /**
     * Get product ID from URL
     */
    function getProductIdFromUrl() {
        var params = new URLSearchParams(window.location.search);
        return params.get('id');
    }

    /**
     * Load product details
     */
    function loadProduct() {
        var productId = getProductIdFromUrl();

        if (!productId) {
            showError('No product ID provided');
            return;
        }

        showLoading();

        fetch('/JCart/products/' + encodeURIComponent(productId), {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
        })
        .then(function (response) {
            if (!response.ok) {
                if (response.status === 404) {
                    throw new Error('Product not found');
                }
                throw new Error('Failed to load product');
            }
            return response.json();
        })
        .then(function (data) {
            state.product = data.data || data;
            renderProduct();
        })
        .catch(function (error) {
            console.error('Error loading product:', error);
            showError(error.message || 'Unable to load product details');
        });
    }

    /**
     * Render product details
     */
    function renderProduct() {
        var product = state.product;

        if (!product) {
            showError('Product data is unavailable');
            return;
        }

        hideAllStates();
        if (elements.productDetail) {
            elements.productDetail.classList.remove('hidden');
        }

        document.title = product.productName + ' - JCart';

        if (elements.breadcrumbProduct) {
            elements.breadcrumbProduct.textContent = truncate(product.productName, 40);
        }

        if (elements.productImage) {
            elements.productImage.src = PLACEHOLDER_IMAGE;
            elements.productImage.alt = product.productName;
        }

        var inStock = product.stockLevel !== null && product.stockLevel > 0;
        var hasDiscount = product.discount && product.discount > 0;

        if (elements.productBadge) {
            if (!inStock) {
                elements.productBadge.textContent = 'Out of Stock';
                elements.productBadge.className = 'product-badge badge-out-of-stock';
                elements.productBadge.classList.remove('hidden');
            } else if (hasDiscount) {
                elements.productBadge.textContent = product.discount + '% OFF';
                elements.productBadge.className = 'product-badge badge-discount';
                elements.productBadge.classList.remove('hidden');
            } else {
                elements.productBadge.classList.add('hidden');
            }
        }

        if (elements.productCategory) {
            elements.productCategory.textContent = product.category || 'General';
        }
        if (elements.productName) {
            elements.productName.textContent = product.productName;
        }
        if (elements.productId) {
            elements.productId.textContent = product.productId;
        }

        var originalPrice = product.price || 0;
        var finalPrice = product.finalPrice || originalPrice;

        if (elements.currentPrice) {
            elements.currentPrice.textContent = '₹' + formatPrice(finalPrice);
        }

        if (elements.originalPrice) {
            if (hasDiscount && originalPrice !== finalPrice) {
                elements.originalPrice.textContent = '₹' + formatPrice(originalPrice);
                elements.originalPrice.classList.remove('hidden');
            } else {
                elements.originalPrice.classList.add('hidden');
            }
        }

        if (elements.discountBadge) {
            if (hasDiscount) {
                elements.discountBadge.textContent = 'Save ' + product.discount + '%';
                elements.discountBadge.classList.remove('hidden');
            } else {
                elements.discountBadge.classList.add('hidden');
            }
        }

        if (elements.taxInfo) {
            var taxRate = product.taxRate || 0;
            elements.taxInfo.textContent = taxRate > 0 ? 'Includes ' + taxRate + '% tax' : 'Tax calculated at checkout';
        }

        if (elements.stockStatus) {
            if (!inStock) {
                elements.stockStatus.innerHTML = 
                    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>' +
                    'Out of Stock';
                elements.stockStatus.className = 'stock-status out-of-stock';
            } else if (product.stockLevel <= 5) {
                elements.stockStatus.innerHTML = 
                    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' +
                    'Only ' + product.stockLevel + ' left in stock';
                elements.stockStatus.className = 'stock-status low-stock';
            } else {
                elements.stockStatus.innerHTML = 
                    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>' +
                    'In Stock (' + product.stockLevel + ' available)';
                elements.stockStatus.className = 'stock-status in-stock';
            }
        }

        if (elements.shippingInfo) {
            var shippingCost = product.shippingCost || 0;
            var shippingMethod = product.shippingMethod || 'Standard';
            elements.shippingInfo.innerHTML = 
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>' +
                (shippingCost > 0 ? shippingMethod + ' - ₹' + formatPrice(shippingCost) : 'Free Shipping');
        }

        updateAttribute(elements.attrGender, elements.productGender, product.gender);
        updateAttribute(elements.attrAgeGroup, elements.productAgeGroup, product.ageGroup);
        updateAttribute(elements.attrSeasonality, elements.productSeasonality, product.seasonality);
        updateAttribute(elements.attrLocation, elements.productLocation, product.location);

        if (inStock) {
            state.maxQuantity = Math.min(50, product.stockLevel);
            state.quantity = 1;
            updateQuantityDisplay();

            if (elements.quantitySelector) {
                elements.quantitySelector.classList.remove('hidden');
            }
            if (elements.addToCartBtn) {
                elements.addToCartBtn.disabled = false;
            }
            if (elements.buyNowBtn) {
                elements.buyNowBtn.disabled = false;
            }
            if (elements.outOfStockMessage) {
                elements.outOfStockMessage.classList.add('hidden');
            }
        } else {
            if (elements.quantitySelector) {
                elements.quantitySelector.classList.add('hidden');
            }
            if (elements.addToCartBtn) {
                elements.addToCartBtn.disabled = true;
            }
            if (elements.buyNowBtn) {
                elements.buyNowBtn.disabled = true;
            }
            if (elements.outOfStockMessage) {
                elements.outOfStockMessage.classList.remove('hidden');
            }
        }
    }

    /**
     * Update product attribute display - show/hide based on value
     * @param {HTMLElement} container - Container element
     * @param {HTMLElement} valueEl - Value display element
     * @param {string} value - Attribute value
     */
    function updateAttribute(container, valueEl, value) {
        if (!container || !valueEl) return;

        if (value) {
            valueEl.textContent = value;
            container.classList.remove('hidden');
        } else {
            container.classList.add('hidden');
        }
    }

    /**
     * Update quantity display
     */
    function updateQuantityDisplay() {
        if (elements.quantity) {
            elements.quantity.value = state.quantity;
        }
        if (elements.decreaseQty) {
            elements.decreaseQty.disabled = state.quantity <= 1;
        }
        if (elements.increaseQty) {
            elements.increaseQty.disabled = state.quantity >= state.maxQuantity;
        }
    }

    /**
     * Handle Add to Cart
     */
    function handleAddToCart() {
        if (!state.product || state.isLoading) return;

        var isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
        if (!isLoggedIn) {
            window.showToast('Please log in to add items to cart', 'error');
            setTimeout(function () {
                window.location.href = '/JCart/views/features/auth/customer/login/';
            }, 1500);
            return;
        }

        state.isLoading = true;
        setButtonLoading(elements.addToCartBtn, true);

        fetch('/JCart/customer/cart', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                productId: state.product.productId,
                quantity: state.quantity
            })
        })
        .then(function (response) {
            if (!response.ok) {
                return response.json().then(function (data) {
                    throw new Error(data.message || 'Failed to add to cart');
                });
            }
            return response.json();
        })
        .then(function () {
            if (window.loadCartCount) {
                window.loadCartCount();
            }

            if (window.cartModal && state.product) {
                window.cartModal.show({
                    productName: state.product.productName,
                    price: state.product.finalPrice || state.product.price,
                    quantity: state.quantity,
                    image: '/JCart/views/assets/images/product-placeholder.jpg'
                });
            }

            window.showToast('Added to cart successfully!', 'success');
        })
        .catch(function (error) {
            console.error('Error adding to cart:', error);
            window.showToast(error.message || 'Failed to add to cart', 'error');
        })
        .finally(function () {
            state.isLoading = false;
            setButtonLoading(elements.addToCartBtn, false);
        });
    }

    /**
     * Handle Buy Now - Direct checkout without adding to cart
     */
    function handleBuyNow() {
        if (!state.product || state.isLoading) return;

        var isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';
        if (!isLoggedIn) {
            window.showToast('Please log in to proceed', 'error');
            setTimeout(function () {
                window.location.href = '/JCart/views/features/auth/customer/login/';
            }, 1500);
            return;
        }

        // Store direct buy product details in sessionStorage
        var directBuyData = {
            productId: state.product.productId,
            name: state.product.productName,
            price: state.product.price,
            discount: state.product.discount || 0,
            imageUrl: state.product.imageUrl,
            quantity: state.quantity
        };
        
        sessionStorage.setItem('directBuyProduct', JSON.stringify(directBuyData));
        
        // Redirect to checkout
        window.location.href = '/JCart/views/features/orders/customer/checkout/?mode=direct';
    }

    /**
     * Set button loading state with spinner
     * @param {HTMLElement} button - Button element
     * @param {boolean} isLoading - Loading state
     */
    function setButtonLoading(button, isLoading) {
        if (!button) return;

        if (isLoading) {
            button.disabled = true;
            var span = button.querySelector('span');
            if (span) {
                button.dataset.originalText = span.textContent;
                span.innerHTML = '<span class="loading"></span> Processing...';
            }
        } else {
            button.disabled = false;
            var span = button.querySelector('span');
            if (span && button.dataset.originalText) {
                span.textContent = button.dataset.originalText;
            }
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
     * Show error state
     */
    function showError(message) {
        hideAllStates();
        if (elements.errorState) {
            elements.errorState.classList.remove('hidden');
        }
        if (elements.errorMessage) {
            elements.errorMessage.textContent = message;
        }
    }

    /**
     * Hide all states
     */
    function hideAllStates() {
        if (elements.loadingState) elements.loadingState.classList.add('hidden');
        if (elements.errorState) elements.errorState.classList.add('hidden');
        if (elements.productDetail) elements.productDetail.classList.add('hidden');
    }

    /**
     * Format price with two decimal places
     */
    function formatPrice(price) {
        return parseFloat(price).toFixed(2);
    }

    /**
     * Truncate text
     */
    function truncate(text, maxLength) {
        if (!text) return '';
        return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    }

    document.addEventListener('DOMContentLoaded', init);
})();


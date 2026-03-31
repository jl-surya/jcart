/**
 * Product Search Page Handler
 * Handles product browsing, filtering, and pagination
 */

(function () {
    'use strict';

    var state = {
        products: [],
        filters: {
            keyword: '',
            category: '',
            gender: '',
            ageGroup: '',
            seasonality: '',
            minPrice: null,
            maxPrice: null,
            inStock: false,
            sortBy: 'created_at_desc',
            sortDir: 'DESC',
            page: 1,
            size: 15
        },
        pagination: {
            currentPage: 1,
            totalPages: 1,
            totalItems: 0,
            pageSize: 15
        },
        isLoading: false,
        filterOptions: {}
    };

    var elements = {};

    var pagination = null;

    var PLACEHOLDER_IMAGE = '/JCart/views/assets/image.svg';

    /**
     * Initialize the product search page
     */
    function init() {
        cacheElements();
        setupEventListeners();
        loadFilterOptions();
        initPagination();
        loadProducts();
    }

    /**
     * Cache DOM elements
     */
    function cacheElements() {
        elements = {
            filtersForm: document.getElementById('filtersForm'),
            filtersSidebar: document.getElementById('filtersSidebar'),
            filtersOverlay: document.getElementById('filtersOverlay'),
            toggleFiltersBtn: document.getElementById('toggleFiltersBtn'),
            closeFiltersBtn: document.getElementById('closeFiltersBtn'),
            clearFiltersBtn: document.getElementById('clearFiltersBtn'),
            resetFiltersBtn: document.getElementById('resetFiltersBtn'),
            retryBtn: document.getElementById('retryBtn'),
            productsGrid: document.getElementById('productsGrid'),
            loadingState: document.getElementById('loadingState'),
            emptyState: document.getElementById('emptyState'),
            errorState: document.getElementById('errorState'),
            errorMessage: document.getElementById('errorMessage'),
            sortBy: document.getElementById('sortBy'),
            searchKeyword: document.getElementById('searchKeyword'),
            filterCategory: document.getElementById('filterCategory'),
            filterGender: document.getElementById('filterGender'),
            filterAgeGroup: document.getElementById('filterAgeGroup'),
            filterSeasonality: document.getElementById('filterSeasonality'),
            filterMinPrice: document.getElementById('filterMinPrice'),
            filterMaxPrice: document.getElementById('filterMaxPrice'),
            filterInStock: document.getElementById('filterInStock')
        };
    }

    /**
     * Set up event listeners
     */
    function setupEventListeners() {
        if (elements.filtersForm) {
            elements.filtersForm.addEventListener('submit', function (e) {
                e.preventDefault();
                collectFilters();
                state.filters.page = 1;
                loadProducts();
                closeSidebar();
            });
        }

        if (elements.sortBy) {
            elements.sortBy.addEventListener('change', function () {
                var value = this.value;
                if (value.endsWith('_desc')) {
                    state.filters.sortBy = value.replace('_desc', '');
                    state.filters.sortDir = 'DESC';
                } else {
                    state.filters.sortBy = value;
                    state.filters.sortDir = 'ASC';
                }
                state.filters.page = 1;
                loadProducts();
            });
        }

        if (elements.clearFiltersBtn) {
            elements.clearFiltersBtn.addEventListener('click', clearFilters);
        }

        if (elements.resetFiltersBtn) {
            elements.resetFiltersBtn.addEventListener('click', clearFilters);
        }

        if (elements.retryBtn) {
            elements.retryBtn.addEventListener('click', function () {
                loadProducts();
            });
        }

        if (elements.toggleFiltersBtn) {
            elements.toggleFiltersBtn.addEventListener('click', openSidebar);
        }

        if (elements.closeFiltersBtn) {
            elements.closeFiltersBtn.addEventListener('click', closeSidebar);
        }

        if (elements.filtersOverlay) {
            elements.filtersOverlay.addEventListener('click', closeSidebar);
        }

        if (elements.searchKeyword) {
            elements.searchKeyword.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    collectFilters();
                    state.filters.page = 1;
                    loadProducts();
                }
            });
        }
    }

    /**
     * Open sidebar
     */
    function openSidebar() {
        if (elements.filtersSidebar) {
            elements.filtersSidebar.classList.add('active');
        }
        if (elements.filtersOverlay) {
            elements.filtersOverlay.classList.add('active');
        }
        document.body.style.overflow = 'hidden';
    }

    /**
     * Close sidebar
     */
    function closeSidebar() {
        if (elements.filtersSidebar) {
            elements.filtersSidebar.classList.remove('active');
        }
        if (elements.filtersOverlay) {
            elements.filtersOverlay.classList.remove('active');
        }
        document.body.style.overflow = '';
    }

    /**
     * Initialize pagination using shared component
     */
    function initPagination() {
        if (!window.Pagination) {
            console.error('Pagination component not loaded');
            return;
        }

        pagination = new window.Pagination({
            onPageChange: function(page, size) {
                state.filters.page = page;
                state.filters.size = size;
                state.pagination.pageSize = size;
                loadProducts();
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        });

        pagination.init();
    }

    /**
     * Collect filter values from form inputs and update state
     */
    function collectFilters() {
        state.filters.keyword = elements.searchKeyword ? elements.searchKeyword.value.trim() : '';
        state.filters.category = elements.filterCategory ? elements.filterCategory.value : '';
        state.filters.gender = elements.filterGender ? elements.filterGender.value : '';
        state.filters.ageGroup = elements.filterAgeGroup ? elements.filterAgeGroup.value : '';
        state.filters.seasonality = elements.filterSeasonality ? elements.filterSeasonality.value : '';
        state.filters.minPrice = elements.filterMinPrice && elements.filterMinPrice.value ? parseFloat(elements.filterMinPrice.value) : null;
        state.filters.maxPrice = elements.filterMaxPrice && elements.filterMaxPrice.value ? parseFloat(elements.filterMaxPrice.value) : null;
        state.filters.inStock = elements.filterInStock ? elements.filterInStock.checked : false;
    }

    /**
     * Clear all filters
     */
    function clearFilters() {
        state.filters = {
            keyword: '',
            category: '',
            gender: '',
            ageGroup: '',
            seasonality: '',
            minPrice: null,
            maxPrice: null,
            inStock: false,
            sortBy: 'product_name',
            sortDir: 'ASC',
            page: 1,
            size: state.filters.size
        };

        if (elements.searchKeyword) elements.searchKeyword.value = '';
        if (elements.filterCategory) elements.filterCategory.value = '';
        if (elements.filterGender) elements.filterGender.value = '';
        if (elements.filterAgeGroup) elements.filterAgeGroup.value = '';
        if (elements.filterSeasonality) elements.filterSeasonality.value = '';
        if (elements.filterMinPrice) elements.filterMinPrice.value = '';
        if (elements.filterMaxPrice) elements.filterMaxPrice.value = '';
        if (elements.filterInStock) elements.filterInStock.checked = false;
        if (elements.sortBy) elements.sortBy.value = 'created_at_desc';

        loadProducts();
        closeSidebar();
    }

    /**
     * Load filter options from API
     */
    function loadFilterOptions() {
        fetch('/JCart/products/filter-options', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include'
        })
        .then(function (response) {
            if (!response.ok) throw new Error('Failed to load filter options');
            return response.json();
        })
        .then(function (data) {
            state.filterOptions = data.data || data;
            populateFilterOptions();
        })
        .catch(function (error) {
            console.error('Error loading filter options:', error);
        });
    }

    /**
     * Populate filter dropdowns with options from API
     */
    function populateFilterOptions() {
        var options = state.filterOptions;

        if (elements.filterCategory && options.categories) {
            populateSelect(elements.filterCategory, options.categories, 'All Categories');
        }

        if (elements.filterGender && options.genders) {
            populateSelect(elements.filterGender, options.genders, 'All');
        }

        if (elements.filterAgeGroup && options.ageGroups) {
            populateSelect(elements.filterAgeGroup, options.ageGroups, 'All Ages');
        }
    }

    /**
     * Populate a select element
     */
    function populateSelect(select, options, defaultText) {
        select.innerHTML = '<option value="">' + defaultText + '</option>';
        for (var i = 0; i < options.length; i++) {
            var option = document.createElement('option');
            option.value = options[i];
            option.textContent = options[i];
            select.appendChild(option);
        }
    }

    /**
     * Load products from API with current filters and pagination
     */
    function loadProducts() {
        if (state.isLoading) return;

        state.isLoading = true;
        showLoading();

        var requestBody = {
            page: state.filters.page,
            size: state.filters.size,
            sortBy: state.filters.sortBy,
            sortDir: state.filters.sortDir
        };

        if (state.filters.keyword) requestBody.keyword = state.filters.keyword;
        if (state.filters.category) requestBody.category = state.filters.category;
        if (state.filters.gender) requestBody.gender = state.filters.gender;
        if (state.filters.ageGroup) requestBody.ageGroup = state.filters.ageGroup;
        if (state.filters.seasonality) requestBody.seasonality = state.filters.seasonality;
        if (state.filters.minPrice !== null) requestBody.minPrice = state.filters.minPrice;
        if (state.filters.maxPrice !== null) requestBody.maxPrice = state.filters.maxPrice;
        if (state.filters.inStock) requestBody.inStock = true;

        fetch('/JCart/products/search', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(requestBody)
        })
        .then(function (response) {
            if (!response.ok) throw new Error('Failed to load products');
            return response.json();
        })
        .then(function (data) {
            var result = data.data || data;
            state.products = result.products || [];
            
            // Use API response values, but fallback to current state if missing
            state.pagination = {
                currentPage: result.page || state.filters.page || 1,
                totalPages: result.totalPages || 1,
                totalItems: result.total || 0,
                pageSize: result.size || state.filters.size
            };

            renderProducts();
            updatePagination();
        })
        .catch(function (error) {
            console.error('Error loading products:', error);
            showError(error.message || 'Unable to load products. Please try again.');
        })
        .finally(function () {
            state.isLoading = false;
        });
    }

    /**
     * Render products grid
     */
    function renderProducts() {
        hideAllStates();

        if (state.products.length === 0) {
            showEmpty();
            return;
        }

        var html = '';
        for (var i = 0; i < state.products.length; i++) {
            html += renderProductCard(state.products[i]);
        }

        if (elements.productsGrid) {
            elements.productsGrid.innerHTML = html;
            elements.productsGrid.classList.remove('hidden');
        }
    }

    /**
     * Render a single product card HTML
     * @param {Object} product - Product data object
     * @returns {string} - HTML string for product card
     */
    function renderProductCard(product) {
        var hasDiscount = product.discount && product.discount > 0;
        var inStock = product.stockLevel !== null && product.stockLevel > 0;
        var originalPrice = product.price || 0;
        var finalPrice = product.finalPrice || originalPrice;

        var badge = '';
        if (!inStock) {
            badge = '<span class="product-badge badge-out-of-stock">Out of Stock</span>';
        } else if (hasDiscount) {
            badge = '<span class="product-badge badge-discount">' + product.discount + '% OFF</span>';
        }

        var priceHtml = '<span class="current-price">₹' + formatPrice(finalPrice) + '</span>';
        if (hasDiscount && originalPrice !== finalPrice) {
            priceHtml += '<span class="original-price">₹' + formatPrice(originalPrice) + '</span>';
        }

        var stockStatus = inStock
            ? '<span class="in-stock"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg> In Stock</span>'
            : '<span class="out-of-stock"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg> Out of Stock</span>';

        var cartButtons = '';
        if (inStock) {
            cartButtons = 
                '<div class="product-actions">' +
                    '<button class="btn-quick-add" onclick="quickAddToCart(\'' + product.productId + '\', \'' + escapeHtml(product.productName) + '\', ' + finalPrice + ', ' + (product.stockLevel || 0) + ', ' + (product.discount || 0) + ')" title="Quick add to cart">' +
                        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
                            '<circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>' +
                            '<path d="m1 1 4 4 5.39 7.836a1 1 0 0 0 .85.486L19.42 15.5a1 1 0 0 0 .85-.694l1.8-8.4a1 1 0 0 0-.97-1.243L6 4.98L5.37 3.18a1 1 0 0 0-.93-.66L1 2.5"/>' +
                        '</svg>' +
                    '</button>' +
                    '<button class="btn-quick-buy" onclick="quickBuyNow(\'' + product.productId + '\', \'' + escapeHtml(product.productName) + '\', ' + finalPrice + ', ' + (product.stockLevel || 0) + ', ' + (product.discount || 0) + ')" title="Buy now">' +
                        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
                            '<path d="M3 3h2l.4 2M7 13h10l4-8H5.4m0 0L7 13m0 0l-2.5 5M7 13l2.5 5m6.5-5v6a2 2 0 01-2 2H9a2 2 0 01-2-2v-6m8 0V9a2 2 0 00-2-2H9a2 2 0 00-2 2v4.01"/>' +
                        '</svg>' +
                    '</button>' +
                '</div>';
        }

        return '<article class="product-card">' +
            '<a href="../detail/?id=' + encodeURIComponent(product.productId) + '" class="product-link">' +
                '<div class="product-image">' +
                    '<img src="' + PLACEHOLDER_IMAGE + '" alt="' + escapeHtml(product.productName) + '" loading="lazy">' +
                    badge +
                '</div>' +
                '<div class="product-info">' +
                    '<span class="product-category">' + escapeHtml(product.category || 'General') + '</span>' +
                    '<h3 class="product-name">' + escapeHtml(product.productName) + '</h3>' +
                    '<div class="product-price">' + priceHtml + '</div>' +
                    '<div class="product-meta">' + stockStatus + '</div>' +
                '</div>' +
            '</a>' +
            cartButtons +
        '</article>';
    }

    /**
     * Update pagination component
     */
    /**
     * Update pagination using shared component
     */
    function updatePagination() {
        if (!pagination) {
            console.error('Pagination not initialized');
            return;
        }

        pagination.options.actualItemsCount = state.products.length;
        pagination.update({
            currentPage: state.pagination.currentPage,
            totalPages: state.pagination.totalPages,
            totalItems: state.pagination.totalItems,
            pageSize: state.pagination.pageSize
        });
    }

    /**
     * Show loading state
     */
    function showLoading() {
        hideAllStates();
        if (elements.loadingState) {
            elements.loadingState.classList.remove('hidden');
        }
        if (elements.productsGrid) {
            elements.productsGrid.classList.add('hidden');
        }
    }

    /**
     * Show empty state
     */
    function showEmpty() {
        hideAllStates();
        if (elements.emptyState) {
            elements.emptyState.classList.remove('hidden');
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
    /**
     * Hide all UI states (loading, error, empty)
     */
    function hideAllStates() {
        if (elements.loadingState) elements.loadingState.classList.add('hidden');
        if (elements.emptyState) elements.emptyState.classList.add('hidden');
        if (elements.errorState) elements.errorState.classList.add('hidden');
    }

    /**
     * Format price with two decimal places
     * @param {number} price - Price value to format
     * @returns {string} - Formatted price string
     */
    function formatPrice(price) {
        return parseFloat(price).toFixed(2);
    }

    /**
     * Escape HTML special characters to prevent XSS
     * @param {string} text - Text to escape
     * @returns {string} - Escaped HTML string
     */
    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Quick add to cart from product card - shows quantity modal
     * @param {string} productId - Product ID
     * @param {string} productName - Product name for modal display
     * @param {number} price - Product price
     * @param {number} stockLevel - Available stock
     * @param {number} discount - Product discount percentage
     */
    window.quickAddToCart = function(productId, productName, price, stockLevel, discount) {
        if (!window.authUtils || !window.authUtils.isAuthenticated()) {
            window.showToast('Please log in to add items to cart', 'warning');
            window.location.href = '/JCart/views/features/auth/customer/login/';
            return;
        }

        showQuantityModal(productId, productName, price, stockLevel || 99, false, discount || 0);
    };

    /**
     * Quick buy now from product card - shows quantity modal then redirects to cart
     * @param {string} productId - Product ID
     * @param {string} productName - Product name
     * @param {number} price - Product price
     * @param {number} stockLevel - Available stock
     * @param {number} discount - Product discount percentage
     */
    window.quickBuyNow = function(productId, productName, price, stockLevel, discount) {
        if (!window.authUtils || !window.authUtils.isAuthenticated()) {
            window.showToast('Please log in to make a purchase', 'warning');
            window.location.href = '/JCart/views/features/auth/customer/login/';
            return;
        }

        showQuantityModal(productId, productName, price, stockLevel || 99, true, discount || 0);
    };

    /**
     * Quantity Modal Management
     */
    var quantityModalData = {
        productId: null,
        productName: null,
        price: null,
        stockLevel: null,
        isBuyNow: false,
        discount: 0
    };

    /**
     * Show quantity selection modal
     * @param {string} productId - Product ID
     * @param {string} productName - Product name
     * @param {number} price - Product price
     * @param {number} stockLevel - Available stock
     * @param {boolean} isBuyNow - Whether this is for buy now (redirect to cart) or add to cart
     * @param {number} discount - Product discount percentage
     */
    function showQuantityModal(productId, productName, price, stockLevel, isBuyNow, discount) {
        quantityModalData = { 
            productId: productId, 
            productName: productName, 
            price: price, 
            stockLevel: stockLevel,
            isBuyNow: isBuyNow || false,
            discount: discount || 0
        };
        
        var modal = document.getElementById('quantityModal');
        var image = document.getElementById('quantityModalImage');
        var nameEl = document.getElementById('quantityModalProductName');
        var priceEl = document.getElementById('quantityModalPrice');
        var stockEl = document.getElementById('quantityModalStock');
        var qtyInput = document.getElementById('quantityInput');
        var confirmBtn = document.getElementById('quantityModalConfirm');
        
        if (image) image.src = PLACEHOLDER_IMAGE;
        if (nameEl) nameEl.textContent = productName;
        if (priceEl) priceEl.textContent = price.toFixed(2);
        if (stockEl) stockEl.textContent = stockLevel;
        if (qtyInput) {
            qtyInput.value = 1;
            qtyInput.max = stockLevel;
        }
        
        if (confirmBtn) {
            confirmBtn.textContent = isBuyNow ? 'Buy Now' : 'Add to Cart';
        }
        
        if (modal) {
            modal.classList.remove('hidden');
            document.body.style.overflow = 'hidden';
        }
    }

    /**
     * Hide quantity modal
     */
    function hideQuantityModal() {
        var modal = document.getElementById('quantityModal');
        if (modal) {
            modal.classList.add('hidden');
            document.body.style.overflow = '';
        }
        quantityModalData = { productId: null, productName: null, price: null, stockLevel: null, isBuyNow: false };
    }

    /**
     * Add product to cart with selected quantity
     */
    function addToCartWithQuantity() {
        var qtyInput = document.getElementById('quantityInput');
        var quantity = parseInt(qtyInput ? qtyInput.value : 1);
        
        if (!quantityModalData.productId || quantity < 1) {
            window.showToast('Invalid quantity selected', 'error');
            return;
        }
        
        if (quantity > quantityModalData.stockLevel) {
            window.showToast('Quantity exceeds available stock', 'error');
            return;
        }
        
        var productId = String(quantityModalData.productId);
        var productName = quantityModalData.productName;
        var price = quantityModalData.price;
        var isBuyNow = quantityModalData.isBuyNow;
        var discount = quantityModalData.discount || 0;
        
        hideQuantityModal();
        
        // If Buy Now, go directly to checkout
        if (isBuyNow) {
            var directBuyData = {
                productId: productId,
                name: productName,
                price: price,
                discount: discount,
                imageUrl: PLACEHOLDER_IMAGE,
                quantity: quantity
            };
            
            sessionStorage.setItem('directBuyProduct', JSON.stringify(directBuyData));
            window.location.href = '/JCart/views/features/orders/customer/checkout/?mode=direct';
            return;
        }
        
        // Otherwise, add to cart
        var requestBody = {
            productId: productId,
            quantity: String(quantity)
        };
        
        console.log('Adding to cart:', requestBody);
        
        // Add to cart via API
        fetch('/JCart/customer/cart', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(requestBody)
        })
        .then(function(response) {
            if (!response.ok) {
                return response.text().then(function(text) {
                    console.error('Cart API error response:', text);
                    throw new Error('Failed to add to cart');
                });
            }
            return response.json();
        })
        .then(function(data) {
            if (data.success) {
                if (window.loadCartCount) {
                    window.loadCartCount();
                }
                
                if (window.cartModal) {
                    window.cartModal.show({
                        productName: productName,
                        price: price,
                        quantity: quantity,
                        image: PLACEHOLDER_IMAGE
                    });
                }
                
                window.showToast('Product added to cart successfully!', 'success');
            } else {
                throw new Error(data.message || 'Failed to add to cart');
            }
        })
        .catch(function(error) {
            console.error('Error adding to cart:', error);
            window.showToast('Failed to add product to cart. Please try again.', 'error');
        });
    }

    /**
     * Initialize quantity modal event listeners
     */
    function initQuantityModal() {
        var modal = document.getElementById('quantityModal');
        var overlay = document.getElementById('quantityModalOverlay');
        var closeBtn = document.getElementById('quantityModalClose');
        var cancelBtn = document.getElementById('quantityModalCancel');
        var confirmBtn = document.getElementById('quantityModalConfirm');
        var decreaseBtn = document.getElementById('decreaseQty');
        var increaseBtn = document.getElementById('increaseQty');
        var qtyInput = document.getElementById('quantityInput');
        
        if (closeBtn) closeBtn.addEventListener('click', hideQuantityModal);
        if (cancelBtn) cancelBtn.addEventListener('click', hideQuantityModal);
        if (overlay) overlay.addEventListener('click', hideQuantityModal);
        
        if (confirmBtn) confirmBtn.addEventListener('click', addToCartWithQuantity);
        
        if (decreaseBtn && qtyInput) {
            decreaseBtn.addEventListener('click', function() {
                var val = parseInt(qtyInput.value) || 1;
                if (val > 1) qtyInput.value = val - 1;
            });
        }
        
        if (increaseBtn && qtyInput) {
            increaseBtn.addEventListener('click', function() {
                var val = parseInt(qtyInput.value) || 1;
                var max = parseInt(qtyInput.max) || 99;
                if (val < max) qtyInput.value = val + 1;
            });
        }
        
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && modal && !modal.classList.contains('hidden')) {
                hideQuantityModal();
            }
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        initQuantityModal();
    });

    window.initializeProductSearch = init;

    document.addEventListener('DOMContentLoaded', init);
})();


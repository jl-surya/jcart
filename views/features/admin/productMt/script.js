/**
 * Product Management Handler
 * Manages product CRUD operations, search, and table display with filters and pagination.
 */

(function () {
    'use strict';

    var productsData = [];
    var editingProductId = null;
    var pagination = null;
    var currentPermissions = [];
    var filterOptions = {};

    var PERM_PRODUCT_VIEW = 'products:view';
    var PERM_PRODUCT_CREATE = 'products:create';
    var PERM_PRODUCT_UPDATE = 'products:update';
    var PERM_PRODUCT_DELETE = 'products:delete';

    var searchTerm = '';
    var categoryFilter = '';
    var statusFilter = '';
    var stockFilter = '';
    var ageGroupFilter = '';
    var genderFilter = '';
    var seasonalityFilter = '';
    var minPrice = null;
    var maxPrice = null;
    var sortBy = 'created_at';
    var sortDir = 'DESC';

    document.addEventListener('DOMContentLoaded', function () {
        loadCurrentAdminPermissions();
        applyPermissionBasedUI();
        initializePagination();
        initializeEventListeners();
        loadFilterOptions();
        loadProducts(1, 15);
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
        if (!hasPermission(PERM_PRODUCT_VIEW)) {
            showPermissionError();
            return;
        }
        
        var addProductBtn = document.getElementById('addProductBtn');
        if (addProductBtn && !hasPermission(PERM_PRODUCT_CREATE)) {
            addProductBtn.style.display = 'none';
        }
    }

    /**
     * Show permission error and redirect
     */
    function showPermissionError() {
        window.showToast('You do not have permission to view products', 'error');
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
                        loadProducts(page, size);
                    }
                });
                pagination.init();
            })
            .catch(function (error) {
                console.error('Error loading pagination:', error);
            });
    }

    /**
     * Load filter options from API
     */
    async function loadFilterOptions() {
        try {
            var response = await fetch('/JCart/products/filter-options', {
                method: 'GET',
                credentials: 'include'
            });

            if (response.ok) {
                var data = await response.json();
                filterOptions = data.data || data;
                populateAllFilters();
            } else {
                console.error('Failed to load filter options:', response.status);
            }
        } catch (error) {
            console.error('Error loading filter options:', error);
        }
    }

    /**
     * Populate all filter dropdowns
     */
    function populateAllFilters() {
        populateSelectFilter('categoryFilter', filterOptions.categories, 'All Categories');
        populateSelectFilter('ageGroupFilter', filterOptions.ageGroups, 'All Ages');
        populateSelectFilter('genderFilter', filterOptions.genders, 'All Genders');
        
        populateSelect('category', filterOptions.categories, 'Select category');
        populateSelect('ageGroup', filterOptions.ageGroups, 'Select age group');
        populateSelect('gender', filterOptions.genders, 'Select gender');
        populateSelect('location', filterOptions.locations, 'Select location');
    }

    /**
     * Helper to populate a select filter
     */
    function populateSelectFilter(selectId, options, defaultText) {
        var selectEl = document.getElementById(selectId);
        if (!selectEl || !options) return;

        selectEl.innerHTML = '<option value="">' + defaultText + '</option>';
        
        options.forEach(function (opt) {
            if (opt) {
                var option = document.createElement('option');
                option.value = opt;
                option.textContent = opt;
                selectEl.appendChild(option);
            }
        });
    }

    /**
     * Helper to populate a form select dropdown
     */
    function populateSelect(selectId, options, defaultText) {
        var selectEl = document.getElementById(selectId);
        if (!selectEl) {
            console.warn('Select not found:', selectId);
            return;
        }
        if (!options) {
            console.warn('No options provided for:', selectId);
            return;
        }

        selectEl.innerHTML = '<option value="">' + defaultText + '</option>';
        
        options.forEach(function (opt) {
            if (opt) {
                var option = document.createElement('option');
                option.value = opt;
                option.textContent = opt;
                selectEl.appendChild(option);
            }
        });
    }

    /**
     * Helper to populate a datalist for input suggestions (deprecated)
     */
    function populateDatalist(datalistId, options) {
        var datalist = document.getElementById(datalistId);
        if (!datalist) {
            console.warn('Datalist not found:', datalistId);
            return;
        }
        if (!options) {
            console.warn('No options provided for:', datalistId);
            return;
        }

        datalist.innerHTML = '';
        
        options.forEach(function (opt) {
            if (opt) {
                var option = document.createElement('option');
                option.value = opt;
                datalist.appendChild(option);
            }
        });
    }

    /**
     * Initialize all event listeners
     */
    function initializeEventListeners() {
        var addProductBtn = document.getElementById('addProductBtn');
        if (addProductBtn) {
            addProductBtn.addEventListener('click', openAddModal);
        }

        var modalClose = document.getElementById('modalClose');
        var cancelBtn = document.getElementById('cancelBtn');
        if (modalClose) modalClose.addEventListener('click', closeModal);
        if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

        var deleteModalClose = document.getElementById('deleteModalClose');
        var deleteCancelBtn = document.getElementById('deleteCancelBtn');
        if (deleteModalClose) deleteModalClose.addEventListener('click', closeDeleteModal);
        if (deleteCancelBtn) deleteCancelBtn.addEventListener('click', closeDeleteModal);

        var confirmDeleteInput = document.getElementById('confirmDelete');
        var deleteConfirmBtn = document.getElementById('deleteConfirmBtn');
        if (confirmDeleteInput) {
            confirmDeleteInput.addEventListener('input', function () {
                deleteConfirmBtn.disabled = this.value !== 'DELETE';
            });
        }

        // Activate Modal
        var activateModalClose = document.getElementById('activateModalClose');
        var activateCancelBtn = document.getElementById('activateCancelBtn');
        if (activateModalClose) activateModalClose.addEventListener('click', closeActivateModal);
        if (activateCancelBtn) activateCancelBtn.addEventListener('click', closeActivateModal);

        // Activate confirmation input
        var confirmActivateInput = document.getElementById('confirmActivate');
        var activateConfirmBtn = document.getElementById('activateConfirmBtn');
        if (confirmActivateInput) {
            confirmActivateInput.addEventListener('input', function () {
                activateConfirmBtn.disabled = this.value !== 'ACTIVATE';
            });
        }

        // Form Submit
        var productForm = document.getElementById('productForm');
        if (productForm) {
            productForm.addEventListener('submit', handleFormSubmit);
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

        // Search input - Enter key triggers search
        var searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') {
                    applyFilters();
                }
            });
        }

        // Toggle Filters Panel
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

        // Close modals on overlay click
        var productModal = document.getElementById('productModal');
        var deleteModal = document.getElementById('deleteModal');
        var viewModal = document.getElementById('viewModal');
        var activateModal = document.getElementById('activateModal');
        
        if (productModal) {
            productModal.addEventListener('click', function (e) {
                if (e.target === this) closeModal();
            });
        }
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
        if (activateModal) {
            activateModal.addEventListener('click', function (e) {
                if (e.target === this) closeActivateModal();
            });
        }
    }

    /**
     * Apply filters and load products
     */
    function applyFilters() {
        var searchInput = document.getElementById('searchInput');
        var categoryFilterEl = document.getElementById('categoryFilter');
        var statusFilterEl = document.getElementById('statusFilter');
        var stockFilterEl = document.getElementById('stockFilter');
        var ageGroupFilterEl = document.getElementById('ageGroupFilter');
        var genderFilterEl = document.getElementById('genderFilter');
        var seasonalityFilterEl = document.getElementById('seasonalityFilter');
        var minPriceEl = document.getElementById('minPrice');
        var maxPriceEl = document.getElementById('maxPrice');
        var sortByEl = document.getElementById('sortBy');
        var sortDirEl = document.getElementById('sortDir');

        searchTerm = searchInput ? searchInput.value.trim() : '';
        categoryFilter = categoryFilterEl ? categoryFilterEl.value : '';
        statusFilter = statusFilterEl ? statusFilterEl.value : '';
        stockFilter = stockFilterEl ? stockFilterEl.value : '';
        ageGroupFilter = ageGroupFilterEl ? ageGroupFilterEl.value : '';
        genderFilter = genderFilterEl ? genderFilterEl.value : '';
        seasonalityFilter = seasonalityFilterEl ? seasonalityFilterEl.value : '';
        minPrice = minPriceEl && minPriceEl.value ? parseFloat(minPriceEl.value) : null;
        maxPrice = maxPriceEl && maxPriceEl.value ? parseFloat(maxPriceEl.value) : null;
        sortBy = sortByEl ? sortByEl.value : 'created_at';
        sortDir = sortDirEl ? sortDirEl.value : 'DESC';

        updateFilterCount();
        loadProducts(1, pagination ? pagination.getPageSize() : 15);
    }

    /**
     * Update filter count badge
     */
    function updateFilterCount() {
        var count = 0;
        if (categoryFilter) count++;
        if (statusFilter) count++;
        if (stockFilter) count++;
        if (ageGroupFilter) count++;
        if (genderFilter) count++;
        if (seasonalityFilter) count++;
        if (minPrice !== null) count++;
        if (maxPrice !== null) count++;
        
        var filterCountEl = document.getElementById('filterCount');
        if (filterCountEl) {
            filterCountEl.textContent = count > 0 ? count : '';
            filterCountEl.style.display = count > 0 ? 'inline-flex' : 'none';
        }
    }

    /**
     * Build search request body
     */
    function buildSearchRequest(page, size) {
        var request = {
            page: page,
            size: size,
            sortBy: sortBy,
            sortDir: sortDir
        };
        
        if (searchTerm) {
            request.keyword = searchTerm;
        }
        if (categoryFilter) {
            request.category = categoryFilter;
        }
        if (ageGroupFilter) {
            request.ageGroup = ageGroupFilter;
        }
        if (genderFilter) {
            request.gender = genderFilter;
        }
        if (seasonalityFilter) {
            request.seasonality = seasonalityFilter;
        }
        if (minPrice !== null && !isNaN(minPrice)) {
            request.minPrice = minPrice;
        }
        if (maxPrice !== null && !isNaN(maxPrice)) {
            request.maxPrice = maxPrice;
        }
        // Note: showInactive is handled in loadProducts based on statusFilter
        if (stockFilter === 'lowStock') {
            request.lowStock = true;
        } else if (stockFilter === 'inStock') {
            request.inStock = true;
        } else if (stockFilter === 'outOfStock') {
            request.inStock = false;
        }
        
        return request;
    }

    /**
     * Load products from API with filters
     */
    async function loadProducts(page, size) {
        showLoadingState();
        
        try {
            var searchRequest = buildSearchRequest(page, size);
            
            if (statusFilter === 'active') {
                searchRequest.showInactive = false;
            } else if (statusFilter === 'inactive') {
                searchRequest.showInactive = true;
            }
              
            var response = await fetch('/JCart/admin/products/search', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(searchRequest),
                credentials: 'include'
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
                throw new Error('Failed to load products');
            }

            var data = await response.json();
            var result = data.data || data;
            
            productsData = result.products || [];
            
            // Use stats from API response (for ALL filtered data, not just current page)
            var activeCount = result.activeCount || 0;
            var inactiveCount = result.inactiveCount || 0;
            var lowStockCount = result.lowStockCount || 0;
            var totalFiltered = activeCount + inactiveCount;
            
            updateStats(totalFiltered, activeCount, inactiveCount, lowStockCount);
            
            // Render table
            renderProductTable();
            
            if (pagination) {
                pagination.update({
                    currentPage: result.page || page,
                    totalPages: result.totalPages || 1,
                    totalItems: result.total || 0,
                    pageSize: result.size || size
                });
            }

        } catch (error) {
            console.error('Error loading products:', error);
            showTableError('Failed to load products');
        }
    }

    /**
     * Show loading state
     */
    function showLoadingState() {
        document.getElementById('productTableBody').innerHTML = 
            '<tr class="loading-row"><td colspan="7">' +
            '<div class="loading-spinner"></div>' +
            '<span>Loading products...</span>' +
            '</td></tr>';
    }

    /**
     * Update stats cards
     */
    function updateStats(total, activeCount, inactiveCount, lowStockCount) {
        document.getElementById('totalProducts').textContent = total || 0;
        document.getElementById('activeProducts').textContent = activeCount || 0;
        document.getElementById('inactiveProducts').textContent = inactiveCount || 0;
        document.getElementById('lowStockProducts').textContent = lowStockCount || 0;
    }

    /**
     * Render product table
     */
    function renderProductTable() {
        var tbody = document.getElementById('productTableBody');
        var canUpdate = hasPermission(PERM_PRODUCT_UPDATE);
        var canDelete = hasPermission(PERM_PRODUCT_DELETE);
        
        if (productsData.length === 0) {
            var hasFilters = searchTerm || categoryFilter || statusFilter || stockFilter || 
                ageGroupFilter || genderFilter || seasonalityFilter || minPrice !== null || maxPrice !== null;
            var emptyMessage = hasFilters 
                ? 'No products match your filters' 
                : 'No products found';
            tbody.innerHTML = '<tr class="empty-state"><td colspan="7">' +
                '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>' +
                '<p>' + emptyMessage + '</p></td></tr>';
            return;
        }

        var html = productsData.map(function (product) {
            var stockClass = getStockClass(product.stockLevel);
            var stockLabel = getStockLabel(product.stockLevel);

            var actionButtons = '<button class="btn-icon view" onclick="viewProduct(\'' + product.productId + '\')" title="View">' +
                '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' +
            '</button>';

            // Edit button - only show if user has update permission
            if (canUpdate) {
                actionButtons += '<button class="btn-icon" onclick="editProduct(\'' + product.productId + '\')" title="Edit">' +
                    '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>' +
                '</button>';
            }

            // Delete/Activate button - only show if user has delete permission
            if (canDelete) {
                if (product.isActive) {
                    actionButtons += '<button class="btn-icon danger" onclick="confirmDelete(\'' + product.productId + '\', \'' + escapeHtml(product.productName) + '\')" title="Deactivate">' +
                        '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>' +
                    '</button>';
                } else if (canUpdate) {
                    actionButtons += '<button class="btn-icon success" onclick="confirmActivate(\'' + product.productId + '\', \'' + escapeHtml(product.productName) + '\')" title="Activate">' +
                        '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>' +
                    '</button>';
                }
            }

            var priceCell = '<div class="price-info">';
            if (product.discount > 0) {
                priceCell += '<span class="original-price">$' + formatPrice(product.price) + '</span>';
                priceCell += '<span class="final-price">$' + formatPrice(product.finalPrice || product.price) + '</span>';
            } else {
                priceCell += '<span class="final-price">$' + formatPrice(product.price) + '</span>';
            }
            priceCell += '</div>';

            var discountCell = product.discount > 0 
                ? '<span class="discount-tag">-' + product.discount + '%</span>' 
                : '<span class="no-discount">--</span>';

            return '<tr class="' + (product.isActive ? '' : 'inactive-row') + '">' +
                '<td>' +
                    '<div class="product-info">' +
                        '<div class="product-avatar">' +
                            '<svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>' +
                        '</div>' +
                        '<div class="product-details">' +
                            '<span class="product-name">' + escapeHtml(product.productName) + '</span>' +
                            '<span class="product-id">' + escapeHtml(product.productId) + '</span>' +
                        '</div>' +
                    '</div>' +
                '</td>' +
                '<td><span class="category-tag">' + escapeHtml(product.category || 'Uncategorized') + '</span></td>' +
                '<td>' + priceCell + '</td>' +
                '<td>' + discountCell + '</td>' +
                '<td><span class="stock-badge ' + stockClass + '">' + stockLabel + '</span></td>' +
                '<td>' +
                    '<span class="status-badge ' + (product.isActive ? 'active' : 'inactive') + '">' +
                        '<span class="status-dot"></span>' +
                        (product.isActive ? 'Active' : 'Inactive') +
                    '</span>' +
                '</td>' +
                '<td>' +
                    '<div class="actions-cell">' + actionButtons + '</div>' +
                '</td>' +
            '</tr>';
        }).join('');

        tbody.innerHTML = html;
    }

    /**
     * Get stock status class
     */
    function getStockClass(stockLevel) {
        if (stockLevel === null || stockLevel === undefined) return 'unknown';
        if (stockLevel === 0) return 'out-of-stock';
        if (stockLevel <= 10) return 'low-stock';
        return 'in-stock';
    }

    /**
     * Get stock status label
     */
    function getStockLabel(stockLevel) {
        if (stockLevel === null || stockLevel === undefined) return 'N/A';
        if (stockLevel === 0) return 'Out of Stock';
        if (stockLevel <= 10) return 'Low (' + stockLevel + ')';
        return stockLevel + ' units';
    }

    /**
     * Format price with 2 decimal places
     */
    function formatPrice(price) {
        if (price === null || price === undefined) return '0.00';
        return parseFloat(price).toFixed(2);
    }

    /**
     * Clear all filters
     */
    window.clearFilters = function () {
        searchTerm = '';
        categoryFilter = '';
        statusFilter = '';
        stockFilter = '';
        ageGroupFilter = '';
        genderFilter = '';
        seasonalityFilter = '';
        minPrice = null;
        maxPrice = null;
        sortBy = 'created_at';
        sortDir = 'DESC';

        var searchInput = document.getElementById('searchInput');
        var categoryFilterEl = document.getElementById('categoryFilter');
        var statusFilterEl = document.getElementById('statusFilter');
        var stockFilterEl = document.getElementById('stockFilter');
        var ageGroupFilterEl = document.getElementById('ageGroupFilter');
        var genderFilterEl = document.getElementById('genderFilter');
        var seasonalityFilterEl = document.getElementById('seasonalityFilter');
        var minPriceEl = document.getElementById('minPrice');
        var maxPriceEl = document.getElementById('maxPrice');
        var sortByEl = document.getElementById('sortBy');
        var sortDirEl = document.getElementById('sortDir');

        if (searchInput) searchInput.value = '';
        if (categoryFilterEl) categoryFilterEl.value = '';
        if (statusFilterEl) statusFilterEl.value = '';
        if (stockFilterEl) stockFilterEl.value = '';
        if (ageGroupFilterEl) ageGroupFilterEl.value = '';
        if (genderFilterEl) genderFilterEl.value = '';
        if (seasonalityFilterEl) seasonalityFilterEl.value = '';
        if (minPriceEl) minPriceEl.value = '';
        if (maxPriceEl) maxPriceEl.value = '';
        if (sortByEl) sortByEl.value = 'created_at';
        if (sortDirEl) sortDirEl.value = 'DESC';

        updateFilterCount();
        loadProducts(1, pagination ? pagination.getPageSize() : 15);
    };

    /**
     * Open add product modal
     */
    function openAddModal() {
        editingProductId = null;
        document.getElementById('modalTitle').textContent = 'Add New Product';
        document.getElementById('submitBtn').innerHTML = '<span>Create Product</span>';
        document.getElementById('productForm').reset();
        clearFormErrors();
        document.getElementById('productModal').classList.add('active');
    }

    /**
     * Open edit product modal
     */
    window.editProduct = function (productId) {
        var product = productsData.find(function (p) { return p.productId === productId; });
        if (!product) return;

        editingProductId = productId;
        document.getElementById('modalTitle').textContent = 'Edit Product';
        document.getElementById('submitBtn').innerHTML = '<span>Save Changes</span>';

        // Populate form
        document.getElementById('productId').value = product.productId;
        document.getElementById('productName').value = product.productName || '';
        document.getElementById('category').value = product.category || '';
        document.getElementById('price').value = product.price || '';
        document.getElementById('discount').value = product.discount || '';
        document.getElementById('taxRate').value = product.taxRate || '';
        document.getElementById('stockLevel').value = product.stockLevel !== null ? product.stockLevel : '';
        document.getElementById('ageGroup').value = product.ageGroup || '';
        document.getElementById('gender').value = product.gender || '';
        document.getElementById('seasonality').value = product.seasonality || '';
        document.getElementById('location').value = product.location || '';
        document.getElementById('shippingCost').value = product.shippingCost || '';
        document.getElementById('shippingMethod').value = product.shippingMethod || '';

        clearFormErrors();
        document.getElementById('productModal').classList.add('active');
    };

    /**
     * Close modal
     */
    function closeModal() {
        document.getElementById('productModal').classList.remove('active');
        editingProductId = null;
    }

    /**
     * Confirm delete/deactivate
     */
    window.confirmDelete = function (productId, productName) {
        document.getElementById('deleteProductName').textContent = productName;
        document.getElementById('confirmDelete').value = '';
        document.getElementById('deleteConfirmBtn').disabled = true;
        document.getElementById('deleteConfirmBtn').onclick = function () {
            deactivateProduct(productId);
        };
        document.getElementById('deleteModal').classList.add('active');
    };

    /**
     * Close delete modal
     */
    function closeDeleteModal() {
        document.getElementById('deleteModal').classList.remove('active');
        document.getElementById('confirmDelete').value = '';
    }

    /**
     * Confirm activate
     */
    window.confirmActivate = function (productId, productName) {
        document.getElementById('activateProductName').textContent = productName;
        document.getElementById('confirmActivate').value = '';
        document.getElementById('activateConfirmBtn').disabled = true;
        document.getElementById('activateConfirmBtn').onclick = function () {
            activateProduct(productId);
        };
        document.getElementById('activateModal').classList.add('active');
    };

    /**
     * Close activate modal
     */
    function closeActivateModal() {
        document.getElementById('activateModal').classList.remove('active');
        document.getElementById('confirmActivate').value = '';
    }

    /**
     * Handle form submit
     */
    async function handleFormSubmit(e) {
        e.preventDefault();

        if (!validateForm()) return;

        var submitBtn = document.getElementById('submitBtn');
        var originalText = submitBtn.innerHTML;
        submitBtn.innerHTML = '<span class="loading"></span> Saving...';
        submitBtn.disabled = true;

        try {
            var formData = getFormData();
            var url = '/JCart/admin/products/';
            var method = 'POST';

            if (editingProductId) {
                url = '/JCart/admin/products/' + editingProductId;
                formData._method = 'PATCH';
            }

            var response = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData),
                credentials: 'include'
            });

            var data = await response.json();

            if (response.ok) {
                window.showToast(editingProductId ? 'Product updated successfully' : 'Product created successfully', 'success');
                closeModal();
                loadProducts(pagination ? pagination.getCurrentPage() : 1, pagination ? pagination.getPageSize() : 15);
                // Reload filter options in case new category/ageGroup/gender/location was added
                loadFilterOptions();
            } else {
                window.showToast(data.message || 'Operation failed', 'error');
            }

        } catch (error) {
            console.error('Form submit error:', error);
            window.showToast('Network error. Please try again.', 'error');
        } finally {
            submitBtn.innerHTML = originalText;
            submitBtn.disabled = false;
        }
    }

    /**
     * Get form data
     */
    function getFormData() {
        var data = {
            productName: document.getElementById('productName').value.trim(),
            category: document.getElementById('category').value.trim() || null,
            price: document.getElementById('price').value || null,
            discount: document.getElementById('discount').value || null,
            taxRate: document.getElementById('taxRate').value || null,
            stockLevel: document.getElementById('stockLevel').value || null,
            ageGroup: document.getElementById('ageGroup').value.trim() || null,
            gender: document.getElementById('gender').value.trim() || null,
            seasonality: document.getElementById('seasonality').value || null,
            location: document.getElementById('location').value.trim() || null,
            shippingCost: document.getElementById('shippingCost').value || null,
            shippingMethod: document.getElementById('shippingMethod').value || null
        };

        return data;
    }

    /**
     * Validate form
     */
    function validateForm() {
        clearFormErrors();
        var isValid = true;

        // Product Name - required, min 2 chars, max 200 chars
        var productName = document.getElementById('productName').value.trim();
        if (!productName) {
            showFieldError('productName', 'Product name is required');
            isValid = false;
        } else if (productName.length < 2) {
            showFieldError('productName', 'Product name must be at least 2 characters');
            isValid = false;
        } else if (productName.length > 200) {
            showFieldError('productName', 'Product name must not exceed 200 characters');
            isValid = false;
        }

        // Price - required, must be positive number
        var price = document.getElementById('price').value;
        if (!price || price === '') {
            showFieldError('price', 'Price is required');
            isValid = false;
        } else if (parseFloat(price) <= 0) {
            showFieldError('price', 'Price must be greater than 0');
            isValid = false;
        } else if (parseFloat(price) > 999999.99) {
            showFieldError('price', 'Price must not exceed 999,999.99');
            isValid = false;
        }

        // Discount - optional, must be between 0 and 100
        var discount = document.getElementById('discount').value;
        if (discount && discount !== '') {
            var discountValue = parseFloat(discount);
            if (isNaN(discountValue) || discountValue < 0 || discountValue > 100) {
                showFieldError('discount', 'Discount must be between 0 and 100');
                isValid = false;
            }
        }

        // Tax Rate - required, must be non-negative
        var taxRate = document.getElementById('taxRate').value;
        if (!taxRate || taxRate === '') {
            showFieldError('taxRate', 'Tax rate is required');
            isValid = false;
        } else if (parseFloat(taxRate) < 0) {
            showFieldError('taxRate', 'Tax rate cannot be negative');
            isValid = false;
        } else if (parseFloat(taxRate) > 100) {
            showFieldError('taxRate', 'Tax rate must not exceed 100%');
            isValid = false;
        }

        var stockLevel = document.getElementById('stockLevel').value;
        if (stockLevel && stockLevel !== '') {
            var stockValue = parseInt(stockLevel);
            if (isNaN(stockValue) || stockValue < 0) {
                showFieldError('stockLevel', 'Stock level must be a non-negative number');
                isValid = false;
            } else if (stockValue > 999999) {
                showFieldError('stockLevel', 'Stock level must not exceed 999,999');
                isValid = false;
            }
        }

        // Shipping Cost - optional, must be non-negative
        var shippingCost = document.getElementById('shippingCost').value;
        if (shippingCost && shippingCost !== '') {
            var shippingValue = parseFloat(shippingCost);
            if (isNaN(shippingValue) || shippingValue < 0) {
                showFieldError('shippingCost', 'Shipping cost must be a non-negative number');
                isValid = false;
            } else if (shippingValue > 9999.99) {
                showFieldError('shippingCost', 'Shipping cost must not exceed 9,999.99');
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Deactivate product
     */
    async function deactivateProduct(productId) {
        try {
            var response = await fetch('/JCart/admin/products/' + productId, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ _method: 'DELETE', confirm: 'DELETE' }),
                credentials: 'include'
            });

            var data = await response.json();

            if (response.ok) {
                window.showToast('Product deactivated successfully', 'success');
                closeDeleteModal();
                loadProducts(pagination ? pagination.getCurrentPage() : 1, pagination ? pagination.getPageSize() : 15);
            } else {
                window.showToast(data.message || 'Failed to deactivate product', 'error');
            }

        } catch (error) {
            console.error('Deactivate error:', error);
            window.showToast('Network error. Please try again.', 'error');
        }
    }

    /**
     * Activate product
     */
    async function activateProduct(productId) {
        try {
            var response = await fetch('/JCart/admin/products/' + productId, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ _method: 'ACTIVATE', confirm: 'ACTIVATE' }),
                credentials: 'include'
            });

            var data = await response.json();

            if (response.ok) {
                window.showToast('Product activated successfully', 'success');
                closeActivateModal();
                loadProducts(pagination ? pagination.getCurrentPage() : 1, pagination ? pagination.getPageSize() : 15);
            } else {
                window.showToast(data.message || 'Failed to activate product', 'error');
            }

        } catch (error) {
            console.error('Activate error:', error);
            window.showToast('Network error. Please try again.', 'error');
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function showFieldError(fieldId, message) {
        var field = document.getElementById(fieldId);
        if (field) {
            field.classList.add('error');
            var errorDiv = field.parentElement.querySelector('.error-message');
            if (errorDiv) {
                errorDiv.textContent = message;
                errorDiv.classList.add('show');
            }
        }
    }

    function clearFormErrors() {
        document.querySelectorAll('.form-group input, .form-group select').forEach(function (el) {
            el.classList.remove('error');
        });
        document.querySelectorAll('.error-message').forEach(function (el) {
            el.classList.remove('show');
        });
    }

    function showTableError(message) {
        document.getElementById('productTableBody').innerHTML = 
            '<tr class="empty-state"><td colspan="7">' + escapeHtml(message) + '</td></tr>';
    }

    /**
     * View product details in modal
     */
    window.viewProduct = function (productId) {
        var product = productsData.find(function (p) { return p.productId === productId; });
        if (!product) return;

        document.getElementById('viewProductName').textContent = product.productName || 'N/A';
        document.getElementById('viewProductId').textContent = product.productId;
        
        var categoryBadge = document.getElementById('viewCategoryBadge');
        categoryBadge.textContent = product.category || 'Uncategorized';

        document.getElementById('viewPrice').textContent = '$' + formatPrice(product.price);
        document.getElementById('viewDiscount').textContent = product.discount ? product.discount + '%' : 'None';
        document.getElementById('viewFinalPrice').textContent = '$' + formatPrice(product.finalPrice || product.price);
        document.getElementById('viewTaxRate').textContent = product.taxRate ? product.taxRate + '%' : 'N/A';

        document.getElementById('viewStockLevel').textContent = getStockLabel(product.stockLevel);
        
        var viewStatus = document.getElementById('viewStatus');
        viewStatus.className = 'status-badge ' + (product.isActive ? 'active' : 'inactive');
        viewStatus.innerHTML = '<span class="status-dot"></span>' + (product.isActive ? 'Active' : 'Inactive');

        document.getElementById('viewAgeGroup').textContent = product.ageGroup || 'N/A';
        document.getElementById('viewGender').textContent = product.gender || 'N/A';
        document.getElementById('viewSeasonality').textContent = product.seasonality || 'N/A';
        document.getElementById('viewLocation').textContent = product.location || 'N/A';

        document.getElementById('viewShippingCost').textContent = product.shippingCost ? '$' + formatPrice(product.shippingCost) : 'N/A';
        document.getElementById('viewShippingMethod').textContent = product.shippingMethod || 'N/A';

        document.getElementById('viewCreatedAt').textContent = formatDate(product.createdAt);
        document.getElementById('viewUpdatedAt').textContent = formatDate(product.updatedAt);

        // Show modal
        document.getElementById('viewModal').classList.add('active');
    };

    /**
     * Close view modal
     */
    function closeViewModal() {
        document.getElementById('viewModal').classList.remove('active');
    }

    /**
     * Format date
     */
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

})();

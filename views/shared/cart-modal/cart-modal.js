/**
 * Cart Modal Component - Add to Cart Confirmation Modal
 * 
 * Includes:
 * 1. Modal display with product confirmation after cart addition
 * 2. Real-time cart summary with item count and total calculation  
 * 3. Navigation options for Continue Shopping or View Cart
 * 4. Auto-refresh cart data from backend API integration
 * 5. Responsive design with overlay, animations, and keyboard support
 */

(function() {
    'use strict';

    const PLACEHOLDER_IMAGE = '/JCart/views/assets/image.svg';

    var modal = null;
    var overlay = null;

    /**
     * Initialize modal by loading HTML or binding events
     * Checks if modal exists in DOM, loads if not present
     */
    function init() {
        if (!document.getElementById('cartModal')) {
            loadModal();
        } else {
            bindEvents();
        }
    }

    /**
     * Asynchronously load modal HTML from external file
     * Fetches cart-modal.html and injects into DOM
     * @throws {Error} When modal HTML cannot be loaded
     */
    async function loadModal() {
        try {
            const response = await fetch('/JCart/views/shared/cart-modal/cart-modal.html');
            const html = await response.text();
            
            const container = document.createElement('div');
            container.innerHTML = html;
            document.body.appendChild(container.firstElementChild);
            
            bindEvents();
        } catch (error) {
            console.error('Error loading cart modal:', error);
        }
    }

    /**
     * Bind event listeners to modal elements
     * Sets up click handlers for close, continue, view cart, and overlay
     * Adds keyboard support for Escape key to close modal
     */
    function bindEvents() {
        modal = document.getElementById('cartModal');
        overlay = document.getElementById('cartModalOverlay');
        
        if (!modal) return;

        document.getElementById('cartModalClose')?.addEventListener('click', function() { hide(); });
        document.getElementById('cartModalContinue')?.addEventListener('click', function() { hide(); });
        overlay?.addEventListener('click', function() { hide(); });
        
        document.getElementById('cartModalViewCart')?.addEventListener('click', function() {
            hide();
            window.location.href = '/JCart/views/features/cart/';
        });

        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && !modal.classList.contains('hidden')) {
                hide();
            }
        });
    }

    /**
     * Display modal with product confirmation data
     * Shows modal, populates with product info, and updates cart summary
     * @param {Object} productData - Product information object
     * @param {string} productData.productName - Name of the product
     * @param {number} productData.price - Product price
     * @param {number} productData.quantity - Quantity added to cart
     * @param {string} productData.productId - Product ID
     */
    function show(productData) {
        if (!modal) return;

        populateModal(productData);
        
        modal.classList.remove('hidden');
        document.body.style.overflow = 'hidden';
        
        setTimeout(function() {
            document.getElementById('cartModalContinue')?.focus();
        }, 100);
    }

    /**
     * Hide modal and restore page scroll
     * Removes modal visibility and restores body overflow
     */
    function hide() {
        if (!modal) return;
        
        modal.classList.add('hidden');
        document.body.style.overflow = '';
    }

    /**
     * Populate modal with product confirmation data
     * Updates modal content with product details and refreshes cart summary
     * @param {Object} data - Product data to display in modal
     * @param {string} data.productName - Product name to display
     * @param {number} data.price - Product price
     * @param {number} data.quantity - Quantity added
     */
    function populateModal(data) {
        const productName = document.getElementById('cartModalProductName');
        const quantity = document.getElementById('cartModalQuantity');
        const price = document.getElementById('cartModalPrice');
        
        if (productName) productName.textContent = data.productName || 'Product';
        if (quantity) quantity.textContent = data.quantity || 1;
        if (price) price.textContent = (data.price || 0).toFixed(2);

        updateCartSummary();
    }

    /**
     * Update cart summary with latest data from backend
     * Fetches current cart data from API and calculates totals
     * Updates modal display with item count, subtotal, tax, shipping, and total
     * @async
     * @throws {Error} When cart data cannot be fetched from API
     */
    async function updateCartSummary() {
        try {
            const response = await fetch('/JCart/customer/cart', {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include'
            });
            
            if (response.ok) {
                const result = await response.json();
                const cartData = result.data || result;
                
                const totalElement = document.getElementById('cartModalTotal');
                const countElement = document.getElementById('cartModalItemCount');
                
                const items = cartData.items || [];
                const itemCount = items.length;
                const subtotal = parseFloat(cartData.totalAmount || 0);
                
                const tax = subtotal * 0.08;
                const shipping = subtotal > 100 ? 0 : (subtotal > 0 ? 10 : 0);
                const total = subtotal + tax + shipping;
                
                if (totalElement) totalElement.textContent = total.toFixed(2);
                if (countElement) countElement.textContent = itemCount;
            }
        } catch (error) {
            console.error('Error updating cart summary:', error);
        }
    }

    document.addEventListener('DOMContentLoaded', function() {
        init();
    });

    window.cartModal = {
        show: show,
        hide: hide,
        init: init
    };

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = {
            show: show,
            hide: hide,
            init: init
        };
    }
})();
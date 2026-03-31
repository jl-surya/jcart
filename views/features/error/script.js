/**
 * Error Page Handler
 * Handles different error scenarios and navigation
 */

(function () {
    'use strict';

    var ERROR_TYPES = {
        404: {
            title: 'Page Not Found',
            message: "The page you're looking for doesn't exist or has been moved.",
            icon: '<circle cx="12" cy="12" r="10"/><path d="m9 9 6 6"/><path d="m15 9-6 6"/>'
        },
        403: {
            title: 'Access Forbidden',
            message: "You don't have permission to access this page.",
            icon: '<rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><circle cx="12" cy="16" r="1"/><path d="m7 11V7a5 5 0 0 1 10 0v4"/>'
        },
        500: {
            title: 'Server Error',
            message: "Something went wrong on our end. Please try again later.",
            icon: '<circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>'
        },
        maintenance: {
            title: 'Under Maintenance',
            message: "We're currently performing maintenance. Please check back soon.",
            icon: '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>'
        }
    };

    /**
     * Initialize error page
     */
    function init() {
        loadNavbarAndFooter();
        setupEventListeners();
        configureErrorType();
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
     * Setup event listeners
     */
    function setupEventListeners() {
        var goBackBtn = document.getElementById('goBackBtn');
        if (goBackBtn) {
            goBackBtn.addEventListener('click', function () {
                if (window.history.length > 1) {
                    window.history.back();
                } else {
                    window.location.href = getHomeUrl();
                }
            });
        }

        var goHomeBtn = document.getElementById('goHomeBtn');
        if (goHomeBtn) {
            goHomeBtn.addEventListener('click', function (e) {
                e.preventDefault();
                window.location.href = getHomeUrl();
            });
        }
    }

    /**
     * Determine correct home URL based on context
     * Admin -> admin dashboard, Customer/guest -> main homepage
     */
    function getHomeUrl() {
        try {
            var isAdminLoggedIn = localStorage.getItem('adminLoggedIn') === 'true';
            if (isAdminLoggedIn) {
                return '/JCart/views/features/admin/dashboard/';
            }
        } catch (e) {
            // Fallback to home page
        }
        return '/JCart/views/index.html';
    }

    /**
     * Configure error type based on URL parameters or default to 404
     */
    function configureErrorType() {
        var urlParams = new URLSearchParams(window.location.search);
        var errorType = urlParams.get('type') || '404';
        var customMessage = urlParams.get('message');

        var config = ERROR_TYPES[errorType] || ERROR_TYPES['404'];
        
        updateErrorDisplay(errorType, config, customMessage);
        updatePageTitle(config.title);
        addErrorClass(errorType);
    }

    /**
     * Update error display elements
     * @param {string} errorType - Error type code
     * @param {Object} config - Error configuration
     * @param {string} customMessage - Optional custom message
     */
    function updateErrorDisplay(errorType, config, customMessage) {
        var errorIcon = document.getElementById('errorIcon');
        if (errorIcon && config.icon) {
            errorIcon.innerHTML = 
                '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
                config.icon + 
                '</svg>';
        }

        var errorTitle = document.getElementById('errorTitle');
        if (errorTitle) {
            errorTitle.textContent = config.title;
        }

        var errorMessage = document.getElementById('errorMessage');
        if (errorMessage) {
            errorMessage.textContent = customMessage || config.message;
        }

        var errorCode = document.getElementById('errorCode');
        if (errorCode) {
            if (errorType === 'maintenance') {
                errorCode.textContent = 'Maintenance Mode';
            } else {
                errorCode.textContent = 'Error ' + errorType;
            }
        }
    }

    /**
     * Update page title
     * @param {string} title - Error title
     */
    function updatePageTitle(title) {
        document.title = title + ' - JCart';
    }

    /**
     * Add error-specific CSS class
     * @param {string} errorType - Error type for styling
     */
    function addErrorClass(errorType) {
        var errorContent = document.querySelector('.error-content');
        if (errorContent && ERROR_TYPES[errorType]) {
            errorContent.classList.add('error-' + errorType);
        }
    }

    /**
     * Show error page with specific configuration
     * @param {string} type - Error type ('404', '403', '500', 'maintenance')
     * @param {string} message - Optional custom message
     */
    function showError(type, message) {
        var config = ERROR_TYPES[type] || ERROR_TYPES['404'];
        updateErrorDisplay(type, config, message);
        updatePageTitle(config.title);
        addErrorClass(type);
        
        var newUrl = window.location.pathname + '?type=' + type;
        if (message) {
            newUrl += '&message=' + encodeURIComponent(message);
        }
        window.history.replaceState({}, '', newUrl);
    }

    /**
     * Navigate to specific page with error handling
     * @param {string} url - URL to navigate to
     */
    function navigateWithFallback(url) {
        try {
            window.location.href = url;
        } catch (error) {
            console.error('Navigation error:', error);
            window.location.href = '/JCart/views/index.html';
        }
    }

    document.addEventListener('DOMContentLoaded', init);

    window.errorPage = {
        showError: showError,
        navigateWithFallback: navigateWithFallback
    };

    window.addEventListener('error', function (event) {
        console.error('Unhandled error:', event.error);
    });

    window.addEventListener('unhandledrejection', function (event) {
        console.error('Unhandled promise rejection:', event.reason);
    });
})();
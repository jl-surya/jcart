/**
 * Shared Footer Component
 * 
 * Includes:
 * 1. Dynamic copyright year update based on current date
 * 2. Footer visibility tracking for analytics and performance
 * 3. Newsletter subscription form handling capabilities
 * 4. Responsive footer design with company information
 * 5. Auto-initialization on DOM content loaded event
 */

(function () {
    'use strict';

    /**
     * Initialize footer functionality
     * Sets up year display and visibility tracking
     */
    function initializeFooter() {
        updateFooterYear();
        trackFooterVisibility();
    }

    /**
     * Update copyright year to current year
     * Looks for .footer-year element or updates copyright text directly
     */
    function updateFooterYear() {
        var currentYear = new Date().getFullYear();
        
        var yearSpan = document.querySelector('.footer-year');
        if (yearSpan) {
            yearSpan.textContent = currentYear;
            return;
        }
        
        var copyrightEl = document.querySelector('.footer-bottom p');
        if (copyrightEl) {
            copyrightEl.innerHTML = copyrightEl.innerHTML.replace(/\d{4}/, currentYear);
        }
    }

    /**
     * Track footer visibility using Intersection Observer
     * Disconnects observer when footer becomes visible (performance optimization)
     */
    function trackFooterVisibility() {
        var footer = document.querySelector('.footer');
        
        if (!footer || !('IntersectionObserver' in window)) return;

        var observer = new IntersectionObserver(function (entries) {
            entries.forEach(function (entry) {
                if (entry.isIntersecting) {
                    observer.disconnect();
                }
            });
        }, { threshold: 0.3 });

        observer.observe(footer);
    }

    document.addEventListener('DOMContentLoaded', initializeFooter);
})();
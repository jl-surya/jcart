/**
 * Shared Pagination Component
 * Reusable pagination logic for list views
 * 
 * Includes:
 * 1. Dynamic pagination controls with customizable page ranges
 * 2. Previous/Next navigation with disabled state handling
 * 3. Page number buttons with active state highlighting
 * 4. Responsive design that adapts to different screen sizes
 * 5. Callback integration for seamless page change handling
 */

(function () {
    'use strict';

    /**
     * Pagination constructor - Initialize pagination instance
     * @param {Object} options - Configuration options with onPageChange callback
     */
    function Pagination(options) {
        this.options = options || {};
        this.currentPage = 1;
        this.totalPages = 1;
        this.totalItems = 0;
        this.pageSize = 15;
        this.maxVisiblePages = 5;

        this.container = null;
        this.wrapper = null;
        this.prevBtn = null;
        this.nextBtn = null;
        this.pagesContainer = null;
        this.summaryEl = null;
        this.sizeSelect = null;
        this.initialized = false;

        this.onPageChange = options.onPageChange || function () {};
    }

    /**
     * Initialize pagination component with DOM elements and event listeners
     * @returns {boolean} - True if initialized successfully, false if container not found
     */
    Pagination.prototype.init = function () {
        var self = this;

        this.container = document.getElementById('paginationContainer');
        this.wrapper = document.getElementById('paginationWrapper');
        this.prevBtn = document.getElementById('paginationPrev');
        this.nextBtn = document.getElementById('paginationNext');
        this.pagesContainer = document.getElementById('paginationPages');
        this.summaryEl = document.getElementById('paginationSummary');
        this.sizeSelect = document.getElementById('paginationSize');

        if (!this.container) {
            return false;
        }

        if (this.prevBtn) {
            var newPrevBtn = this.prevBtn.cloneNode(true);
            this.prevBtn.parentNode.replaceChild(newPrevBtn, this.prevBtn);
            this.prevBtn = newPrevBtn;
            this.prevBtn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                if (self.currentPage > 1) {
                    self.goToPage(self.currentPage - 1);
                }
            });
        }

        if (this.nextBtn) {
            var newNextBtn = this.nextBtn.cloneNode(true);
            this.nextBtn.parentNode.replaceChild(newNextBtn, this.nextBtn);
            this.nextBtn = newNextBtn;
            this.nextBtn.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                if (self.currentPage < self.totalPages) {
                    self.goToPage(self.currentPage + 1);
                }
            });
        }

        if (this.sizeSelect) {
            var newSizeSelect = this.sizeSelect.cloneNode(true);
            this.sizeSelect.parentNode.replaceChild(newSizeSelect, this.sizeSelect);
            this.sizeSelect = newSizeSelect;
            this.sizeSelect.value = this.pageSize;
            this.sizeSelect.addEventListener('change', function (e) {
                e.preventDefault();
                self.pageSize = parseInt(this.value, 10);
                self.currentPage = 1;
                self.onPageChange(1, self.pageSize);
            });
        }

        this.initialized = true;
        return true;
    };

    /**
     * Update pagination state with new data
     * @param {Object} data - Pagination data (currentPage, totalPages, totalItems, pageSize)
     */
    Pagination.prototype.update = function (data) {
        this.currentPage = data.currentPage || 1;
        this.totalPages = data.totalPages || 1;
        this.totalItems = data.totalItems || 0;
        this.pageSize = data.pageSize || this.pageSize;

        this.render();
    };

    /**
     * Render pagination controls based on current state
     */
    Pagination.prototype.render = function () {
        var self = this;

        if (!this.container) return;

        if (this.prevBtn) {
            this.prevBtn.disabled = this.currentPage <= 1;
        }
        if (this.nextBtn) {
            this.nextBtn.disabled = this.currentPage >= this.totalPages;
        }

        if (this.summaryEl) {
            var actualItems = this.options.actualItemsCount || 0;
            var start = (this.currentPage - 1) * this.pageSize + 1;
            var end;
            
            if (actualItems === 0 && this.totalItems === 0) {
                this.summaryEl.textContent = 'No results';
            } else if (this.totalItems > 0) {
                end = Math.min(this.currentPage * this.pageSize, this.totalItems);
                this.summaryEl.textContent = 'Showing ' + start + '-' + end + ' of ' + this.totalItems;
            } else if (actualItems > 0) {
                end = start + actualItems - 1;
                this.summaryEl.textContent = 'Showing ' + start + '-' + end + ' (Page ' + this.currentPage + ' of ' + this.totalPages + ')';
            } else {
                this.summaryEl.textContent = 'Page ' + this.currentPage + ' of ' + this.totalPages;
            }
        }

        if (this.sizeSelect) {
            this.sizeSelect.value = this.pageSize;
        }

        if (this.pagesContainer) {
            this.pagesContainer.innerHTML = '';

            var pages = this.getVisiblePages();
            for (var i = 0; i < pages.length; i++) {
                var page = pages[i];
                if (page === '...') {
                    var ellipsis = document.createElement('span');
                    ellipsis.className = 'page-btn ellipsis';
                    ellipsis.textContent = '...';
                    this.pagesContainer.appendChild(ellipsis);
                } else {
                    var btn = document.createElement('button');
                    btn.type = 'button';
                    btn.className = 'page-btn' + (page === this.currentPage ? ' active' : '');
                    btn.textContent = page;
                    btn.setAttribute('data-page', page);
                    (function(pageNum) {
                        btn.addEventListener('click', function (e) {
                            e.preventDefault();
                            e.stopPropagation();
                            self.goToPage(pageNum);
                        });
                    })(page);
                    this.pagesContainer.appendChild(btn);
                }
            }
        }

        var hasContent = this.totalPages > 0 || actualItems > 0;
        if (this.wrapper) {
            this.wrapper.style.display = hasContent ? 'block' : 'none';
        }
        if (this.container) {
            this.container.style.display = hasContent ? 'flex' : 'none';
        }
    };

    /**
     * Calculate which page numbers to show in pagination
     * @returns {Array} - Array of page numbers to display
     */
    Pagination.prototype.getVisiblePages = function () {
        var pages = [];
        var total = this.totalPages;
        var current = this.currentPage;
        var maxVisible = this.maxVisiblePages;

        if (total <= 0) return [];
        
        if (total === 1) {
            return [1];
        }

        if (total <= maxVisible + 2) {
            for (var i = 1; i <= total; i++) {
                pages.push(i);
            }
        } else {
            var start = Math.max(1, current - Math.floor(maxVisible / 2));
            var end = Math.min(total, start + maxVisible - 1);

            if (end - start + 1 < maxVisible) {
                start = Math.max(1, end - maxVisible + 1);
            }

            if (start > 1) {
                pages.push(1);
                if (start > 2) {
                    pages.push('...');
                }
            }

            for (var j = start; j <= end; j++) {
                pages.push(j);
            }

            if (end < total) {
                if (end < total - 1) {
                    pages.push('...');
                }
                pages.push(total);
            }
        }

        return pages;
    };

    /**
     * Navigate to specific page
     * @param {number} page - Page number to navigate to
     */
    Pagination.prototype.goToPage = function (page) {
        if (page < 1 || page > this.totalPages || page === this.currentPage) return;

        this.currentPage = page;
        this.render();
        this.onPageChange(page, this.pageSize);
    };

    Pagination.prototype.getPageSize = function () {
        return this.pageSize;
    };

    Pagination.prototype.getCurrentPage = function () {
        return this.currentPage;
    };

    window.Pagination = Pagination;
})();


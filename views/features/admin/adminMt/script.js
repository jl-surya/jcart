/**
 * Admin Management Handler
 * Manages admin CRUD operations, permissions, and table display with filters and pagination.
 */

(function () {
    'use strict';

    var adminsData = [];
    var editingAdminId = null;
    var pagination = null;
    var currentPermissions = [];

    var PERM_ADMIN_VIEW = 'admins:view';
    var PERM_ADMIN_CREATE = 'admins:create';
    var PERM_ADMIN_UPDATE = 'admins:update';
    var PERM_ADMIN_DELETE = 'admins:delete';

    var searchTerm = '';
    var roleFilter = '';
    var statusFilter = '';
    var sortBy = '';
    var sortDir = 'desc';

    document.addEventListener('DOMContentLoaded', function () {
        loadCurrentAdminPermissions();
        applyPermissionBasedUI();
        initializePagination();
        initializeEventListeners();
        loadAdmins(1, 15);
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
        if (!hasPermission(PERM_ADMIN_VIEW)) {
            showPermissionError();
            return;
        }
        
        var addAdminBtn = document.getElementById('addAdminBtn');
        if (addAdminBtn && !hasPermission(PERM_ADMIN_CREATE)) {
            addAdminBtn.style.display = 'none';
        }
    }

    /**
     * Show permission error and redirect
     */
    function showPermissionError() {
        window.showToast('You do not have permission to view admins', 'error');
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
                        loadAdmins(page, size);
                    }
                });
                pagination.init();
            })
            .catch(function (error) {
                console.error('Error loading pagination:', error);
            });
    }

    /**
     * Initialize all event listeners
     */
    function initializeEventListeners() {
        var addAdminBtn = document.getElementById('addAdminBtn');
        if (addAdminBtn) {
            addAdminBtn.addEventListener('click', openAddModal);
        }

        var modalClose = document.getElementById('modalClose');
        var cancelBtn = document.getElementById('cancelBtn');
        if (modalClose) modalClose.addEventListener('click', closeModal);
        if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

        var deleteModalClose = document.getElementById('deleteModalClose');
        var deleteCancelBtn = document.getElementById('deleteCancelBtn');
        if (deleteModalClose) deleteModalClose.addEventListener('click', closeDeleteModal);
        if (deleteCancelBtn) deleteCancelBtn.addEventListener('click', closeDeleteModal);

        var adminForm = document.getElementById('adminForm');
        if (adminForm) {
            adminForm.addEventListener('submit', handleFormSubmit);
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

        var searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') {
                    applyFilters();
                }
            });
        }

        var viewModalClose = document.getElementById('viewModalClose');
        var viewCloseBtn = document.getElementById('viewCloseBtn');
        if (viewModalClose) viewModalClose.addEventListener('click', closeViewModal);
        if (viewCloseBtn) viewCloseBtn.addEventListener('click', closeViewModal);

        var adminModal = document.getElementById('adminModal');
        var deleteModal = document.getElementById('deleteModal');
        var viewModal = document.getElementById('viewModal');
        if (adminModal) {
            adminModal.addEventListener('click', function (e) {
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
        
        initializePermissionDependencies();
    }
    
    /**
     * Initialize permission dependency logic
     * When CREATE/UPDATE/DELETE is selected, auto-select VIEW for that module
     * When VIEW is unselected, auto-unselect all other permissions for that module
     */
    function initializePermissionDependencies() {
        var permissionCheckboxes = document.querySelectorAll('input[name="permissions"]');
        
        permissionCheckboxes.forEach(function(checkbox) {
            checkbox.addEventListener('change', function() {
                handlePermissionChange(this);
            });
        });
    }
    
    /**
     * Handle permission checkbox change with dependency logic
     */
    function handlePermissionChange(checkbox) {
        var value = checkbox.value;
        var parts = value.split(':');
        var module = parts[0];
        var action = parts[1];
        
        if (checkbox.checked && action !== 'view') {
            var viewCheckbox = document.querySelector('input[name="permissions"][value="' + module + ':view"]');
            if (viewCheckbox && !viewCheckbox.checked) {
                viewCheckbox.checked = true;
            }
        }
        
        if (!checkbox.checked && action === 'view') {
            var moduleCheckboxes = document.querySelectorAll('input[name="permissions"][value^="' + module + ':"]');
            moduleCheckboxes.forEach(function(cb) {
                if (cb.value !== value) {
                    cb.checked = false;
                }
            });
        }
    }

    /**
     * Apply filters and load admins
     */
    function applyFilters() {
        var searchInput = document.getElementById('searchInput');
        var roleFilterEl = document.getElementById('roleFilter');
        var statusFilterEl = document.getElementById('statusFilter');
        var sortByFilterEl = document.getElementById('sortByFilter');
        var sortDirFilterEl = document.getElementById('sortDirFilter');

        searchTerm = searchInput ? searchInput.value.trim() : '';
        roleFilter = roleFilterEl ? roleFilterEl.value : '';
        statusFilter = statusFilterEl ? statusFilterEl.value : '';
        sortBy = sortByFilterEl ? sortByFilterEl.value : '';
        sortDir = sortDirFilterEl ? sortDirFilterEl.value : 'desc';

        loadAdmins(1, pagination ? pagination.getPageSize() : 15);
    }

    /**
     * Build query string with filters
     */
    function buildQueryString(page, size) {
        var params = [];
        params.push('page=' + page);
        params.push('size=' + size);
        
        if (searchTerm) {
            params.push('search=' + encodeURIComponent(searchTerm));
        }
        if (roleFilter) {
            params.push('role=' + encodeURIComponent(roleFilter));
        }
        if (statusFilter) {
            params.push('status=' + encodeURIComponent(statusFilter));
        }
        if (sortBy) {
            params.push('sortBy=' + encodeURIComponent(sortBy));
        }
        if (sortDir) {
            params.push('sortDir=' + encodeURIComponent(sortDir));
        }
        
        return params.join('&');
    }

    /**
     * Load admins from API with filters
     */
    async function loadAdmins(page, size) {
        showLoadingState();
        
        try {
            var searchRequest = buildSearchRequest(page, size);
            var response = await fetch('/JCart/admin/admins/search', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                credentials: 'include',
                body: JSON.stringify(searchRequest)
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
                throw new Error('Failed to load admins');
            }

            var data = await response.json();
            var result = data.data || data;
            
            adminsData = result.admins || [];
            
            updateStats(result.total, result.activeCount, result.inactiveCount);
            
            renderAdminTable();
            
            if (pagination) {
                pagination.update({
                    currentPage: result.page || page,
                    totalPages: result.totalPages || 1,
                    totalItems: result.total || 0,
                    pageSize: result.size || size
                });
            }

        } catch (error) {
            console.error('Error loading admins:', error);
            showTableError('Failed to load administrators');
        }
    }
    
    /**
     * Build search request object
     */
    function buildSearchRequest(page, size) {
        var request = {
            page: page,
            size: size
        };
        
        if (searchTerm) {
            request.search = searchTerm;
        }
        if (roleFilter) {
            request.role = roleFilter;
        }
        if (statusFilter) {
            request.status = statusFilter;
        }
        if (sortBy) {
            request.sortBy = sortBy;
        }
        if (sortDir) {
            request.sortDir = sortDir;
        }
        
        return request;
    }

    /**
     * Show loading state
     */
    function showLoadingState() {
        document.getElementById('adminTableBody').innerHTML = 
            '<tr class="loading-row"><td colspan="5">' +
            '<div class="loading-spinner"></div>' +
            '<span>Loading administrators...</span>' +
            '</td></tr>';
    }

    /**
     * Update stats cards
     */
    function updateStats(total, activeCount, inactiveCount) {
        document.getElementById('totalAdmins').textContent = total || 0;
        document.getElementById('activeAdmins').textContent = activeCount || 0;
        document.getElementById('inactiveAdmins').textContent = inactiveCount || 0;
    }

    /**
     * Render admin table
     */
    function renderAdminTable() {
        var tbody = document.getElementById('adminTableBody');
        var canUpdate = hasPermission(PERM_ADMIN_UPDATE);
        var canDelete = hasPermission(PERM_ADMIN_DELETE);
        
        if (adminsData.length === 0) {
            var emptyMessage = (searchTerm || roleFilter || statusFilter) 
                ? 'No administrators match your filters' 
                : 'No administrators found';
            tbody.innerHTML = '<tr class="empty-state"><td colspan="5">' +
                '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>' +
                '<p>' + emptyMessage + '</p></td></tr>';
            return;
        }

        var html = adminsData.map(function (admin) {
            var initials = getInitials(admin.username);
            var roleClass = admin.isSuperAdmin ? 'super-admin' : admin.role.toLowerCase();
            var roleName = admin.isSuperAdmin ? 'SUPER ADMIN' : admin.role;
            var permissions = admin.permissions || [];
            var permissionsHtml = renderPermissions(permissions, admin.isSuperAdmin);
            var isCurrentAdmin = isCurrentUser(admin.adminId);

            var actionButtons = '<button class="btn-icon view" onclick="viewAdmin(\'' + admin.adminId + '\')" title="View">' +
                '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>' +
            '</button>';

            // Edit button - only show if user has update permission
            if (canUpdate) {
                actionButtons += '<button class="btn-icon" onclick="editAdmin(\'' + admin.adminId + '\')" title="Edit" ' + 
                    (admin.isSuperAdmin || isCurrentAdmin ? 'disabled' : '') + '>' +
                    '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>' +
                '</button>';
            }

            // Delete button - only show if user has delete permission
            if (canDelete) {
                actionButtons += '<button class="btn-icon danger" onclick="confirmDelete(\'' + admin.adminId + '\', \'' + escapeHtml(admin.username) + '\')" title="Deactivate" ' + 
                    (admin.isSuperAdmin || isCurrentAdmin || !admin.isActive ? 'disabled' : '') + '>' +
                    '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>' +
                '</button>';
            }

            return '<tr>' +
                '<td>' +
                    '<div class="admin-info">' +
                        '<div class="admin-avatar ' + (admin.isSuperAdmin ? 'super' : '') + '">' + initials + '</div>' +
                        '<div class="admin-details">' +
                            '<span class="admin-name">' + escapeHtml(admin.username) + '</span>' +
                            '<span class="admin-email">' + escapeHtml(admin.email) + '</span>' +
                        '</div>' +
                    '</div>' +
                '</td>' +
                '<td><span class="role-badge ' + roleClass + '">' + roleName + '</span></td>' +
                '<td><div class="permissions-list">' + permissionsHtml + '</div></td>' +
                '<td>' +
                    '<span class="status-badge ' + (admin.isActive ? 'active' : 'inactive') + '">' +
                        '<span class="status-dot"></span>' +
                        (admin.isActive ? 'Active' : 'Inactive') +
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
     * Render permissions tags
     */
    function renderPermissions(permissions, isSuperAdmin) {
        if (isSuperAdmin) {
            return '<span class="permission-tag super-admin-tag">* (All Permissions)</span>';
        }
        
        if (!permissions || permissions.length === 0) {
            return '<span class="permission-tag">No permissions</span>';
        }

        var maxShow = 3;
        var shown = permissions.slice(0, maxShow);
        var remaining = permissions.length - maxShow;

        var html = shown.map(function (p) {
            return '<span class="permission-tag">' + formatPermission(p) + '</span>';
        }).join('');

        if (remaining > 0) {
            html += '<span class="permission-tag more" title="' + permissions.slice(maxShow).join(', ') + '">+' + remaining + ' more</span>';
        }

        return html;
    }

    /**
     * Format permission string
     */
    function formatPermission(perm) {
        var parts = perm.split(':');
        return parts[0].charAt(0).toUpperCase() + parts[0].slice(1) + ' ' + parts[1];
    }

    /**
     * Clear all filters
     */
    window.clearFilters = function () {
        searchTerm = '';
        roleFilter = '';
        statusFilter = '';
        sortBy = '';
        sortDir = 'desc';

        document.getElementById('searchInput').value = '';
        document.getElementById('roleFilter').value = '';
        document.getElementById('statusFilter').value = '';
        document.getElementById('sortByFilter').value = '';
        document.getElementById('sortDirFilter').value = 'desc';

        loadAdmins(1, pagination ? pagination.getPageSize() : 15);
    };

    /**
     * Open add admin modal
     */
    function openAddModal() {
        editingAdminId = null;
        document.getElementById('modalTitle').textContent = 'Add New Admin';
        document.getElementById('submitBtn').innerHTML = '<span>Create Admin</span>';
        document.getElementById('passwordRow').style.display = 'grid';
        document.getElementById('statusGroup').style.display = 'none';
        document.getElementById('adminForm').reset();
        clearFormErrors();
        document.getElementById('adminModal').classList.add('active');
    }

    /**
     * Open edit admin modal
     */
    window.editAdmin = function (adminId) {
        var admin = adminsData.find(function (a) { return a.adminId === adminId; });
        if (!admin) return;

        editingAdminId = adminId;
        document.getElementById('modalTitle').textContent = 'Edit Admin';
        document.getElementById('submitBtn').innerHTML = '<span>Save Changes</span>';
        document.getElementById('passwordRow').style.display = 'none';
        document.getElementById('statusGroup').style.display = 'block';

        document.getElementById('adminId').value = admin.adminId;
        document.getElementById('username').value = admin.username;
        document.getElementById('email').value = admin.email;
        document.getElementById('phone').value = admin.phone || '';
        document.getElementById('role').value = admin.role;
        document.getElementById('isActive').checked = admin.isActive;

        var checkboxes = document.querySelectorAll('input[name="permissions"]');
        checkboxes.forEach(function (cb) {
            cb.checked = admin.permissions && admin.permissions.indexOf(cb.value) !== -1;
        });

        clearFormErrors();
        document.getElementById('adminModal').classList.add('active');
    };

    /**
     * Close modal
     */
    function closeModal() {
        document.getElementById('adminModal').classList.remove('active');
        editingAdminId = null;
    }

    /**
     * Confirm delete/deactivate
     */
    window.confirmDelete = function (adminId, username) {
        document.getElementById('deleteAdminName').textContent = username;
        document.getElementById('deleteConfirmBtn').onclick = function () {
            deactivateAdmin(adminId);
        };
        document.getElementById('deleteModal').classList.add('active');
    };

    /**
     * Close delete modal
     */
    function closeDeleteModal() {
        document.getElementById('deleteModal').classList.remove('active');
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
            var url = '/JCart/admin/admins';
            var method = 'POST';

            if (editingAdminId) {
                url += '/' + editingAdminId;
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
                window.showToast(editingAdminId ? 'Admin updated successfully' : 'Admin created successfully', 'success');
                closeModal();
                loadAdmins(pagination ? pagination.getCurrentPage() : 1, pagination ? pagination.getPageSize() : 15);
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
        var permissions = [];
        document.querySelectorAll('input[name="permissions"]:checked').forEach(function (cb) {
            permissions.push(cb.value);
        });

        var data = {
            username: document.getElementById('username').value.trim(),
            email: document.getElementById('email').value.trim(),
            phone: document.getElementById('phone').value.trim() || null,
            role: document.getElementById('role').value,
            permissions: permissions
        };

        if (!editingAdminId) {
            data.password = document.getElementById('password').value;
            data.confirmPassword = document.getElementById('confirmPassword').value;
        } else {
            data.isActive = document.getElementById('isActive').checked;
        }

        return data;
    }

    /**
     * Validate form
     */
    function validateForm() {
        clearFormErrors();
        var isValid = true;

        // Username - required, 3-50 chars, alphanumeric and underscore only
        var username = document.getElementById('username').value.trim();
        if (!username) {
            showFieldError('username', 'Username is required');
            isValid = false;
        } else if (username.length < 3) {
            showFieldError('username', 'Username must be at least 3 characters');
            isValid = false;
        } else if (username.length > 50) {
            showFieldError('username', 'Username must not exceed 50 characters');
            isValid = false;
        } else if (!/^[a-zA-Z0-9_]+$/.test(username)) {
            showFieldError('username', 'Username can only contain letters, numbers, and underscores');
            isValid = false;
        }

        // Email - required, valid format
        var email = document.getElementById('email').value.trim();
        if (!email) {
            showFieldError('email', 'Email is required');
            isValid = false;
        } else if (!isValidEmail(email)) {
            showFieldError('email', 'Please enter a valid email address');
            isValid = false;
        } else if (email.length > 100) {
            showFieldError('email', 'Email must not exceed 100 characters');
            isValid = false;
        }

        var phone = document.getElementById('phone').value.trim();
        if (phone) {
            var cleanPhone = phone.replace(/[\s\-\(\)\.]/g, '');
            if (!/^[\+]?[0-9]{10,15}$/.test(cleanPhone)) {
                showFieldError('phone', 'Please enter a valid phone number (10-15 digits)');
                isValid = false;
            }
        }

        // Role - required
        var role = document.getElementById('role').value;
        if (!role) {
            showFieldError('role', 'Please select a role');
            isValid = false;
        }

        if (!editingAdminId) {
            var password = document.getElementById('password').value;
            var confirmPassword = document.getElementById('confirmPassword').value;

            if (!password) {
                showFieldError('password', 'Password is required');
                isValid = false;
            } else if (password.length < 6) {
                showFieldError('password', 'Password must be at least 6 characters');
                isValid = false;
            } else if (password.length > 100) {
                showFieldError('password', 'Password must not exceed 100 characters');
                isValid = false;
            } else {
                var hasUpper = /[A-Z]/.test(password);
                var hasLower = /[a-z]/.test(password);
                var hasNumber = /[0-9]/.test(password);
                
                if (!hasUpper || !hasLower || !hasNumber) {
                    showFieldError('password', 'Password must contain uppercase, lowercase, and number');
                    isValid = false;
                }
            }

            if (!confirmPassword) {
                showFieldError('confirmPassword', 'Please confirm your password');
                isValid = false;
            } else if (password !== confirmPassword) {
                showFieldError('confirmPassword', 'Passwords do not match');
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Deactivate admin
     */
    async function deactivateAdmin(adminId) {
        try {
            var response = await fetch('/JCart/admin/admins/' + adminId, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ _method: 'DELETE' }),
                credentials: 'include'
            });

            var data = await response.json();

            if (response.ok) {
                window.showToast('Admin deactivated successfully', 'success');
                closeDeleteModal();
                loadAdmins(pagination ? pagination.getCurrentPage() : 1, pagination ? pagination.getPageSize() : 15);
            } else {
                window.showToast(data.message || 'Failed to deactivate admin', 'error');
            }

        } catch (error) {
            console.error('Deactivate error:', error);
            window.showToast('Network error. Please try again.', 'error');
        }
    }

    function getInitials(name) {
        return name.substring(0, 2).toUpperCase();
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function isValidEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    function isCurrentUser(adminId) {
        try {
            var admin = JSON.parse(localStorage.getItem('admin') || '{}');
            return admin.adminId === adminId;
        } catch (e) {
            return false;
        }
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
        document.getElementById('adminTableBody').innerHTML = 
            '<tr class="empty-state"><td colspan="5">' + escapeHtml(message) + '</td></tr>';
    }

    /**
     * View admin details in modal
     */
    window.viewAdmin = function (adminId) {
        var admin = adminsData.find(function (a) { return a.adminId === adminId; });
        if (!admin) return;

        var initials = getInitials(admin.username);
        var viewAvatar = document.getElementById('viewAvatar');
        viewAvatar.textContent = initials;
        viewAvatar.className = 'view-avatar' + (admin.isSuperAdmin ? ' super' : '');

        document.getElementById('viewUsername').textContent = admin.username;
        document.getElementById('viewEmail').textContent = admin.email;
        document.getElementById('viewPhone').textContent = admin.phone || 'N/A';
        document.getElementById('viewAdminId').textContent = admin.adminId;

        var roleClass = admin.isSuperAdmin ? 'super-admin' : admin.role.toLowerCase();
        var roleName = admin.isSuperAdmin ? 'Super Admin' : admin.role;
        var viewRoleBadge = document.getElementById('viewRoleBadge');
        viewRoleBadge.textContent = roleName;
        viewRoleBadge.className = 'role-badge ' + roleClass;

        var viewStatus = document.getElementById('viewStatus');
        viewStatus.className = 'status-badge ' + (admin.isActive ? 'active' : 'inactive');
        viewStatus.innerHTML = '<span class="status-dot"></span>' + (admin.isActive ? 'Active' : 'Inactive');

        var permissionsHtml;
        if (admin.isSuperAdmin) {
            permissionsHtml = '<div class="permission-tag-full super-admin-tag">* (All Permissions)</div>';
        } else if (!admin.permissions || admin.permissions.length === 0) {
            permissionsHtml = '<span class="permission-tag-full">No permissions assigned</span>';
        } else {
            permissionsHtml = admin.permissions.map(function (p) {
                return '<span class="permission-tag-full">' + formatPermission(p) + '</span>';
            }).join('');
        }
        document.getElementById('viewPermissions').innerHTML = permissionsHtml;

        document.getElementById('viewCreatedAt').textContent = formatDate(admin.createdAt);
        document.getElementById('viewUpdatedAt').textContent = formatDate(admin.updatedAt);

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

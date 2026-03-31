/**
 * Admin Login Form Handler
 * Manages admin login form interactions, validation, and communication with the server.
 */

(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var loginForm = document.getElementById('loginForm');
        var usernameOrEmailInput = document.getElementById('usernameOrEmail');
        var passwordInput = document.getElementById('password');
        var submitButton = loginForm ? loginForm.querySelector('button[type="submit"]') : null;

        if (!loginForm || !usernameOrEmailInput || !passwordInput || !submitButton) {
            console.error('Admin Login: Required form elements not found.');
            return;
        }

        usernameOrEmailInput.addEventListener('input', function () { removeError(usernameOrEmailInput); });
        passwordInput.addEventListener('input', function () { removeError(passwordInput); });

        function showError(input, message) {
            var errorDiv = input.parentElement.querySelector('.error-message');
            if (errorDiv) {
                errorDiv.textContent = message;
                errorDiv.classList.add('show');
            }
            input.classList.add('error');
        }

        function removeError(input) {
            var errorDiv = input.parentElement.querySelector('.error-message');
            if (errorDiv) errorDiv.classList.remove('show');
            input.classList.remove('error');
        }

        function showButtonLoading() {
            var originalText = submitButton.innerHTML;
            submitButton.innerHTML = '<span class="loading"></span> Signing in...';
            submitButton.disabled = true;
            return originalText;
        }

        function hideButtonLoading(originalText) {
            submitButton.innerHTML = originalText;
            submitButton.disabled = false;
        }

        /**
         * Validate form inputs before submission
         * @returns {boolean} True if valid, false otherwise
         */
        function validateForm() {
            var usernameOrEmail = usernameOrEmailInput.value.trim();
            var password = passwordInput.value;
            var isValid = true;

            if (!usernameOrEmail) {
                showError(usernameOrEmailInput, 'Username or email is required');
                isValid = false;
            } else if (usernameOrEmail.length < 3) {
                showError(usernameOrEmailInput, 'Must be at least 3 characters');
                isValid = false;
            }

            if (!password) {
                showError(passwordInput, 'Password is required');
                isValid = false;
            } else if (password.length < 6) {
                showError(passwordInput, 'Password must be at least 6 characters');
                isValid = false;
            }

            return isValid;
        }

        loginForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            if (!validateForm()) return;

            var usernameOrEmail = usernameOrEmailInput.value.trim();
            var password = passwordInput.value;
            var originalButtonText = showButtonLoading();

            try {
                var response = await fetch('/JCart/admin/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        usernameOrEmail: usernameOrEmail, 
                        password: password 
                    }),
                    credentials: 'include'
                });

                if (response.ok) {
                    var data = await response.json();

                    if (data.data && data.data.admin) {
                        localStorage.setItem('admin', JSON.stringify(data.data.admin));
                    } else {
                        // Fallback if admin data not provided
                        var adminName = usernameOrEmail.includes('@')
                            ? usernameOrEmail.split('@')[0]
                            : usernameOrEmail;
                        
                        localStorage.setItem('admin', JSON.stringify({
                            email: usernameOrEmail,
                            name: adminName,
                            role: 'ADMIN',
                            loginTime: new Date().toISOString(),
                            permissions: [],
                            isSuperAdmin: false
                        }));
                    }
                    localStorage.setItem('adminLoggedIn', 'true');

                    window.showToast('Login successful! Redirecting...', 'success');
                    loginForm.reset();

                    setTimeout(function () {
                        window.location.href = '/JCart/views/features/admin/dashboard/';
                    }, 800);
                } else {
                    var errorMessage = 'Invalid credentials. Please try again.';
                    try {
                        var errorData = await response.json();
                        errorMessage = errorData.message || errorMessage;
                    } catch (_e) { }

                    if (errorMessage.toLowerCase().indexOf('username') !== -1 ||
                        errorMessage.toLowerCase().indexOf('email') !== -1) {
                        showError(usernameOrEmailInput, errorMessage);
                    } else {
                        showError(passwordInput, errorMessage);
                    }

                    window.showToast(errorMessage, 'error');
                }
            } catch (error) {
                console.error('Admin login error:', error);
                window.showToast('Network error. Please check your connection and try again.', 'error');
            } finally {
                hideButtonLoading(originalButtonText);
            }
        });
    });
})();

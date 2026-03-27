/**
 * Registration Form Handler
 * Manages customer registration form interactions, validation, and communication with the server.
 */

(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var termsLink = document.getElementById('termsLink');
        var termsPopup = document.getElementById('termsPopup');
        var cancelPopup = document.getElementById('cancelPopup');
        var closePopup = document.getElementById('closePopup');
        var agreeTerms = document.getElementById('agreeTerms');
        var registerForm = document.getElementById('registerForm');
        var submitButton = registerForm ? registerForm.querySelector('button[type="submit"]') : null;

        var usernameInput = document.getElementById('username');
        var emailInput = document.getElementById('email');
        var phoneInput = document.getElementById('phone');
        var passwordInput = document.getElementById('password');
        var confirmPasswordInput = document.getElementById('confirmPassword');

        if (!registerForm || !submitButton) {
            console.error('Register: Required form elements not found.');
            return;
        }

        usernameInput.addEventListener('input', function () { validateField(usernameInput); });
        emailInput.addEventListener('input', function () { validateField(emailInput); });
        phoneInput.addEventListener('input', function () { validateField(phoneInput); });
        passwordInput.addEventListener('input', function () {
            validateField(passwordInput);
            updatePasswordStrength(passwordInput.value);
            if (confirmPasswordInput.value) validatePasswordMatch();
        });
        confirmPasswordInput.addEventListener('input', function () { validatePasswordMatch(); });

        /**
         * Validate individual form field based on field type and rules
         * @param {HTMLElement} input - Input element to validate
         * @returns {boolean} - True if valid, false otherwise
         */
        function validateField(input) {
            var value = input.value.trim();
            var isValid = true;
            var errorMessage = '';

            switch (input.id) {
                case 'username':
                    if (!value) {
                        errorMessage = 'Full name is required';
                        isValid = false;
                    } else if (value.length < 2) {
                        errorMessage = 'Full name must be at least 2 characters';
                        isValid = false;
                    } else if (value.length > 50) {
                        errorMessage = 'Full name must be less than 50 characters';
                        isValid = false;
                    }
                    break;
                case 'email':
                    var emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                    if (!value) {
                        errorMessage = 'Email is required';
                        isValid = false;
                    } else if (!emailRegex.test(value)) {
                        errorMessage = 'Please enter a valid email address';
                        isValid = false;
                    }
                    break;
                case 'phone':
                    var phoneRegex = /^\d{10}$/;
                    if (!value) {
                        errorMessage = 'Phone number is required';
                        isValid = false;
                    } else if (!phoneRegex.test(value)) {
                        errorMessage = 'Please enter a valid 10-digit phone number';
                        isValid = false;
                    }
                    break;
                case 'password':
                    if (!value) {
                        errorMessage = 'Password is required';
                        isValid = false;
                    } else if (value.length < 6) {
                        errorMessage = 'Password must be at least 6 characters';
                        isValid = false;
                    }
                    break;
            }

            if (!isValid) {
                showError(input, errorMessage);
            } else {
                removeError(input);
            }
            return isValid;
        }

        /**
         * Validate password confirmation matches password
         * Shows/hides error for confirm password field
         */
        function validatePasswordMatch() {
            var password = passwordInput.value;
            var confirm = confirmPasswordInput.value;
            if (confirm) {
                if (password !== confirm) {
                    showError(confirmPasswordInput, 'Passwords do not match');
                    return false;
                } else {
                    removeError(confirmPasswordInput);
                    return true;
                }
            }
            return true;
        }

        /**
         * Update password strength indicator based on password complexity
         * @param {string} password - Password to analyze
         */
        function updatePasswordStrength(password) {
            var strengthEl = document.getElementById('passwordStrength');
            if (!strengthEl) return;

            strengthEl.className = 'password-strength';
            if (!password) return;

            var hasUpper = /[A-Z]/.test(password);
            var hasLower = /[a-z]/.test(password);
            var hasNumber = /[0-9]/.test(password);
            var hasSpecial = /[^A-Za-z0-9]/.test(password);
            var score = [hasUpper, hasLower, hasNumber, hasSpecial].filter(Boolean).length;

            if (password.length < 6) {
                strengthEl.classList.add('weak');
            } else if (password.length >= 8 && score >= 3) {
                strengthEl.classList.add('strong');
            } else {
                strengthEl.classList.add('medium');
            }
        }

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
            submitButton.innerHTML = '<span class="loading"></span> Creating account...';
            submitButton.disabled = true;
            return originalText;
        }

        function hideButtonLoading(originalText) {
            submitButton.innerHTML = originalText;
            submitButton.disabled = false;
        }

        /**
         * Validate entire registration form before submission
         * @returns {boolean} - True if all fields are valid, false otherwise
         */
        function validateForm() {
            var v1 = validateField(usernameInput);
            var v2 = validateField(emailInput);
            var v3 = validateField(phoneInput);
            var v4 = validateField(passwordInput);
            var v5 = validatePasswordMatch();
            return v1 && v2 && v3 && v4 && v5 && agreeTerms.checked;
        }

        if (termsLink) {
            termsLink.addEventListener('click', function (e) {
                e.preventDefault();
                termsPopup.classList.remove('hidden');
            });
        }

        if (closePopup) {
            closePopup.addEventListener('click', function () {
                termsPopup.classList.add('hidden');
                agreeTerms.disabled = false;
                agreeTerms.checked = true;
            });
        }

        if (cancelPopup) {
            cancelPopup.addEventListener('click', function () {
                termsPopup.classList.add('hidden');
            });
        }

        if (termsPopup) {
            termsPopup.addEventListener('click', function (e) {
                if (e.target === termsPopup) termsPopup.classList.add('hidden');
            });
        }

        registerForm.addEventListener('submit', async function (e) {
            e.preventDefault();

            if (!validateForm()) {
                if (!agreeTerms.checked) {
                    window.showToast('You must agree to the terms and conditions', 'error');
                }
                return;
            }

            var formData = {
                username: usernameInput.value.trim(),
                email: emailInput.value.trim(),
                phone: phoneInput.value.trim(),
                password: passwordInput.value,
                confirmPassword: confirmPasswordInput.value
            };

            var originalButtonText = showButtonLoading();

            try {
                var response = await fetch('/JCart/customer/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData),
                    credentials: 'include'
                });

                if (response.ok) {
                    var data = await response.json();

                    localStorage.setItem('isLoggedIn', 'true');
                    localStorage.setItem('user', JSON.stringify({
                        email: formData.email,
                        name: data.name || formData.username,
                        registeredAt: new Date().toISOString()
                    }));

                    window.showToast('Registration successful! Welcome to JCart!', 'success');
                    registerForm.reset();

                    setTimeout(function () {
                        window.location.href = '/JCart/views/index.html';
                    }, 1200);
                } else {
                    var errorMessage = 'Registration failed. Please try again.';
                    try {
                        var errorData = await response.json();
                        errorMessage = errorData.message || errorMessage;
                    } catch (_e) { }

                    window.showToast(errorMessage, 'error');

                    if (errorMessage.toLowerCase().indexOf('username') !== -1) {
                        showError(usernameInput, errorMessage);
                    } else if (errorMessage.toLowerCase().indexOf('email') !== -1) {
                        showError(emailInput, errorMessage);
                    } else if (errorMessage.toLowerCase().indexOf('phone') !== -1) {
                        showError(phoneInput, errorMessage);
                    }
                }
            } catch (error) {
                console.error('Registration error:', error);
                window.showToast('Network error. Please check your connection and try again.', 'error');
            } finally {
                hideButtonLoading(originalButtonText);
            }
        });
    });

    if (!document.getElementById('register-animations')) {
        var style = document.createElement('style');
        style.id = 'register-animations';
        style.textContent =
            '.loading{display:inline-block;width:18px;height:18px;border:2px solid rgba(255,255,255,0.3);' +
            'border-radius:50%;border-top-color:white;animation:spin 0.8s linear infinite}' +
            '@keyframes spin{to{transform:rotate(360deg)}}';
        document.head.appendChild(style);
    }
})();
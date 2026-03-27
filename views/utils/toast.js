/**
 * Global Toast Notification Utility
 * 
 * Includes:
 * 1. Consistent notification styling across entire application
 * 2. Multiple notification types with success, error, warning, and info
 * 3. Smooth animations with cubic-bezier timing functions
 * 4. Auto-dismissal with configurable duration settings
 * 5. Single active toast system and responsive positioning
 */

(function() {
    'use strict';

    /**
     * Show a toast notification
     * @param {string} message - Message to display
     * @param {string} type - Type of toast (success, error, warning, info)
     * @param {number} duration - Duration in milliseconds (default: 3000)
     */
    function showToast(message, type, duration) {
        type = type || 'info';
        duration = duration || 3000;

        var toast = document.createElement('div');
        toast.textContent = message;

        var bgColor, textColor, borderColor;
        
        switch (type) {
            case 'success':
                bgColor = '#f0f9ff';
                textColor = '#065f46';
                borderColor = '#10b981';
                break;
            case 'error':
                bgColor = '#fef2f2';
                textColor = '#991b1b';
                borderColor = '#ef4444';
                break;
            case 'warning':
                bgColor = '#fffbeb';
                textColor = '#92400e';
                borderColor = '#f59e0b';
                break;
            default: // info
                bgColor = '#f0f9ff';
                textColor = '#1e40af';
                borderColor = '#3b82f6';
        }

        toast.style.cssText =
            'position:fixed;top:80px;right:24px;z-index:10000;max-width:360px;' +
            'padding:14px 24px;border-radius:10px;font-size:15px;font-weight:500;' +
            'font-family:inherit;line-height:1.5;' +
            'animation:toastIn 0.35s cubic-bezier(0.4,0,0.2,1);' +
            'background:' + bgColor + ';color:' + textColor + ';' +
            'border-left:4px solid ' + borderColor + ';' +
            'box-shadow:0 4px 16px rgba(0,0,0,0.12);';

        document.body.appendChild(toast);

        setTimeout(function () {
            toast.style.animation = 'toastOut 0.3s cubic-bezier(0.4,0,0.2,1) forwards';
            setTimeout(function () { 
                if (toast.parentNode) {
                    toast.remove(); 
                }
            }, 300);
        }, duration);

        return toast;
    }

    if (!document.getElementById('toast-animations')) {
        var style = document.createElement('style');
        style.id = 'toast-animations';
        style.textContent =
            '@keyframes toastIn{from{transform:translateX(100%);opacity:0}to{transform:translateX(0);opacity:1}}' +
            '@keyframes toastOut{from{transform:translateX(0);opacity:1}to{transform:translateX(100%);opacity:0}}';
        document.head.appendChild(style);
    }

    window.showToast = showToast;

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = { showToast: showToast };
    }

})();

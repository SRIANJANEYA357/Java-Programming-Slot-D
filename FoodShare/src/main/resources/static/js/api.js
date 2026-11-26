/**
 * FoodShare - Frontend Core API Client & Global Helpers
 */

const API_BASE = '/api';

const FoodShare = {
    // HTML Escaping Utility to prevent Stored & Reflected Cross-Site Scripting (XSS)
    escapeHtml(str) {
        if (str === null || str === undefined) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    },

    // Auth Helpers
    getToken() {
        return localStorage.getItem('foodshare_token');
    },

    getUser() {
        const u = localStorage.getItem('foodshare_user');
        return u ? JSON.parse(u) : null;
    },

    saveAuth(authResponse) {
        localStorage.setItem('foodshare_token', authResponse.token);
        localStorage.setItem('foodshare_user', JSON.stringify({
            id: authResponse.id,
            email: authResponse.email,
            fullName: authResponse.fullName,
            role: authResponse.role,
            providerType: authResponse.providerType,
            organizationName: authResponse.organizationName
        }));
    },

    logout() {
        localStorage.removeItem('foodshare_token');
        localStorage.removeItem('foodshare_user');
        window.location.href = '/login.html';
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    hasRole(expectedRole) {
        const user = this.getUser();
        return user && user.role === expectedRole;
    },

    // HTTP Request Handler
    async request(endpoint, options = {}) {
        const url = `${API_BASE}${endpoint}`;
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const res = await fetch(url, { ...options, headers });
            const data = res.status !== 204 ? await res.json().catch(() => null) : null;

            if (!res.ok) {
                if (res.status === 401) {
                    FoodShare.showToast('Session expired. Please login again.', 'error');
                    setTimeout(() => FoodShare.logout(), 1500);
                }
                const errorMsg = (data && data.message) || (data && data.error) || `Request failed (${res.status})`;
                throw new Error(errorMsg);
            }
            return data;
        } catch (err) {
            console.error(`API Error on ${endpoint}:`, err);
            throw err;
        }
    },

    get(endpoint) {
        return this.request(endpoint, { method: 'GET' });
    },

    post(endpoint, body) {
        return this.request(endpoint, {
            method: 'POST',
            body: JSON.stringify(body)
        });
    },

    put(endpoint, body) {
        return this.request(endpoint, {
            method: 'PUT',
            body: JSON.stringify(body)
        });
    },

    delete(endpoint) {
        return this.request(endpoint, { method: 'DELETE' });
    },

    // Toast Notification System
    showToast(message, type = 'success') {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : type === 'warning' ? '⚠' : 'ℹ';
        toast.innerHTML = `<span style="font-weight: bold; font-size: 1.1rem;">${icon}</span> <span>${FoodShare.escapeHtml(message)}</span>`;

        container.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    },

    // Render Global Dynamic Navbar
    renderNav(activePage = '') {
        const nav = document.querySelector('.navbar');
        if (!nav) return;

        const user = this.getUser();
        let authHtml = '';

        if (user) {
            let dashboardUrl = '/user-dashboard.html';
            let roleLabel = 'Recipient';
            if (user.role === 'ROLE_ADMIN') {
                dashboardUrl = '/admin-dashboard.html';
                roleLabel = 'Admin';
            } else if (user.role === 'ROLE_PROVIDER') {
                dashboardUrl = '/provider-dashboard.html';
                roleLabel = user.providerType ? user.providerType.replace('_', ' ') : 'Provider';
            } else if (user.role === 'ROLE_VOLUNTEER') {
                dashboardUrl = '/volunteer-dashboard.html';
                roleLabel = 'Volunteer';
            }

            authHtml = `
                <div class="user-pill">
                    <span>${FoodShare.escapeHtml(user.fullName)}</span>
                    <span class="user-badge">${FoodShare.escapeHtml(roleLabel)}</span>
                </div>
                <a href="${dashboardUrl}" class="btn btn-sm btn-outline">Dashboard</a>
                <a href="/profile.html" class="btn btn-sm btn-secondary">Profile</a>
                <button onclick="FoodShare.logout()" class="btn btn-sm btn-secondary">Logout</button>
            `;
        } else {
            authHtml = `
                <a href="/login.html" class="btn btn-sm btn-outline">Login</a>
                <a href="/register.html" class="btn btn-sm btn-primary">Join FoodShare</a>
            `;
        }

        nav.innerHTML = `
            <div class="nav-container">
                <a href="/index.html" class="brand">
                    <div class="brand-icon">🌱</div>
                    <span>FoodShare</span>
                </a>
                <ul class="nav-links">
                    <li><a href="/index.html" class="${activePage === 'home' ? 'active' : ''}">Home</a></li>
                    <li><a href="/available-food.html" class="${activePage === 'available' ? 'active' : ''}">Find Food</a></li>
                    <li><a href="/distribution-history.html" class="${activePage === 'history' ? 'active' : ''}">Impact & History</a></li>
                    <li><a href="/about.html" class="${activePage === 'about' ? 'active' : ''}">About Us</a></li>
                </ul>
                <div class="nav-auth">
                    ${authHtml}
                </div>
            </div>
        `;
    },

    // Formatters
    formatCurrency(amount) {
        if (!amount || amount === 0) return 'FREE';
        return `₹${parseFloat(amount).toFixed(2)}`;
    },

    formatDate(dateString) {
        if (!dateString) return 'N/A';
        const d = new Date(dateString);
        return d.toLocaleDateString('en-IN', {
            day: 'numeric', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    },

    formatTimeRemaining(expiryString) {
        if (!expiryString) return 'Expired';
        const now = new Date();
        const exp = new Date(expiryString);
        const diffMs = exp - now;

        if (diffMs <= 0) return '<span style="color:var(--danger); font-weight:700;">Expired</span>';
        const hours = Math.floor(diffMs / (1000 * 60 * 60));
        const mins = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));

        if (hours > 24) {
            const days = Math.floor(hours / 24);
            return `${days}d ${hours % 24}h left`;
        }
        return `${hours}h ${mins}m left`;
    }
};

window.FoodShare = FoodShare;
window.escapeHtml = FoodShare.escapeHtml;

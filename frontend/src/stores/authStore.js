import {defineStore} from 'pinia';
import api from "../api/index"

const readStoredUser = () => {
    const storedUser = localStorage.getItem('user');

    if (!storedUser) {
        return null;
    }

    try {
        return JSON.parse(storedUser);
    } catch {
        localStorage.removeItem('user');
        return null;
    }
};

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('token') || null,
        user: readStoredUser(),
        loading: false,
        error: null
    }),

    getters: {
        isAuthenticated: (state) => !!state.token,
        isAdmin: (state) => state.user?.role === 'ADMIN',

        userInitials: (state) => {
            if (!state.user || !state.user.name) return '??';
            const first = state.user.name.firstName?.charAt(0) || '';
            const last = state.user.name.lastName?.charAt(0) || '';
            return (first + last).toUpperCase();
        }
    },

    actions: {
        async login(email, password) {
            this.loading = true;
            this.error = null;

            try {
                const response = await api.post('/api/v1/auth/login', {
                    email: email,
                    password: password
                });

                const { token } = response.data;

                this.token = token;
                this.user = { email, role: 'CUSTOMER' };

                localStorage.setItem('token', token);
                localStorage.setItem('user', JSON.stringify(this.user));

                return true;
            } catch (err) {
                this.error = err.response?.data?.message || "Invalid email or password";
                throw err;
            } finally {
                this.loading = false;
            }
        },

        async register(customerData) {
            this.loading = true;
            this.error = null;

            try {
                const response = await api.post('/api/v1/auth/register', customerData);

                const { token } = response.data;

                this.token = token;
                this.user = {
                    email: customerData.customerEmail,
                    name: customerData.name,
                    role: 'CUSTOMER'
                };

                localStorage.setItem('token', token);
                localStorage.setItem('user', JSON.stringify(this.user));

                return true;
            } catch (err) {
                this.error = err.response?.data?.message || "Registration failed. Email might be in use.";
                throw err;
            } finally {
                this.loading = false;
            }
        },
        logout() {
            this.token = null;
            this.user = null;
            this.error = null;

            localStorage.removeItem('token');
            localStorage.removeItem('user');
        }
    }
});

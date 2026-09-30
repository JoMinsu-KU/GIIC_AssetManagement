import axios from 'axios';
import { API_BASE_URL } from './config';

export const AUTH_SESSION_EXPIRED_EVENT = 'auth:session-expired';

const AUTH_ENDPOINTS_WITHOUT_REFRESH = [
    '/auth/login',
    '/auth/signup',
    '/auth/refresh',
    '/auth/logout',
];

let refreshRequest = null;

export const clearStoredAuthSession = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userName');
    localStorage.removeItem('userRole');
};

const notifySessionExpired = () => {
    clearStoredAuthSession();
    window.dispatchEvent(new Event(AUTH_SESSION_EXPIRED_EVENT));
};

const shouldRefreshRequest = (request) => {
    const requestUrl = request?.url || '';
    return !AUTH_ENDPOINTS_WITHOUT_REFRESH.some((endpoint) => requestUrl.endsWith(endpoint));
};

const requestNewAccessToken = async () => {
    if (!refreshRequest) {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
            throw new Error('No refresh token');
        }

        refreshRequest = axios
            .post(`${API_BASE_URL}/auth/refresh`, { refreshToken })
            .then((response) => {
                const tokenResponse = response.data?.data;
                if (!tokenResponse?.accessToken) {
                    throw new Error('Access token was not returned');
                }

                localStorage.setItem('accessToken', tokenResponse.accessToken);
                if (tokenResponse.refreshToken) {
                    localStorage.setItem('refreshToken', tokenResponse.refreshToken);
                }

                return tokenResponse.accessToken;
            })
            .finally(() => {
                refreshRequest = null;
            });
    }

    return refreshRequest;
};

const client = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: 10000,
});

// Request interceptor to add token
client.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('accessToken');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor to handle token refresh
client.interceptors.response.use(
    (response) => {
        return response.data;
    },
    async (error) => {
        const originalRequest = error.config;
        if (
            error.response?.status === 401
            && originalRequest
            && !originalRequest._retry
            && shouldRefreshRequest(originalRequest)
        ) {
            originalRequest._retry = true;
            try {
                const accessToken = await requestNewAccessToken();

                client.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
                originalRequest.headers = originalRequest.headers || {};
                originalRequest.headers['Authorization'] = `Bearer ${accessToken}`;

                return client(originalRequest);
            } catch (refreshError) {
                notifySessionExpired();
                return Promise.reject(refreshError);
            }
        }
        return Promise.reject(error);
    }
);

export default client;

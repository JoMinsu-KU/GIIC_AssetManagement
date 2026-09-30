import client, { clearStoredAuthSession } from './client';

export const login = async (employeeNo, password) => {
    const response = await client.post('/auth/login', { employeeNo, password });
    const { accessToken, refreshToken } = response.data;

    if (accessToken) {
        localStorage.setItem('accessToken', accessToken);
    }
    if (refreshToken) {
        localStorage.setItem('refreshToken', refreshToken);
    }

    return response;
};

export const signup = async (userData) => {
    const response = await client.post('/auth/signup', userData);
    return response;
};

export const logout = async () => {
    try {
        await client.post('/auth/logout');
    } catch (error) {
        console.error('Logout failed:', error);
    } finally {
        clearStoredAuthSession();
    }
};

export const refreshToken = async () => {
    const currentRefreshToken = localStorage.getItem('refreshToken');
    if (!currentRefreshToken) {
        throw new Error('No refresh token available');
    }

    const response = await client.post('/auth/refresh', { refreshToken: currentRefreshToken });
    const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data;

    if (newAccessToken) {
        localStorage.setItem('accessToken', newAccessToken);
    }
    if (newRefreshToken) {
        localStorage.setItem('refreshToken', newRefreshToken);
    }

    return response.data;
};

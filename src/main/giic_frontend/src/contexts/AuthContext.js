import React, { createContext, useContext, useState, useEffect } from 'react';
import * as authApi from '../api/auth';
import { AUTH_SESSION_EXPIRED_EVENT, clearStoredAuthSession } from '../api/client';
import SessionExtensionModal from '../components/common/SessionExtensionModal';
import { parseJwt } from '../utils/jwt';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [loading, setLoading] = useState(true);
    const [showSessionModal, setShowSessionModal] = useState(false);
    const [sessionTimeRemaining, setSessionTimeRemaining] = useState(0);

    useEffect(() => {
        let isActive = true;

        const applyAuthenticatedUser = (name, role) => {
            setIsAuthenticated(true);
            setUser(name && role ? { name, role } : null);
        };

        const checkAuth = async () => {
            const token = localStorage.getItem('accessToken');
            const decoded = token ? parseJwt(token) : null;
            const hasValidToken = decoded?.exp && decoded.exp * 1000 > Date.now();

            if (hasValidToken) {
                const name = localStorage.getItem('userName');
                const role = localStorage.getItem('userRole');
                if (isActive) {
                    applyAuthenticatedUser(name, role);
                }
            } else if (localStorage.getItem('refreshToken')) {
                try {
                    const refreshedUser = await authApi.refreshToken();
                    if (isActive) {
                        applyAuthenticatedUser(refreshedUser.name, refreshedUser.role);
                        localStorage.setItem('userName', refreshedUser.name);
                        localStorage.setItem('userRole', refreshedUser.role);
                    }
                } catch (error) {
                    clearStoredAuthSession();
                    if (isActive) {
                        setIsAuthenticated(false);
                        setUser(null);
                    }
                }
            } else {
                clearStoredAuthSession();
                if (isActive) {
                    setIsAuthenticated(false);
                    setUser(null);
                }
            }

            if (isActive) {
                setLoading(false);
            }
        };

        checkAuth();
        return () => {
            isActive = false;
        };
    }, []);

    useEffect(() => {
        const handleSessionExpired = () => {
            setIsAuthenticated(false);
            setUser(null);
            setShowSessionModal(false);
        };

        window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, handleSessionExpired);
        return () => window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, handleSessionExpired);
    }, []);

    useEffect(() => {
        if (!isAuthenticated) {
            setShowSessionModal(false);
            return;
        }

        const interval = setInterval(() => {
            const token = localStorage.getItem('accessToken');
            if (!token) return;

            const decoded = parseJwt(token);
            if (!decoded || !decoded.exp) return;

            const expTime = decoded.exp * 1000;
            const timeLeft = expTime - Date.now();

            if (timeLeft <= 0) {
                // Token expired
                logout();
            } else if (timeLeft < 300000) { // Warn 5 minutes before expiration (300000 ms)
                setShowSessionModal(true);
                setSessionTimeRemaining(timeLeft);
            } else {
                if (showSessionModal) setShowSessionModal(false);
            }
        }, 1000);

        return () => clearInterval(interval);
    }, [isAuthenticated, showSessionModal]);

    const login = async (employeeNo, password) => {
        try {
            const response = await authApi.login(employeeNo, password);
            if (response.success) {
                const { role, name } = response.data; // Fixed to match API response structure
                setIsAuthenticated(true);
                setUser({ name, role });
                localStorage.setItem('userName', name);
                localStorage.setItem('userRole', role);
            }
            return response;
        } catch (error) {
            console.error("Login failed", error);
            throw error;
        }
    };

    const logout = async () => {
        try {
            await authApi.logout();
        } finally {
            setIsAuthenticated(false);
            setUser(null);
            setShowSessionModal(false);
            localStorage.removeItem('userName');
            localStorage.removeItem('userRole');
            // authApi.logout() already removes tokens
        }
    };

    const signup = async (userData) => {
        return await authApi.signup(userData);
    };

    const extendSession = async () => {
        try {
            await authApi.refreshToken();
            setShowSessionModal(false);
        } catch (error) {
            console.error("Failed to extend session:", error);
            logout();
        }
    };

    return (
        <AuthContext.Provider value={{ user, isAuthenticated, loading, login, logout, signup }}>
            {children}
            <SessionExtensionModal
                show={showSessionModal}
                onExtend={extendSession}
                onLogout={logout}
                remainingTime={sessionTimeRemaining}
            />
        </AuthContext.Provider>
    );
};

export const useAuth = () => useContext(AuthContext);

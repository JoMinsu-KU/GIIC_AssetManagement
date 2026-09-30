// 사용자 역할 확인 유틸리티

export const isAdmin = (user) => {
    if (!user) return false;
    return user.role === 'ADMIN' || user.role === 'ROLE_ADMIN' || user.role === 'SUPER_ADMIN' || user.role === 'ROLE_SUPER_ADMIN' || user.role === 'SUPER' || user.role === 'ROLE_SUPER';
};

export const isSuper = (user) => {
    if (!user) return false;
    return user.role === 'SUPER_ADMIN' || user.role === 'ROLE_SUPER_ADMIN' || user.role === 'SUPER' || user.role === 'ROLE_SUPER';
};

export const isUser = (user) => {
    if (!user) return false;
    return user.role === 'USER' || user.role === 'ROLE_USER';
};

export const getUserRole = (user) => {
    if (!user || !user.role) return null;
    // 'ROLE_' 접두사 제거
    return user.role.replace('ROLE_', '');
};

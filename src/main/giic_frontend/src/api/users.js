import client from './client';

export const getCurrentUser = async () => {
    const response = await client.get('/users/me');
    return response.data;
};

export const getUsers = async (params, signal) => {
    const response = await client.get('/users', { params, signal });
    return response.data;
};

export const approveUser = async (employeeNo) => {
    return await client.put(`/users/${employeeNo}/approve`);
};

export const rejectUser = async (employeeNo) => {
    return await client.put('/users/reject', { employeeNo });
};

export const updateUserRole = async (employeeNo, role) => {
    return await client.put(`/users/${employeeNo}/role`, { role });
};

export const deactivateUser = async (employeeNo) => {
    return await client.put(`/users/${employeeNo}/deactivate`);
};

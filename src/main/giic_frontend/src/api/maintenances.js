import client from './client';

// Get maintenance list (permission-based)
export const getMaintenanceList = async () => {
    const response = await client.get('/maintenances');
    return response;
};

// Create maintenance request
export const createMaintenanceRequest = async (data) => {
    const response = await client.post('/maintenances', data);
    return response;
};

// Approve maintenance (ADMIN/SUPER)
export const approveMaintenance = async (id) => {
    const response = await client.put(`/maintenances/${id}/approve`);
    return response;
};

// Complete maintenance (ADMIN/SUPER)
export const completeMaintenance = async (id) => {
    const response = await client.put(`/maintenances/${id}/complete`);
    return response;
};

// Reject maintenance with reason (ADMIN/SUPER)
export const rejectMaintenance = async (id, rejectionReason) => {
    const response = await client.put(`/maintenances/${id}/reject`, { rejectionReason });
    return response;
};

import client from './client';

export const getLossRequests = async () => client.get('/losses');

export const createLossRequest = async (data) => client.post('/losses', data);

export const approveLossRequest = async (id) => client.put(`/losses/${id}/approve`);

export const rejectLossRequest = async (id, rejectionReason) => (
    client.put(`/losses/${id}/reject`, { rejectionReason })
);

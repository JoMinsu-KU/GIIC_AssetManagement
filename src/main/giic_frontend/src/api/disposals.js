import client from './client';

export const getDisposalList = async () => {
    const response = await client.get('/disposals');
    return response;
};

export const createDisposalRequest = async (data) => {
    // data should contain { assetId, reason }
    const response = await client.post('/disposals', data);
    return response;
};

export const approveDisposal = async (id) => {
    const response = await client.put(`/disposals/${id}/approve`);
    return response;
};

export const rejectDisposal = async (id, reason) => {
    const response = await client.put(`/disposals/${id}/reject`, { rejectionReason: reason });
    return response;
};

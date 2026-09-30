import client from './client';

// 이관 신청 생성
export const createTransfer = async (transferData) => {
    // transferData: { assetId, recipientMode, locationMode, recipientName, recipientDepartment, recipientContact, recipientEmail, transferLocation, remarks }
    const response = await client.post('/transfers', transferData);
    return response.data;
};

// 본인 이관 목록 조회 (USER용)
export const getMyTransfers = async () => {
    const response = await client.get('/transfers/my');
    return response.data;
};

// 전체 이관 목록 조회 (ADMIN용)
export const getAllTransfers = async () => {
    const response = await client.get('/transfers/all');
    return response.data;
};

export const getAssetTransferHistory = async (assetId) => {
    const response = await client.get(`/transfers/asset/${assetId}`);
    return response.data;
};

// 이관 승인 (ADMIN용)
export const approveTransfer = async (transferId) => {
    const response = await client.put(`/transfers/${transferId}/approve`);
    return response.data;
};

// 이관 거절 (ADMIN용)
export const rejectTransfer = async (transferId, reason) => {
    const response = await client.put(`/transfers/${transferId}/reject`, { reason });
    return response.data;
};

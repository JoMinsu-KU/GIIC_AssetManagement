import client from './client';
import { API_BASE_URL } from './config';

// Add this helper function
export const getAssetImageUrl = (imagePath) => {
    if (!imagePath) return null;

    // Extract just the filename if a full path is provided
    const filename = imagePath.split('/').pop();

    return `${API_BASE_URL}/assets/images/${filename}`;
};

export const getAssets = async (params = {}) => {
    const response = await client.get('/assets', { params });
    return response.data;
};

export const exportAssets = async (payload) => {
    return client.post('/assets/export', payload, {
        responseType: 'blob',
        timeout: 60000
    });
};

export const getAssetExportPreview = async (filters) => {
    const response = await client.post('/assets/export/preview', { filters }, {
        params: {
            page: 0,
            size: 20,
            sort: 'assetNo,asc'
        }
    });
    return response.data;
};

export const getAssetStats = async () => {
    // Get counts by status
    // Backend supports: NORMAL, REPAIRING, DISPOSED, LOST
    const [total, normal, repairing, disposed, lost] = await Promise.all([
        client.get('/assets', { params: { size: 1 } }),
        client.get('/assets', { params: { status: 'NORMAL', size: 1 } }),
        client.get('/assets', { params: { status: 'REPAIRING', size: 1 } }),
        client.get('/assets', { params: { status: 'DISPOSED', size: 1 } }),
        client.get('/assets', { params: { status: 'LOST', size: 1 } })
    ]);

    const getCount = (response) => {
        // Handle both camelCase (default) and snake_case (legacy/intended)
        const info = response.data?.pageInfo || response.data?.page_info;
        return info?.totalElements || info?.total_elements || 0;
    };

    return {
        total: getCount(total),
        // NORMAL maps to Available for now as API doesn't distinguish InUse/Available by status alone
        inUse: 0,
        available: getCount(normal),
        maintenance: getCount(repairing),
        disposed: getCount(disposed),
        lost: getCount(lost)
    };
};

export const getRecentAssets = async (size = 5) => {
    const response = await client.get('/assets', {
        params: { page: 0, size, sort: 'createdAt,desc' }
    });
    return response.data;
};

export const getAssetDetail = async (id) => {
    const response = await client.get(`/assets/${id}`);
    return response.data;
};

export const getAssetLocations = async () => {
    const response = await client.get('/assets/locations');
    return Array.isArray(response.data) ? response.data : [];
};

export const getAssetCategories = async () => {
    const response = await client.get('/assets/categories');
    return Array.isArray(response.data) ? response.data : [];
};

export const createAsset = async (data) => {
    const response = await client.post('/assets', data);
    return response.data;
};

export const updateAsset = async (id, data) => {
    console.log('API updateAsset:', id, data);
    const response = await client.put(`/assets/${id}`, data);
    return response.data;
};

export const deleteAsset = async (id) => {
    const response = await client.delete(`/assets/${id}`);
    return response.data;
};

export const uploadAssetImage = async (id, file, isPrimary = false) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('is_primary', isPrimary);

    const response = await client.post(`/assets/${id}/images`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
    return response.data;
};

export const addAssetImages = async (id, files, primaryIndex = null) => {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    if (primaryIndex !== null && primaryIndex !== undefined) {
        formData.append('primary_index', primaryIndex);
    }

    const response = await client.post(`/assets/${id}/images/batch`, formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    });
    return response.data;
};

export const setAssetPrimaryImage = async (assetId, imageId) => {
    const response = await client.put(`/assets/${assetId}/images/${imageId}/primary`);
    return response.data;
};

export const deleteAssetImage = async (assetId, imageId) => {
    const response = await client.delete(`/assets/${assetId}/images/${imageId}`);
    return response.data;
};

export const ocrAssetImage = async (file) => {
    const formData = new FormData();
    formData.append('file', file);  // Backend expects 'file' parameter

    const response = await client.post('/assets/ocr', formData, {
        headers: {
            'Content-Type': undefined
        },
        timeout: 60000
    });
    return response.data;
};

// Disposal APIs
export const requestDisposal = async (id, data) => {
    const response = await client.post(`/assets/${id}/disposal`, data);
    return response.data;
};

export const approveDisposal = async (id) => {
    const response = await client.put(`/assets/${id}/disposal/approve`);
    return response.data;
};

export const rejectDisposal = async (id, reason) => {
    const response = await client.put(`/assets/${id}/disposal/reject`, { rejectionReason: reason });
    return response.data;
};

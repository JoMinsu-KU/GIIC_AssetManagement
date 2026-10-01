import client from './client';

export const getAccessDashboard = async () => (await client.get('/admin/access')).data;
export const getAdminCategories = async () => (await client.get('/admin/categories')).data;
export const createCategory = async (payload) => (await client.post('/admin/categories', payload)).data;
export const renameCategory = async (id, name) => (await client.put(`/admin/categories/${id}`, { name })).data;
export const setCategoryActive = async (id, active) => (
    await client.patch(`/admin/categories/${id}/active`, { active })
).data;

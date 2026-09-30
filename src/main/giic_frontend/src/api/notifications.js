import client from './client';

export const getNotifications = () => client.get('/notifications');
export const getUnreadCount = () => client.get('/notifications/unread-count');
export const markAsRead = (id) => client.put(`/notifications/${id}/read`);
export const markAllAsRead = () => client.put('/notifications/read-all');
export const deleteAllNotifications = () => client.delete('/notifications');

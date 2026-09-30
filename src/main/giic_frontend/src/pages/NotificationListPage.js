import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/layout/DashboardLayout';
import ConfirmModal from '../components/common/ConfirmModal';
import * as NotificationApi from '../api/notifications';
import '../styles/Notification.css';

const NotificationListPage = () => {
    const navigate = useNavigate();
    const [notifications, setNotifications] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('all'); // all, unread, read
    const [deleteInProgress, setDeleteInProgress] = useState(false);
    const [isDeleteConfirmOpen, setIsDeleteConfirmOpen] = useState(false);

    const fetchNotifications = useCallback(async () => {
        try {
            setLoading(true);
            const response = await NotificationApi.getNotifications();
            if (response.success) {
                setNotifications(response.data);
            }
        } catch (error) {
            console.error('Failed to fetch notifications:', error);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchNotifications();
        window.addEventListener('notifications:updated', fetchNotifications);
        return () => window.removeEventListener('notifications:updated', fetchNotifications);
    }, [fetchNotifications]);

    const handleMarkAsRead = async (id, e) => {
        if (e) e.stopPropagation();
        try {
            await NotificationApi.markAsRead(id);
            setNotifications(prev => prev.map(notif =>
                notif.id === id ? { ...notif, read: true } : notif
            ));
            window.dispatchEvent(new Event('notifications:updated'));
        } catch (error) {
            console.error('Failed to mark notification as read:', error);
        }
    };

    const handleMarkAllAsRead = async () => {
        try {
            await NotificationApi.markAllAsRead();
            setNotifications(prev => prev.map(notif => ({ ...notif, read: true })));
            window.dispatchEvent(new Event('notifications:updated'));
        } catch (error) {
            console.error('Failed to mark all as read:', error);
        }
    };

    const handleDeleteAll = async () => {
        if (deleteInProgress) return;

        try {
            setDeleteInProgress(true);
            await NotificationApi.deleteAllNotifications();
            setNotifications([]);
            setFilter('all');
            window.dispatchEvent(new Event('notifications:updated'));
        } catch (error) {
            console.error('Failed to delete all notifications:', error);
            alert('알림을 모두 지우지 못했습니다. 다시 시도해 주세요.');
        } finally {
            setDeleteInProgress(false);
        }
    };

    const handleNotificationClick = async (notification) => {
        if (!notification.read) {
            await handleMarkAsRead(notification.id);
        }

        // Navigate based on related entity type
        if (notification.relatedEntityType) {
            switch (notification.relatedEntityType) {
                case 'TRANSFER':
                    navigate('/transfers');
                    break;
                case 'MAINTENANCE':
                    navigate('/main/maintenance-list');
                    break;
                case 'DISPOSAL':
                    navigate('/disposals');
                    break;
                case 'USER':
                    navigate('/users');
                    break;
                default:
                    break;
            }
        }
    };

    const formatTimeAgo = (dateString) => {
        const date = new Date(dateString);
        const now = new Date();
        const diffInSeconds = Math.floor((now - date) / 1000);

        if (diffInSeconds < 60) return '방금 전';

        const diffInMinutes = Math.floor(diffInSeconds / 60);
        if (diffInMinutes < 60) return `${diffInMinutes}분 전`;

        const diffInHours = Math.floor(diffInMinutes / 60);
        if (diffInHours < 24) return `${diffInHours}시간 전`;

        const diffInDays = Math.floor(diffInHours / 24);
        if (diffInDays === 1) return '어제';
        if (diffInDays < 7) return `${diffInDays}일 전`;

        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    };

    const getIconForType = (type) => {
        if (type.includes('APPROVED')) return <span className="notification-icon icon-success">✓</span>;
        if (type.includes('REJECTED')) return <span className="notification-icon icon-error">✕</span>;
        return <span className="notification-icon icon-info">📋</span>;
    };

    const filteredNotifications = notifications.filter(notification => {
        if (filter === 'unread') return !notification.read;
        if (filter === 'read') return notification.read;
        return true;
    });
    const unreadCount = notifications.filter((notification) => !notification.read).length;

    return (
        <DashboardLayout>
            <div className="notification-page-container">
                <div className="notification-page-header">
                    <h2>알림 센터</h2>
                    <div className="notification-page-actions">
                        <button
                            className="btn-secondary notification-action-button"
                            onClick={handleMarkAllAsRead}
                            disabled={unreadCount === 0 || deleteInProgress}
                        >
                            모두 읽음으로 표시
                        </button>
                        <button
                            className="btn-secondary notification-action-button notification-delete-button"
                            onClick={() => setIsDeleteConfirmOpen(true)}
                            disabled={notifications.length === 0 || deleteInProgress}
                        >
                            {deleteInProgress ? '삭제 중...' : '모두 지우기'}
                        </button>
                    </div>
                </div>

                <div className="notification-tabs">
                    <button
                        className={`notification-tab ${filter === 'all' ? 'active' : ''}`}
                        onClick={() => setFilter('all')}
                    >
                        전체
                    </button>
                    <button
                        className={`notification-tab ${filter === 'unread' ? 'active' : ''}`}
                        onClick={() => setFilter('unread')}
                    >
                        안 읽음
                    </button>
                    <button
                        className={`notification-tab ${filter === 'read' ? 'active' : ''}`}
                        onClick={() => setFilter('read')}
                    >
                        읽음
                    </button>
                </div>

                <div className="notification-list-card">
                    {loading ? (
                        <div className="loading-spinner">↻</div>
                    ) : filteredNotifications.length === 0 ? (
                        <div className="notification-empty">알림이 없습니다.</div>
                    ) : (
                        <div className="notification-list-content">
                            {filteredNotifications.map(notification => (
                                <div
                                    key={notification.id}
                                    className={`notification-list-item ${!notification.read ? 'unread' : ''}`}
                                    onClick={() => handleNotificationClick(notification)}
                                    style={{ cursor: 'pointer' }}
                                >
                                    {getIconForType(notification.type)}
                                    <div className="notification-content">
                                        <div className="notification-title">
                                            <span>{notification.title}</span>
                                            <span className="notification-time">{formatTimeAgo(notification.createdAt)}</span>
                                        </div>
                                        <div className="notification-message">
                                            {notification.message}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <ConfirmModal
                    isOpen={isDeleteConfirmOpen}
                    onClose={() => setIsDeleteConfirmOpen(false)}
                    title="알림 모두 지우기"
                    message={'모든 알림을 삭제하시겠습니까?\n삭제한 알림은 다시 복구할 수 없습니다.'}
                    type="confirm"
                    confirmText="모두 지우기"
                    onConfirm={handleDeleteAll}
                />
            </div>
        </DashboardLayout>
    );
};

export default NotificationListPage;

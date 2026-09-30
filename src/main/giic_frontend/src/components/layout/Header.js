import React, { useState, useRef, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import * as NotificationApi from '../../api/notifications';
import ConfirmModal from '../common/ConfirmModal';
import '../../styles/Dashboard.css';
import '../../styles/Notification.css';

const Header = () => {
    const { logout, user } = useAuth();
    const location = useLocation();
    const navigate = useNavigate();
    const [activeDropdown, setActiveDropdown] = useState(null);
    const [showNotifications, setShowNotifications] = useState(false);
    const [unreadCount, setUnreadCount] = useState(0);
    const [notifications, setNotifications] = useState([]);
    const [isDeleteNotificationConfirmOpen, setIsDeleteNotificationConfirmOpen] = useState(false);
    const navRef = useRef(null);
    const notificationRef = useRef(null);

    // 외부 클릭 감지
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (navRef.current && !navRef.current.contains(event.target)) {
                setActiveDropdown(null);
            }
            if (notificationRef.current && !notificationRef.current.contains(event.target)) {
                setShowNotifications(false);
            }
        };

        // 이벤트 리스너 등록
        document.addEventListener('mousedown', handleClickOutside);
        document.addEventListener('touchstart', handleClickOutside);

        // 클린업
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.removeEventListener('touchstart', handleClickOutside);
        };
    }, []);

    // 알림 폴링
    useEffect(() => {
        if (!user) return;

        const fetchUnreadCount = async () => {
            try {
                const response = await NotificationApi.getUnreadCount();
                if (response.success) {
                    setUnreadCount(response.data);
                }
            } catch (error) {
                console.error("Failed to fetch unread count", error);
            }
        };

        fetchUnreadCount();
        const interval = setInterval(fetchUnreadCount, 30000); // 30초마다 갱신
        window.addEventListener('notifications:updated', fetchUnreadCount);

        return () => {
            clearInterval(interval);
            window.removeEventListener('notifications:updated', fetchUnreadCount);
        };
    }, [user]);

    const handleBellClick = async () => {
        if (!showNotifications) {
            // 알림 열 때 목록 갱신
            try {
                const response = await NotificationApi.getNotifications();
                if (response.success) {
                    setNotifications(response.data);
                }
            } catch (error) {
                console.error("Failed to fetch notifications", error);
            }
        }
        setShowNotifications(!showNotifications);
        setActiveDropdown(null); // 다른 메뉴 닫기
    };

    const handleMarkAsRead = async (id, e) => {
        if (e) e.stopPropagation();
        try {
            await NotificationApi.markAsRead(id);
            setNotifications(prev => prev.map(n =>
                n.id === id ? { ...n, read: true } : n
            ));
            // 안 읽은 개수 감소
            setUnreadCount(prev => Math.max(0, prev - 1));
            window.dispatchEvent(new Event('notifications:updated'));
        } catch (error) {
            console.error("Failed to mark as read", error);
        }
    };

    const handleMarkAllAsRead = async () => {
        try {
            await NotificationApi.markAllAsRead();
            setNotifications(prev => prev.map(n => ({ ...n, read: true })));
            setUnreadCount(0);
            window.dispatchEvent(new Event('notifications:updated'));
        } catch (error) {
            console.error("Failed to mark all as read", error);
        }
    };

    const handleDeleteAllNotifications = async () => {
        try {
            await NotificationApi.deleteAllNotifications();
            setNotifications([]);
            setUnreadCount(0);
            window.dispatchEvent(new Event('notifications:updated'));
        } catch (error) {
            console.error("Failed to delete all notifications", error);
            alert('알림을 모두 지우지 못했습니다. 다시 시도해 주세요.');
        }
    };

    const handleNotificationItemClick = async (notification) => {
        if (!notification.read) {
            await handleMarkAsRead(notification.id);
        }

        setShowNotifications(false);

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
                case 'LOSS':
                    navigate('/losses');
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

        return date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' });
    };

    const getIconForType = (type) => {
        if (type.includes('APPROVED')) return <span className="notification-icon icon-success">✓</span>;
        if (type.includes('REJECTED')) return <span className="notification-icon icon-error">✕</span>;
        return <span className="notification-icon icon-info">📋</span>;
    };

    const getRoleLabel = (role) => {
        const labels = {
            'ADMIN': '관리자',
            'ROLE_ADMIN': '관리자',
            'SUPER': '슈퍼관리자',
            'ROLE_SUPER': '슈퍼관리자',
            'USER': '일반사용자',
            'ROLE_USER': '일반사용자'
        };
        return labels[role] || role;
    };

    const handleNavClick = (menuName) => {
        setActiveDropdown(activeDropdown === menuName ? null : menuName);
    };

    const closeMenus = () => {
        setActiveDropdown(null);
    };

    const menuItems = [
        {
            name: '대시보드',
            icon: '📊',
            path: '/main',
            children: null
        },
        {
            name: '자산 관리',
            icon: '📦',
            children: [
                { name: '자산 목록', path: '/assets' },
                { name: '자산 등록', path: '/assets/new' },
                { name: '이관 목록', path: '/transfers' },
                { name: '폐기 목록', path: '/disposals' },
                { name: '손망 목록', path: '/losses' }
            ]
        },
        {
            name: '유지보수',
            icon: '🔧',
            children: [
                { name: '유지보수 신청 내역', path: '/main/maintenance-list' }
            ]
        },
        {
            name: '사용자 관리',
            icon: '👥',
            path: '/users',
            children: null,
            roles: ['ADMIN', 'SUPER']
        },
        {
            name: '시스템 설정',
            icon: '⚙️',
            path: '/settings',
            children: null,
            roles: ['SUPER']
        }
    ];

    return (
        <header className="main-header">
            <div className="header-logo">
                <Link to="/main" className="logo-section">
                    <img
                        src="/giic-logo-full.svg"
                        alt="GIIC 경남지능화혁신사업단"
                        className="header-brand-image"
                    />
                </Link>
            </div>

            <nav ref={navRef} className="header-nav nav-menu" onClick={(e) => e.stopPropagation()}>
                {menuItems.map((item) => {
                    // Filter by role if specified
                    if (item.roles && !item.roles.includes(user?.role)) {
                        return null;
                    }

                    if (item.children) {
                        return (
                            <div
                                key={item.name}
                                className={`nav-item has-dropdown ${activeDropdown === item.name ? 'active' : ''}`}
                            >
                                <button
                                    className="nav-btn"
                                    onClick={() => handleNavClick(item.name)}
                                >
                                    <span className="nav-icon">{item.icon}</span>
                                    <span className="nav-text">{item.name}</span>
                                    <span className="dropdown-arrow">▼</span>
                                </button>
                                <div className={`nav-dropdown ${activeDropdown === item.name ? 'show' : ''}`}>
                                    {item.children.map((child) => (
                                        <Link
                                            key={child.path}
                                            to={child.path}
                                            className={`dropdown-item ${location.pathname === child.path ? 'active' : ''}`}
                                            onClick={closeMenus}
                                        >
                                            {child.name}
                                        </Link>
                                    ))}
                                </div>
                            </div>
                        );
                    } else {
                        return (
                            <Link
                                key={item.name}
                                to={item.path}
                                className={`nav-item nav-link ${location.pathname === item.path ? 'active' : ''}`}
                                onClick={closeMenus}
                            >
                                <span className="nav-icon">{item.icon}</span>
                                <span className="nav-text">{item.name}</span>
                            </Link>
                        );
                    }
                })}
            </nav>

            <div className="header-actions">
                {/* Notification Bell */}
                <div className="notification-bell-container" ref={notificationRef}>
                    <button className="notification-bell-btn" onClick={handleBellClick}>
                        🔔
                        {unreadCount > 0 && <span className="notification-badge">{unreadCount}</span>}
                    </button>

                    {showNotifications && (
                        <div className="notification-dropdown">
                            <div className="notification-header">
                                <h3>알림</h3>
                                <div className="notification-header-actions">
                                    <button
                                        className="mark-all-read"
                                        onClick={handleMarkAllAsRead}
                                        disabled={unreadCount === 0}
                                    >
                                        모두 읽음
                                    </button>
                                    <button
                                        className="delete-all-notifications"
                                        onClick={() => setIsDeleteNotificationConfirmOpen(true)}
                                        disabled={notifications.length === 0}
                                    >
                                        모두 지우기
                                    </button>
                                </div>
                            </div>
                            <div className="notification-list">
                                {notifications.length === 0 ? (
                                    <div className="notification-empty">새로운 알림이 없습니다.</div>
                                ) : (
                                    notifications.map(notification => (
                                        <div
                                            key={notification.id}
                                            className={`notification-item ${!notification.read ? 'unread' : ''}`}
                                            onClick={() => handleNotificationItemClick(notification)}
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
                                    ))
                                )}
                            </div>
                            <div className="notification-footer">
                                <Link to="/notifications" className="view-all-link" onClick={() => setShowNotifications(false)}>
                                    전체 알림 보기
                                </Link>
                            </div>
                        </div>
                    )}
                </div>

                <div className="user-info">
                    <span className="user-name">{user?.name || '사용자'}</span>
                    <span className="user-role">{getRoleLabel(user?.role)}</span>
                </div>
                <button className="logout-btn" onClick={logout}>
                    <span>로그아웃</span>
                    <span style={{ fontSize: '16px' }}>→</span>
                </button>
            </div>

            <ConfirmModal
                isOpen={isDeleteNotificationConfirmOpen}
                onClose={() => setIsDeleteNotificationConfirmOpen(false)}
                title="알림 모두 지우기"
                message={'모든 알림을 삭제하시겠습니까?\n삭제한 알림은 다시 복구할 수 없습니다.'}
                type="confirm"
                confirmText="모두 지우기"
                onConfirm={handleDeleteAllNotifications}
            />
        </header>
    );
};

export default Header;

import React, { useCallback, useEffect, useState } from 'react';
import DashboardLayout from '../components/layout/DashboardLayout';
import { getUsers, approveUser, rejectUser, updateUserRole, deactivateUser } from '../api/users';
import { useAuth } from '../contexts/AuthContext';
import '../styles/Dashboard.css';
import '../styles/Modal.css';

const USER_PAGE_SIZE = 20;
const USER_STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'INACTIVE'];

const getInitialUserFilters = () => {
    const requestedStatus = new URLSearchParams(window.location.search).get('status');
    return {
        keyword: '',
        status: USER_STATUSES.includes(requestedStatus) ? requestedStatus : '',
        role: ''
    };
};

const UserListPage = () => {
    const { user: currentUser } = useAuth();
    const [users, setUsers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedUser, setSelectedUser] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editRole, setEditRole] = useState('');
    const [pageInfo, setPageInfo] = useState({
        total_elements: 0,
        total_pages: 0,
        current_page: 0,
        size: USER_PAGE_SIZE
    });

    const [filters, setFilters] = useState(getInitialUserFilters);

    const getStatusLabel = (status) => {
        const labels = {
            'PENDING': '승인 대기',
            'APPROVED': '승인됨',
            'REJECTED': '거절됨',
            'INACTIVE': '비활성화'
        };
        return labels[status] || status;
    };

    const getStatusClass = (status) => {
        const classes = {
            'PENDING': 'status-pending',
            'APPROVED': 'status-approved',
            'REJECTED': 'status-rejected',
            'INACTIVE': 'status-inactive'
        };
        return classes[status] || '';
    };

    const fetchUsers = useCallback(async (
        page = 0,
        signal,
        activeFilters = {},
        pageSize = USER_PAGE_SIZE
    ) => {
        setLoading(true);
        try {
            const params = {
                page: page,
                size: pageSize,
                ...activeFilters
            };
            Object.keys(params).forEach(key => {
                if (params[key] === '') delete params[key];
            });

            const pageResponse = await getUsers(params, signal);
            if (pageResponse) {
                const { content, ...info } = pageResponse;
                setUsers(content || []);
                setPageInfo({
                    total_elements: info.totalElements || info.total_elements,
                    total_pages: info.totalPages || info.total_pages,
                    current_page: (info.pageNumber || info.page || 0),
                    size: info.pageSize || info.size
                });
            }
        } catch (error) {
            if (error.code === 'ERR_CANCELED' || error.response?.status === 401) {
                return;
            }
            console.error('Failed to fetch users:', error);
            alert('사용자 목록을 불러오는데 실패했습니다.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        const controller = new AbortController();
        fetchUsers(0, controller.signal, getInitialUserFilters(), USER_PAGE_SIZE);
        return () => controller.abort();
    }, [fetchUsers]);

    const handleSearch = (e) => {
        e.preventDefault();
        fetchUsers(0, undefined, filters, pageInfo.size);
    };

    const handleFilterChange = (e) => {
        const { name, value } = e.target;
        setFilters(prev => ({ ...prev, [name]: value }));
    };

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < pageInfo.total_pages) {
            fetchUsers(newPage, undefined, filters, pageInfo.size);
        }
    };

    const handleApprove = async (employeeNo) => {
        if (!window.confirm('이 사용자를 승인하시겠습니까?')) return;
        try {
            await approveUser(employeeNo);
            alert('승인되었습니다.');
            fetchUsers(pageInfo.current_page, undefined, filters, pageInfo.size);
        } catch (error) {
            console.error(error);
            alert('승인 처리에 실패했습니다.');
        }
    };

    const handleReject = async (employeeNo) => {
        if (!window.confirm('이 사용자를 거절하시겠습니까?')) return;
        try {
            await rejectUser(employeeNo);
            alert('거절되었습니다.');
            fetchUsers(pageInfo.current_page, undefined, filters, pageInfo.size);
        } catch (error) {
            console.error(error);
            alert('거절 처리에 실패했습니다.');
        }
    };

    const handleDeactivate = async (employeeNo) => {
        if (!window.confirm('이 사용자를 비활성화하시겠습니까?')) return;
        try {
            await deactivateUser(employeeNo);
            alert('비활성화되었습니다.');
            fetchUsers(pageInfo.current_page, undefined, filters, pageInfo.size);
        } catch (error) {
            console.error(error);
            alert('비활성화 처리에 실패했습니다.');
        }
    };

    const handleOpenModal = (user) => {
        setSelectedUser(user);
        setEditRole(user.role);
        setIsModalOpen(true);
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
        setSelectedUser(null);
    };

    const handleSaveRole = async () => {
        if (!selectedUser) return;
        if (editRole === selectedUser.role) {
            alert('변경사항이 없습니다.');
            return;
        }

        try {
            await updateUserRole(selectedUser.employeeNo, editRole);
            alert('권한이 변경되었습니다.');
            fetchUsers(pageInfo.current_page, undefined, filters, pageInfo.size);
            handleCloseModal();
        } catch (error) {
            console.error(error);
            alert('권한 변경에 실패했습니다.');
        }
    };

    return (
        <DashboardLayout>
            <div className="asset-page-header">
                <h1 className="page-title">사용자 관리</h1>
            </div>

            <div className="search-filter-container">
                <div className="filter-group">
                    <label className="filter-label">검색어</label>
                    <input
                        type="text"
                        name="keyword"
                        className="form-input"
                        placeholder="이름, 사번..."
                        value={filters.keyword}
                        onChange={handleFilterChange}
                        onKeyDown={(e) => e.key === 'Enter' && handleSearch(e)}
                    />
                </div>
                <div className="filter-group">
                    <label className="filter-label">상태</label>
                    <select
                        name="status"
                        className="form-select"
                        value={filters.status}
                        onChange={handleFilterChange}
                    >
                        <option value="">전체</option>
                        <option value="PENDING">승인 대기</option>
                        <option value="APPROVED">승인됨</option>
                        <option value="REJECTED">거절됨</option>
                        <option value="INACTIVE">비활성화</option>
                    </select>
                </div>
                <div className="filter-group">
                    <label className="filter-label">권한</label>
                    <select
                        name="role"
                        className="form-select"
                        value={filters.role}
                        onChange={handleFilterChange}
                    >
                        <option value="">전체</option>
                        <option value="USER">일반사용자</option>
                        <option value="ADMIN">관리자</option>
                        {currentUser?.role === 'SUPER' && (
                            <option value="SUPER">슈퍼관리자</option>
                        )}
                    </select>
                </div>
                <button className="btn-secondary" onClick={handleSearch}>검색</button>
            </div>

            <div className="table-container">
                <table className="asset-table">
                    <thead>
                        <tr>
                            <th>사번</th>
                            <th>이름</th>
                            <th>부서</th>
                            <th>이메일</th>
                            <th>권한</th>
                            <th>상태</th>
                            <th>등록일</th>
                            <th>관리</th>
                        </tr>
                    </thead>
                    <tbody>
                        {loading ? (
                            <tr>
                                <td colSpan="8" style={{ textAlign: 'center', padding: '40px' }}>로딩 중...</td>
                            </tr>
                        ) : users.length === 0 ? (
                            <tr>
                                <td colSpan="8" style={{ textAlign: 'center', padding: '40px' }}>검색 결과가 없습니다.</td>
                            </tr>
                        ) : (
                            users.map((user) => (
                                <tr key={user.employeeNo} onClick={() => handleOpenModal(user)} style={{ cursor: 'pointer' }}>
                                    <td>{user.employeeNo}</td>
                                    <td>{user.name}</td>
                                    <td>{user.department}</td>
                                    <td>{user.email}</td>
                                    <td>{user.role}</td>
                                    <td>
                                        <span className={`status-badge ${getStatusClass(user.status)}`}>
                                            {getStatusLabel(user.status)}
                                        </span>
                                    </td>
                                    <td>{new Date(user.createdAt).toLocaleDateString()}</td>
                                    <td onClick={(e) => e.stopPropagation()}>
                                        <div style={{ display: 'flex', gap: '5px' }}>
                                            <button className="btn-secondary" style={{ padding: '4px 8px', fontSize: '12px' }} onClick={() => handleOpenModal(user)}>상세</button>
                                            {user.status === 'PENDING' && (
                                                <>
                                                    <button className="btn-secondary" style={{ padding: '4px 8px', fontSize: '12px' }} onClick={() => handleApprove(user.employeeNo)}>승인</button>
                                                    <button className="btn-secondary" style={{ padding: '4px 8px', fontSize: '12px', color: 'red', borderColor: 'red' }} onClick={() => handleReject(user.employeeNo)}>거절</button>
                                                </>
                                            )}
                                            {user.status === 'APPROVED' && (
                                                <>
                                                    <button className="btn-secondary" style={{ padding: '4px 8px', fontSize: '12px', color: 'gray' }} onClick={() => handleDeactivate(user.employeeNo)}>비활성</button>
                                                </>
                                            )}
                                            {user.status === 'INACTIVE' && (
                                                <button className="btn-secondary" style={{ padding: '4px 8px', fontSize: '12px' }} onClick={() => handleApprove(user.employeeNo)}>재활성</button>
                                            )}
                                        </div>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {!loading && pageInfo.total_pages > 0 && (
                <div className="pagination">
                    <button className="pagination-btn" disabled={pageInfo.current_page === 0} onClick={() => handlePageChange(pageInfo.current_page - 1)}>{'<'}</button>
                    <span style={{ margin: '0 10px' }}>{pageInfo.current_page + 1} / {pageInfo.total_pages}</span>
                    <button className="pagination-btn" disabled={pageInfo.current_page === pageInfo.total_pages - 1} onClick={() => handlePageChange(pageInfo.current_page + 1)}>{'>'}</button>
                </div>
            )}

            {isModalOpen && selectedUser && (
                <div className="modal-overlay" onClick={handleCloseModal}>
                    <div className="modal-content" onClick={e => e.stopPropagation()}>
                        <div className="modal-header">
                            <h2>사용자 상세 정보</h2>
                            <button className="modal-close-btn" onClick={handleCloseModal}>&times;</button>
                        </div>
                        <div className="modal-grid">
                            <div className="form-group">
                                <label>사번</label>
                                <input type="text" className="form-input" value={selectedUser.employeeNo} readOnly disabled />
                            </div>
                            <div className="form-group">
                                <label>이름</label>
                                <input type="text" className="form-input" value={selectedUser.name} readOnly disabled />
                            </div>
                            <div className="form-group">
                                <label>부서</label>
                                <input type="text" className="form-input" value={selectedUser.department} readOnly disabled />
                            </div>
                            <div className="form-group">
                                <label>이메일</label>
                                <input type="text" className="form-input" value={selectedUser.email} readOnly disabled />
                            </div>
                            <div className="form-group">
                                <label>전화번호</label>
                                <input type="text" className="form-input" value={selectedUser.phone || '-'} readOnly disabled />
                            </div>
                            <div className="form-group">
                                <label>상태</label>
                                <div style={{ marginTop: '5px' }}>
                                    <span className={`status-badge ${getStatusClass(selectedUser.status)}`}>
                                        {getStatusLabel(selectedUser.status)}
                                    </span>
                                </div>
                            </div>
                            <div className="form-group">
                                <label>가입일</label>
                                <input type="text" className="form-input" value={new Date(selectedUser.createdAt).toLocaleString()} readOnly disabled />
                            </div>

                            <div className="form-group">
                                <label>권한</label>
                                {selectedUser.status === 'APPROVED' && selectedUser.role !== 'SUPER' ? (
                                    <select
                                        className="form-select"
                                        value={editRole}
                                        onChange={(e) => setEditRole(e.target.value)}
                                    >
                                        <option value="USER">일반사용자 (USER)</option>
                                        <option value="ADMIN">관리자 (ADMIN)</option>
                                    </select>
                                ) : (
                                    <input type="text" className="form-input" value={selectedUser.role} readOnly disabled />
                                )}
                            </div>
                        </div>
                        <div className="modal-actions">
                            {selectedUser.status === 'APPROVED' && selectedUser.role !== 'SUPER' && (
                                <button className="btn-primary" onClick={handleSaveRole}>
                                    저장
                                </button>
                            )}
                            <button className="btn-secondary" onClick={handleCloseModal}>
                                닫기
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </DashboardLayout>
    );
};

export default UserListPage;

import React, { useState, useEffect } from 'react';
import DashboardLayout from '../components/layout/DashboardLayout';
import { useAuth } from '../contexts/AuthContext';
import { isAdmin, isSuper } from '../utils/auth';
import { getMaintenanceList, approveMaintenance, rejectMaintenance, completeMaintenance } from '../api/maintenances';
import MessageModal from '../components/common/MessageModal';
import '../styles/Asset.css';
import '../styles/Maintenance.css';
import '../styles/Modal.css'; // For modal styles

const MaintenanceListPage = () => {
    const { user } = useAuth();
    const [maintenances, setMaintenances] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('ALL'); // ALL, PENDING, IN_PROGRESS, COMPLETED, REJECTED

    // For rejection modal
    const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
    const [selectedMaintenanceId, setSelectedMaintenanceId] = useState(null);
    const [rejectionReason, setRejectionReason] = useState('');

    // For viewing rejection reason
    const [viewReasonModal, setViewReasonModal] = useState({ isOpen: false, text: '' });

    useEffect(() => {
        loadMaintenances();
    }, [user]);

    const loadMaintenances = async () => {
        try {
            setLoading(true);
            const response = await getMaintenanceList();
            setMaintenances(Array.isArray(response) ? response : (response.data || []));
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = async (id) => {
        if (!window.confirm('이 유지보수 신청을 승인하시겠습니까?')) return;
        try {
            await approveMaintenance(id);
            alert('승인되었습니다.');
            loadMaintenances();
        } catch (err) {
            alert('승인에 실패했습니다.');
        }
    };

    const handleComplete = async (id) => {
        if (!window.confirm('이 유지보수 작업을 완료 처리하시겠습니까?')) return;
        try {
            await completeMaintenance(id);
            alert('완료 처리되었습니다.');
            loadMaintenances();
        } catch (err) {
            alert('완료 처리에 실패했습니다.');
        }
    };

    const openRejectModal = (id) => {
        setSelectedMaintenanceId(id);
        setRejectionReason('');
        setIsRejectModalOpen(true);
    };

    const handleRejectSubmit = async () => {
        if (!rejectionReason) {
            alert('거절 사유를 입력해주세요.');
            return;
        }
        try {
            await rejectMaintenance(selectedMaintenanceId, rejectionReason);
            alert('거절되었습니다.');
            setIsRejectModalOpen(false);
            loadMaintenances();
        } catch (err) {
            alert('거절에 실패했습니다.');
        }
    };

    const showRejectionReason = (reason) => {
        setViewReasonModal({ isOpen: true, text: reason });
    };

    const getStatusLabel = (status) => {
        const labels = {
            'PENDING': '대기중',
            'IN_PROGRESS': '진행중',
            'COMPLETED': '완료',
            'REJECTED': '거절'
        };
        return labels[status] || status;
    };

    const getStatusClass = (status) => {
        const classes = {
            'PENDING': 'status-pending',
            'IN_PROGRESS': 'status-in-progress',
            'COMPLETED': 'status-completed',
            'REJECTED': 'status-rejected'
        };
        return classes[status] || '';
    };

    const getMaintenanceTypeLabel = (type) => {
        const labels = {
            'REPAIR': '수리',
            'INSPECTION': '점검',
            'REPLACEMENT': '교체',
            'OTHER': '기타'
        };
        return labels[type] || type;
    };

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        try {
            const date = new Date(dateString);
            if (isNaN(date.getTime())) return '-';
            return date.toLocaleDateString('ko-KR', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit'
            });
        } catch {
            return '-';
        }
    };

    const filteredMaintenances = maintenances.filter(m => {
        if (filter === 'ALL') return true;
        return m.status === filter;
    });

    const isManager = isAdmin(user) || isSuper(user);

    return (
        <DashboardLayout>
            <div className="maintenance-list-page">
                <div className="page-header">
                    <h1 className="page-title">유지보수 {isManager ? '관리' : '신청 내역'}</h1>
                </div>

                {isManager && (
                    <div className="filter-section">
                        <select className="form-select" value={filter} onChange={(e) => setFilter(e.target.value)}>
                            <option value="ALL">전체</option>
                            <option value="PENDING">대기중</option>
                            <option value="IN_PROGRESS">진행중</option>
                            <option value="COMPLETED">완료</option>
                            <option value="REJECTED">거절</option>
                        </select>
                    </div>
                )}

                <div className="table-container">
                    <table className="asset-table">
                        <thead>
                            <tr>
                                <th>자산번호</th>
                                <th>자산명</th>
                                <th>유지보수 유형</th>
                                <th>신청 내용</th>
                                <th>신청자</th>
                                <th>사번</th>
                                <th>신청일</th>
                                <th>상태</th>
                                {isManager && <th>관리</th>}
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={isManager ? 9 : 8} style={{ textAlign: 'center', padding: '20px' }}>로딩 중...</td></tr>
                            ) : filteredMaintenances.length === 0 ? (
                                <tr><td colSpan={isManager ? 9 : 8} style={{ textAlign: 'center', padding: '20px' }}>유지보수 신청 내역이 없습니다.</td></tr>
                            ) : (
                                filteredMaintenances.map(m => (
                                    <tr key={m.id}>
                                        <td>{m.assetNo || '-'}</td>
                                        <td>{m.assetName || '-'}</td>
                                        <td>{getMaintenanceTypeLabel(m.maintenanceType)}</td>
                                        <td className="description-cell" title={m.description}>
                                            {m.description && m.description.length > 20
                                                ? `${m.description.substring(0, 20)}...`
                                                : m.description}
                                        </td>
                                        <td>{m.requesterName || '-'}</td>
                                        <td>{m.requesterEmployeeNo || '-'}</td>
                                        <td>{formatDate(m.createdAt)}</td>
                                        <td>
                                            <span
                                                className={`status-badge ${getStatusClass(m.status)}`}
                                                onClick={() => m.status === 'REJECTED' && m.rejectionReason && showRejectionReason(m.rejectionReason)}
                                                style={m.status === 'REJECTED' && m.rejectionReason ? { cursor: 'pointer' } : {}}
                                                title={m.status === 'REJECTED' && m.rejectionReason ? '거절 사유 보기' : ''}
                                            >
                                                {getStatusLabel(m.status)}
                                            </span>
                                        </td>
                                        {isManager && (
                                            <td>
                                                {m.status === 'PENDING' && (
                                                    <div className="action-buttons">
                                                        <button className="btn-approve" onClick={() => handleApprove(m.id)}>승인</button>
                                                        <button className="btn-reject" onClick={() => openRejectModal(m.id)}>거절</button>
                                                    </div>
                                                )}
                                                {m.status === 'IN_PROGRESS' && (
                                                    <div className="action-buttons">
                                                        <button className="btn-complete" onClick={() => handleComplete(m.id)}>완료</button>
                                                    </div>
                                                )}
                                                {m.status === 'REJECTED' && m.rejectionReason && (
                                                    <span className="reject-reason" onClick={() => showRejectionReason(m.rejectionReason)}>사유 보기</span>
                                                )}
                                            </td>
                                        )}
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                {/* View Rejection Reason Modal */}
                <MessageModal
                    isOpen={viewReasonModal.isOpen}
                    onClose={() => setViewReasonModal({ ...viewReasonModal, isOpen: false })}
                    title="거절 사유"
                    message={viewReasonModal.text}
                    buttonText="닫기"
                />

                {/* Rejection Modal */}
                {isRejectModalOpen && (
                    <div className="modal-overlay" onClick={() => setIsRejectModalOpen(false)}>
                        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                            <div className="modal-header">
                                <h2>유지보수 거절</h2>
                                <button className="modal-close-btn" onClick={() => setIsRejectModalOpen(false)}>&times;</button>
                            </div>
                            <div className="modal-body">
                                <p style={{ marginBottom: '10px' }}>거절 사유를 입력하세요:</p>
                                <textarea
                                    className="form-input form-textarea"
                                    value={rejectionReason}
                                    onChange={(e) => setRejectionReason(e.target.value)}
                                    rows="4"
                                    placeholder="거절 사유 입력"
                                />
                            </div>
                            <div className="modal-actions">
                                <button className="btn-secondary" onClick={() => setIsRejectModalOpen(false)}>취소</button>
                                <button className="btn-reject" onClick={handleRejectSubmit}>거절</button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </DashboardLayout>
    );
};

export default MaintenanceListPage;

import React, { useState, useEffect } from 'react';
import DashboardLayout from '../components/layout/DashboardLayout';
import { useAuth } from '../contexts/AuthContext';
import { isAdmin, isSuper } from '../utils/auth';
import { getDisposalList, approveDisposal, rejectDisposal } from '../api/disposals';
import MessageModal from '../components/common/MessageModal';
import '../styles/Asset.css';
import '../styles/Disposal.css';
import '../styles/Modal.css';

const DisposalListPage = () => {
    const { user } = useAuth();
    const [disposals, setDisposals] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('ALL'); // ALL, PENDING, APPROVED, REJECTED

    // For rejection modal
    const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);
    const [selectedDisposalId, setSelectedDisposalId] = useState(null);
    const [rejectionReason, setRejectionReason] = useState('');

    // For viewing rejection reason
    const [viewReasonModal, setViewReasonModal] = useState({ isOpen: false, text: '' });

    useEffect(() => {
        loadDisposals();
    }, [user]);

    const loadDisposals = async () => {
        try {
            setLoading(true);
            const response = await getDisposalList();
            setDisposals(Array.isArray(response) ? response : (response.data || []));
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = async (id) => {
        if (!window.confirm('이 폐기 신청을 승인하시겠습니까? 승인 시 자산 상태가 [폐기]로 변경됩니다.')) return;
        try {
            await approveDisposal(id);
            alert('승인되었습니다.');
            loadDisposals();
        } catch (err) {
            alert('승인에 실패했습니다.');
        }
    };

    const openRejectModal = (id) => {
        setSelectedDisposalId(id);
        setRejectionReason('');
        setIsRejectModalOpen(true);
    };

    const handleRejectSubmit = async () => {
        if (!rejectionReason) {
            alert('거절 사유를 입력해주세요.');
            return;
        }
        try {
            await rejectDisposal(selectedDisposalId, rejectionReason);
            alert('거절되었습니다.');
            setIsRejectModalOpen(false);
            loadDisposals();
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
            'APPROVED': '승인됨',
            'REJECTED': '거절됨'
        };
        return labels[status] || status;
    };

    const getStatusClass = (status) => {
        const classes = {
            'PENDING': 'status-disposal-pending',
            'APPROVED': 'status-disposed',
            'REJECTED': 'status-disposal-rejected'
        };
        return classes[status] || '';
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

    const filteredDisposals = disposals.filter(d => {
        if (filter === 'ALL') return true;
        return d.status === filter;
    });

    const isManager = isAdmin(user) || isSuper(user);

    return (
        <DashboardLayout>
            <div className="maintenance-list-page">
                <div className="page-header">
                    <h1 className="page-title">폐기 신청 내역</h1>
                </div>

                {isManager && (
                    <div className="filter-section">
                        <select className="form-select" value={filter} onChange={(e) => setFilter(e.target.value)}>
                            <option value="ALL">전체</option>
                            <option value="PENDING">대기중</option>
                            <option value="APPROVED">승인됨</option>
                            <option value="REJECTED">거절됨</option>
                        </select>
                    </div>
                )}

                <div className="table-container">
                    <table className="asset-table">
                        <thead>
                            <tr>
                                <th>자산번호</th>
                                <th>자산명</th>
                                <th>폐기 사유</th>
                                <th>신청자</th>
                                <th>신청일</th>
                                <th>상태</th>
                                {isManager && <th>관리</th>}
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={isManager ? 7 : 6} style={{ textAlign: 'center', padding: '20px' }}>로딩 중...</td></tr>
                            ) : filteredDisposals.length === 0 ? (
                                <tr><td colSpan={isManager ? 7 : 6} style={{ textAlign: 'center', padding: '20px' }}>폐기 신청 내역이 없습니다.</td></tr>
                            ) : (
                                filteredDisposals.map(d => (
                                    <tr key={d.id}>
                                        <td>{d.assetNo || '-'}</td>
                                        <td>{d.assetName || '-'}</td>
                                        <td className="description-cell" title={d.reason}>
                                            {d.reason && d.reason.length > 20
                                                ? `${d.reason.substring(0, 20)}...`
                                                : d.reason}
                                        </td>
                                        <td>{d.requesterName || '-'}</td>
                                        <td>{formatDate(d.createdAt)}</td>
                                        <td>
                                            <span
                                                className={`status-badge ${getStatusClass(d.status)}`}
                                                onClick={() => d.status === 'REJECTED' && d.rejectionReason && showRejectionReason(d.rejectionReason)}
                                                style={d.status === 'REJECTED' && d.rejectionReason ? { cursor: 'pointer' } : {}}
                                                title={d.status === 'REJECTED' && d.rejectionReason ? '거절 사유 보기' : ''}
                                            >
                                                {getStatusLabel(d.status)}
                                            </span>
                                        </td>
                                        {isManager && (
                                            <td>
                                                {d.status === 'PENDING' && (
                                                    <div className="action-buttons">
                                                        <button className="btn-approve" onClick={() => handleApprove(d.id)}>승인</button>
                                                        <button className="btn-reject" onClick={() => openRejectModal(d.id)}>거절</button>
                                                    </div>
                                                )}
                                                {d.status === 'REJECTED' && d.rejectionReason && (
                                                    <span className="reject-reason" onClick={() => showRejectionReason(d.rejectionReason)}>사유 보기</span>
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
                                <h2>폐기 요청 거절</h2>
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

export default DisposalListPage;

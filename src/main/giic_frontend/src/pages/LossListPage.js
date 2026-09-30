import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/layout/DashboardLayout';
import MessageModal from '../components/common/MessageModal';
import { useAuth } from '../contexts/AuthContext';
import { isAdmin, isSuper } from '../utils/auth';
import { approveLossRequest, getLossRequests, rejectLossRequest } from '../api/losses';
import '../styles/Asset.css';
import '../styles/Disposal.css';
import '../styles/Modal.css';

const LossListPage = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('ALL');
    const [rejectModal, setRejectModal] = useState({ isOpen: false, id: null });
    const [rejectionReason, setRejectionReason] = useState('');
    const [reasonModal, setReasonModal] = useState({ isOpen: false, text: '' });

    const isManager = isAdmin(user) || isSuper(user);

    const loadRequests = useCallback(async () => {
        try {
            setLoading(true);
            const response = await getLossRequests();
            setRequests(Array.isArray(response) ? response : []);
        } catch (error) {
            console.error('Failed to load loss requests:', error);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadRequests();
    }, [user, loadRequests]);

    const handleApprove = async (id) => {
        if (!window.confirm('이 손망 신청을 승인하시겠습니까? 승인 후 자산 상태가 손망으로 변경됩니다.')) return;
        try {
            await approveLossRequest(id);
            window.alert('손망 신청을 승인했습니다.');
            loadRequests();
        } catch (error) {
            window.alert(error.response?.data?.message || '손망 승인에 실패했습니다.');
        }
    };

    const openRejectModal = (id) => {
        setRejectModal({ isOpen: true, id });
        setRejectionReason('');
    };

    const handleReject = async () => {
        if (!rejectionReason.trim()) {
            window.alert('반려 사유를 입력해주세요.');
            return;
        }
        try {
            await rejectLossRequest(rejectModal.id, rejectionReason.trim());
            window.alert('손망 신청을 반려했습니다.');
            setRejectModal({ isOpen: false, id: null });
            loadRequests();
        } catch (error) {
            window.alert(error.response?.data?.message || '손망 반려에 실패했습니다.');
        }
    };

    const formatDate = (value) => {
        if (!value) return '-';
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('ko-KR');
    };

    const statusLabel = {
        PENDING: '승인 대기',
        APPROVED: '승인',
        REJECTED: '반려'
    };

    const statusClass = {
        PENDING: 'status-loss-pending',
        APPROVED: 'status-lost',
        REJECTED: 'status-loss-rejected'
    };

    const filteredRequests = filter === 'ALL'
        ? requests
        : requests.filter((request) => request.status === filter);
    const columnCount = isManager ? 8 : 7;

    return (
        <DashboardLayout>
            <div className="maintenance-list-page">
                <div className="page-header">
                    <div>
                        <p className="asset-detail-breadcrumb">자산관리 / 손망</p>
                        <h1 className="page-title">손망 신청 내역</h1>
                    </div>
                </div>

                <div className="filter-section">
                    <select className="form-select" value={filter} onChange={(event) => setFilter(event.target.value)}>
                        <option value="ALL">전체</option>
                        <option value="PENDING">승인 대기</option>
                        <option value="APPROVED">승인</option>
                        <option value="REJECTED">반려</option>
                    </select>
                </div>

                <div className="table-container">
                    <table className="asset-table">
                        <thead>
                            <tr>
                                <th>자산번호</th>
                                <th>자산명</th>
                                <th>손망 일자</th>
                                <th>비고</th>
                                <th>신청자</th>
                                <th>신청일</th>
                                <th>상태</th>
                                {isManager && <th>관리</th>}
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={columnCount} className="asset-list-empty-cell">불러오는 중...</td></tr>
                            ) : filteredRequests.length === 0 ? (
                                <tr><td colSpan={columnCount} className="asset-list-empty-cell">손망 신청 내역이 없습니다.</td></tr>
                            ) : filteredRequests.map((request) => (
                                <tr key={request.id}>
                                    <td>
                                        <button
                                            type="button"
                                            className="loss-asset-link"
                                            onClick={() => navigate(`/assets/${request.assetId}`)}
                                        >
                                            {request.assetNo || '-'}
                                        </button>
                                    </td>
                                    <td>{request.assetName || '-'}</td>
                                    <td>{formatDate(request.lossDate)}</td>
                                    <td className="loss-remarks-cell">{request.remarks || '-'}</td>
                                    <td>{request.requesterName || '-'}</td>
                                    <td>{formatDate(request.createdAt)}</td>
                                    <td>
                                        <span
                                            className={`status-badge ${statusClass[request.status] || ''}`}
                                            onClick={() => request.status === 'REJECTED'
                                                && request.rejectionReason
                                                && setReasonModal({ isOpen: true, text: request.rejectionReason })}
                                            role={request.status === 'REJECTED' ? 'button' : undefined}
                                            tabIndex={request.status === 'REJECTED' ? 0 : undefined}
                                        >
                                            {statusLabel[request.status] || request.status}
                                        </span>
                                    </td>
                                    {isManager && (
                                        <td>
                                            {request.status === 'PENDING' && (
                                                <div className="action-buttons">
                                                    <button className="btn-approve" onClick={() => handleApprove(request.id)}>승인</button>
                                                    <button className="btn-reject" onClick={() => openRejectModal(request.id)}>반려</button>
                                                </div>
                                            )}
                                        </td>
                                    )}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>

                <MessageModal
                    isOpen={reasonModal.isOpen}
                    onClose={() => setReasonModal({ isOpen: false, text: '' })}
                    title="반려 사유"
                    message={reasonModal.text}
                    buttonText="닫기"
                />

                {rejectModal.isOpen && (
                    <div className="modal-overlay" onClick={() => setRejectModal({ isOpen: false, id: null })}>
                        <div className="modal-content" onClick={(event) => event.stopPropagation()}>
                            <div className="modal-header">
                                <h2>손망 신청 반려</h2>
                                <button className="modal-close-btn" onClick={() => setRejectModal({ isOpen: false, id: null })}>&times;</button>
                            </div>
                            <div className="modal-body">
                                <p style={{ marginBottom: '10px' }}>반려 사유를 입력해주세요.</p>
                                <textarea
                                    className="form-input form-textarea"
                                    value={rejectionReason}
                                    onChange={(event) => setRejectionReason(event.target.value)}
                                    rows="4"
                                    placeholder="반려 사유 입력"
                                />
                            </div>
                            <div className="modal-actions">
                                <button className="btn-secondary" onClick={() => setRejectModal({ isOpen: false, id: null })}>취소</button>
                                <button className="btn-reject" onClick={handleReject}>반려</button>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </DashboardLayout>
    );
};

export default LossListPage;

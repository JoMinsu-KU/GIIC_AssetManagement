import React, { useCallback, useEffect, useState } from 'react';
import DashboardLayout from '../components/layout/DashboardLayout';
import { useAuth } from '../contexts/AuthContext';
import { isAdmin, isSuper } from '../utils/auth';
import { getMyTransfers, getAllTransfers, approveTransfer, rejectTransfer } from '../api/transfers';
import '../styles/Asset.css';
import '../styles/Transfer.css';

const TransferListPage = () => {
    const { user } = useAuth();
    const [transfers, setTransfers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [filter, setFilter] = useState('ALL'); // ALL, PENDING, APPROVED, REJECTED

    const loadTransfers = useCallback(async () => {
        try {
            setLoading(true);
            let response;
            if (isAdmin(user) || isSuper(user)) {
                response = await getAllTransfers();
            } else {
                response = await getMyTransfers();
            }
            // response can be array or ApiResponse object depending on backend implementation
            setTransfers(Array.isArray(response) ? response : (response.data || []));
        } catch (err) {
            console.error(err);
            // Silent error or toast
        } finally {
            setLoading(false);
        }
    }, [user]);

    useEffect(() => {
        loadTransfers();
    }, [loadTransfers]);

    const handleApprove = async (transferId) => {
        if (!window.confirm('이 이관 신청을 승인하시겠습니까?')) return;
        try {
            await approveTransfer(transferId);
            alert('승인되었습니다.');
            loadTransfers();
        } catch (err) {
            alert('승인에 실패했습니다.');
        }
    };

    const handleReject = async (transferId) => {
        const reason = window.prompt('거절 사유를 입력하세요:');
        if (!reason) return;
        try {
            await rejectTransfer(transferId, reason);
            alert('거절되었습니다.');
            loadTransfers();
        } catch (err) {
            alert('거절에 실패했습니다.');
        }
    };

    const getStatusLabel = (status) => {
        const labels = {
            'PENDING': '대기중',
            'APPROVED': '승인',
            'REJECTED': '거절'
        };
        return labels[status] || status;
    };

    const getStatusClass = (status) => {
        const classes = {
            'PENDING': 'status-pending',
            'APPROVED': 'status-approved',
            'REJECTED': 'status-rejected'
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

    const getRecipientSummary = (transfer) => {
        if (transfer.recipientMode === 'KEEP') return '기존 실사용자 유지';
        if (transfer.recipientMode === 'UNASSIGN' || transfer.assetOnly) return '실사용자 없음';
        return `${transfer.recipientName || '-'} (${transfer.recipientDepartment || '-'})`;
    };

    const getLocationSummary = (transfer) => {
        const location = transfer.transferLocation || transfer.fromLocation || '-';
        return transfer.locationMode === 'KEEP' ? `${location} (유지)` : location;
    };

    const filteredTransfers = transfers.filter(t => {
        if (filter === 'ALL') return true;
        return t.status === filter;
    });

    return (
        <DashboardLayout>
            <div className="transfer-list-page">
                <div className="page-header transfer-page-header">
                    <h1 className="page-title">이관 {(isAdmin(user) || isSuper(user)) ? '관리' : '신청 내역'}</h1>
                </div>

                {(isAdmin(user) || isSuper(user)) && (
                    <div className="filter-section transfer-filter-section">
                        <select className="form-select transfer-filter-select" value={filter} onChange={(e) => setFilter(e.target.value)}>
                            <option value="ALL">전체</option>
                            <option value="PENDING">대기중</option>
                            <option value="APPROVED">승인</option>
                            <option value="REJECTED">거절</option>
                        </select>
                    </div>
                )}

                <div className="table-container transfer-table-container">
                    <table className="asset-table transfer-table">
                        <colgroup>
                            <col className="transfer-col-asset-name" />
                            <col className="transfer-col-asset-no" />
                            <col className="transfer-col-requester" />
                            <col className="transfer-col-recipient" />
                            <col className="transfer-col-location" />
                            <col className="transfer-col-date" />
                            <col className="transfer-col-status" />
                            {(isAdmin(user) || isSuper(user)) && <col className="transfer-col-actions" />}
                        </colgroup>
                        <thead>
                            <tr>
                                <th>자산명</th>
                                <th>자산번호</th>
                                <th>신청자</th>
                                <th>이관 대상자</th>
                                <th>이관 장소</th>
                                <th>신청일</th>
                                <th>상태</th>
                                {(isAdmin(user) || isSuper(user)) && <th>관리</th>}
                            </tr>
                        </thead>
                        <tbody>
                            {loading ? (
                                <tr><td colSpan={(isAdmin(user) || isSuper(user)) ? 8 : 7} style={{ textAlign: 'center', padding: '20px' }}>로딩 중...</td></tr>
                            ) : filteredTransfers.length === 0 ? (
                                <tr><td colSpan={(isAdmin(user) || isSuper(user)) ? 8 : 7} style={{ textAlign: 'center', padding: '20px' }}>이관 내역이 없습니다.</td></tr>
                            ) : (
                                filteredTransfers.map(t => (
                                    <tr key={t.id}>
                                        <td className="transfer-cell-asset-name">{t.assetName || '-'}</td>
                                        <td className="transfer-cell-nowrap">{t.assetNo || '-'}</td>
                                        <td className="transfer-cell-nowrap">{t.requestedByName || '-'}</td>
                                        <td className="transfer-cell-content">
                                            {getRecipientSummary(t)}
                                        </td>
                                        <td className="transfer-cell-content">{getLocationSummary(t)}</td>
                                        <td className="transfer-cell-nowrap">{formatDate(t.requestDate)}</td>
                                        <td className="transfer-cell-status">
                                            <span className={`status-badge ${getStatusClass(t.status)}`}>
                                                {getStatusLabel(t.status)}
                                            </span>
                                        </td>
                                        {(isAdmin(user) || isSuper(user)) && (
                                            <td className="transfer-cell-actions">
                                                {t.status === 'PENDING' && (
                                                    <div className="action-buttons">
                                                        <button type="button" className="btn-approve" onClick={() => handleApprove(t.id)}>승인</button>
                                                        <button type="button" className="btn-reject" onClick={() => handleReject(t.id)}>거절</button>
                                                    </div>
                                                )}
                                                {t.status === 'REJECTED' && t.rejectReason && (
                                                    <span className="reject-reason" title={t.rejectReason}>사유 보기</span>
                                                )}
                                            </td>
                                        )}
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </DashboardLayout>
    );
};

export default TransferListPage;

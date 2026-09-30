import React, { useEffect } from 'react';
import '../../styles/Modal.css';

const STATUS_META = {
    PENDING: { label: '승인 대기', className: 'is-pending' },
    APPROVED: { label: '이관 완료', className: 'is-approved' },
    REJECTED: { label: '반려', className: 'is-rejected' }
};

const formatDateTime = (value) => {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;

    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
};

const displayUser = (name, department) => {
    if (!name && !department) return '-';
    if (!department) return name;
    if (!name) return department;
    return `${name} · ${department}`;
};

const displayRecipient = (transfer) => {
    if (transfer.recipientMode === 'KEEP') return '기존 사용자 유지';
    if (transfer.recipientMode === 'UNASSIGN' || transfer.assetOnly) return '사용자 없음';
    return displayUser(transfer.recipientName, transfer.recipientDepartment);
};

const displayLocation = (transfer) => {
    const fromLocation = transfer.fromLocation || '-';
    const destination = transfer.transferLocation || fromLocation;

    if (transfer.locationMode === 'KEEP') return `${destination} (유지)`;
    if (transfer.fromLocation && destination !== transfer.fromLocation) {
        return `${transfer.fromLocation} → ${destination}`;
    }
    return destination;
};

const TransferHistoryModal = ({ isOpen, onClose, asset, history }) => {
    useEffect(() => {
        if (!isOpen) return undefined;

        const handleKeyDown = (event) => {
            if (event.key === 'Escape') onClose();
        };

        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [isOpen, onClose]);

    if (!isOpen) return null;

    return (
        <div className="modal-overlay transfer-history-overlay" onClick={onClose}>
            <section
                className="modal-content transfer-history-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="transfer-history-title"
                onClick={(event) => event.stopPropagation()}
            >
                <header className="modal-header transfer-history-header">
                    <div>
                        <p className="transfer-history-overline">TRANSFER RECORD</p>
                        <h2 id="transfer-history-title">이관 이력</h2>
                        <p>{asset?.name || '-'} · 자산번호 {asset?.assetNo || '-'}</p>
                    </div>
                    <button className="modal-close-btn" type="button" onClick={onClose} aria-label="이관 이력 닫기">
                        &times;
                    </button>
                </header>

                <div className="transfer-history-body">
                    <div className="transfer-history-current">
                        <div>
                            <span>현재 실사용자</span>
                            <strong>{displayUser(asset?.actualUserName, asset?.actualUserDepartment)}</strong>
                        </div>
                        <span className="transfer-history-count">총 {history.length}건</span>
                    </div>

                    <div className="transfer-history-timeline">
                        {history.map((transfer, index) => {
                            const statusMeta = STATUS_META[transfer.status] || {
                                label: transfer.status || '-',
                                className: ''
                            };

                            return (
                                <article className="transfer-history-entry" key={transfer.id}>
                                    <div className="transfer-history-marker" aria-hidden="true" />
                                    <div className="transfer-history-entry-card">
                                        <div className="transfer-history-entry-head">
                                            <div>
                                                <div className="transfer-history-entry-title">
                                                    <strong>{displayUser(transfer.fromUserName, transfer.fromUserDepartment)}</strong>
                                                    <span aria-hidden="true">→</span>
                                                    <strong>{displayRecipient(transfer)}</strong>
                                                </div>
                                                <time>{formatDateTime(transfer.requestDate)}</time>
                                            </div>
                                            <div className="transfer-history-entry-badges">
                                                {index === 0 && <span className="transfer-history-latest">최신</span>}
                                                <span className={`transfer-history-status ${statusMeta.className}`}>
                                                    {statusMeta.label}
                                                </span>
                                            </div>
                                        </div>

                                        <dl className="transfer-history-details">
                                            <div><dt>이관 장소</dt><dd>{displayLocation(transfer)}</dd></div>
                                            <div><dt>신청자</dt><dd>{transfer.requestedByName || transfer.requestedById || '-'}</dd></div>
                                            <div><dt>처리자</dt><dd>{transfer.processedByName || transfer.processedById || '-'}</dd></div>
                                            <div><dt>처리일</dt><dd>{formatDateTime(transfer.processDate)}</dd></div>
                                        </dl>

                                        {(transfer.reason || transfer.remarks) && (
                                            <div className="transfer-history-note">
                                                <span>사유 및 비고</span>
                                                <p>{[transfer.reason, transfer.remarks].filter(Boolean).join(' · ')}</p>
                                            </div>
                                        )}

                                        {transfer.status === 'REJECTED' && transfer.rejectReason && (
                                            <div className="transfer-history-rejection">
                                                <span>반려 사유</span>
                                                <p>{transfer.rejectReason}</p>
                                            </div>
                                        )}
                                    </div>
                                </article>
                            );
                        })}
                    </div>
                </div>

                <footer className="modal-actions transfer-history-footer">
                    <button className="btn-secondary" type="button" onClick={onClose}>닫기</button>
                </footer>
            </section>
        </div>
    );
};

export default TransferHistoryModal;

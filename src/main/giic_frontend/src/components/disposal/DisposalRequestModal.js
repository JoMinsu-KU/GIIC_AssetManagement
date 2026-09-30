import React, { useState } from 'react';
import { createDisposalRequest } from '../../api/disposals';
import '../../styles/Modal.css';

const DisposalRequestModal = ({ isOpen, onClose, assetId, assetName, onSuccess }) => {
    const [reason, setReason] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!reason.trim()) {
            setError('폐기 사유는 필수 입력 항목입니다.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            await createDisposalRequest({ assetId, reason });
            alert('폐기 신청이 완료되었습니다.');
            onSuccess && onSuccess();
            onClose();
            setReason(''); // Reset form
        } catch (err) {
            console.error('Disposal request failed:', err);
            setError(err.response?.data?.message || '폐기 신청에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="request-modal" onClick={(e) => e.stopPropagation()}>
                <div className="request-modal-header">
                    <h2>폐기 신청</h2>
                    <button className="modal-close-btn" onClick={onClose}>&times;</button>
                </div>

                <div className="request-modal-body">
                    <p className="modal-subtitle">자산: <strong>{assetName}</strong></p>

                    {error && <div className="error-message modal-error">{error}</div>}

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label>폐기 사유 <span className="required">*</span></label>
                            <textarea
                                value={reason}
                                onChange={(e) => setReason(e.target.value)}
                                placeholder="폐기 사유를 상세히 입력하세요."
                                rows="4"
                                required
                            />
                        </div>

                        <div className="request-modal-footer">
                            <button type="button" className="btn-cancel" onClick={onClose} disabled={loading}>
                                취소
                            </button>
                            <button type="submit" className="btn-submit btn-danger" disabled={loading}>
                                {loading ? '처리중...' : '폐기 신청'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default DisposalRequestModal;

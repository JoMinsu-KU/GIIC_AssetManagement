import React, { useState } from 'react';
import { createLossRequest } from '../../api/losses';
import { useAuth } from '../../contexts/AuthContext';
import '../../styles/Modal.css';

const getToday = () => {
    const today = new Date();
    const offset = today.getTimezoneOffset() * 60 * 1000;
    return new Date(today.getTime() - offset).toISOString().split('T')[0];
};

const LossRequestModal = ({ isOpen, onClose, assetId, assetName, onSuccess }) => {
    const { user } = useAuth();
    const [lossDate, setLossDate] = useState(getToday);
    const [remarks, setRemarks] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    if (!isOpen) return null;

    const handleClose = () => {
        if (loading) return;
        setLossDate(getToday());
        setRemarks('');
        setError('');
        onClose();
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (!lossDate) {
            setError('손망 일자를 선택해주세요.');
            return;
        }
        if (!remarks.trim()) {
            setError('비고를 입력해주세요.');
            return;
        }

        setLoading(true);
        setError('');
        try {
            await createLossRequest({ assetId: Number(assetId), lossDate, remarks: remarks.trim() });
            window.alert('손망 신청이 완료되었습니다. 관리자 승인 후 반영됩니다.');
            if (onSuccess) await onSuccess();
            setLossDate(getToday());
            setRemarks('');
            onClose();
        } catch (requestError) {
            console.error('Loss request failed:', requestError);
            setError(requestError.response?.data?.message || '손망 신청에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="request-modal" onClick={(event) => event.stopPropagation()}>
                <div className="request-modal-header">
                    <h2>손망 신청</h2>
                    <button className="modal-close-btn" onClick={handleClose} disabled={loading}>&times;</button>
                </div>

                <div className="request-modal-body">
                    <p className="modal-subtitle">자산: <strong>{assetName}</strong></p>
                    {error && <div className="error-message modal-error">{error}</div>}

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label>신청자</label>
                            <div className="asset-static-field">{user?.name || '-'}</div>
                        </div>
                        <div className="form-group">
                            <label>손망 일자 <span className="required">*</span></label>
                            <input
                                type="date"
                                className="form-input"
                                value={lossDate}
                                max={getToday()}
                                onChange={(event) => setLossDate(event.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label>비고 <span className="required">*</span></label>
                            <textarea
                                className="form-input form-textarea"
                                value={remarks}
                                onChange={(event) => setRemarks(event.target.value)}
                                maxLength={1000}
                                rows={4}
                                placeholder="손망 경위나 확인이 필요한 내용을 입력해주세요."
                                required
                            />
                        </div>

                        <div className="request-modal-footer">
                            <button type="button" className="btn-cancel" onClick={handleClose} disabled={loading}>
                                취소
                            </button>
                            <button type="submit" className="btn-submit btn-danger" disabled={loading}>
                                {loading ? '처리중...' : '손망 신청'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default LossRequestModal;

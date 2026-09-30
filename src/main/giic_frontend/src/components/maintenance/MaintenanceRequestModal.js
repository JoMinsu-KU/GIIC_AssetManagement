import React, { useState } from 'react';
import { createMaintenanceRequest } from '../../api/maintenances';
import '../../styles/Modal.css';

const MaintenanceRequestModal = ({ isOpen, onClose, asset, onSuccess }) => {
    const [formData, setFormData] = useState({
        maintenanceType: 'REPAIR',
        description: ''
    });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    if (!isOpen) return null;

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!formData.description) {
            setError('내용을 입력해주세요.');
            return;
        }

        setLoading(true);
        setError('');

        try {
            await createMaintenanceRequest({
                assetId: asset.id,
                ...formData
            });
            alert('유지보수 신청이 완료되었습니다.');
            onSuccess && onSuccess();
            onClose();
        } catch (err) {
            console.error('Maintenance request failed:', err);
            setError(err.response?.data?.message || '유지보수 신청에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="request-modal" onClick={(e) => e.stopPropagation()}>
                <div className="request-modal-header">
                    <h2>유지보수 신청</h2>
                    <button className="modal-close-btn" onClick={onClose}>&times;</button>
                </div>

                <div className="request-modal-body">
                    <p className="modal-subtitle">자산: <strong>{asset?.name} ({asset?.assetNo})</strong></p>

                    {error && <div className="error-message modal-error">{error}</div>}

                    <form onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label>유지보수 유형 <span className="required">*</span></label>
                            <select
                                name="maintenanceType"
                                value={formData.maintenanceType}
                                onChange={handleChange}
                                required
                            >
                                <option value="REPAIR">수리</option>
                                <option value="INSPECTION">점검</option>
                                <option value="REPLACEMENT">교체</option>
                                <option value="OTHER">기타</option>
                            </select>
                        </div>
                        <div className="form-group">
                            <label>신청 사유 및 상세 내용 <span className="required">*</span></label>
                            <textarea
                                name="description"
                                value={formData.description}
                                onChange={handleChange}
                                placeholder="유지보수가 필요한 사유나 증상을 상세히 적어주세요."
                                rows="5"
                                required
                            />
                        </div>

                        <div className="request-modal-footer">
                            <button type="button" className="btn-cancel" onClick={onClose} disabled={loading}>
                                취소
                            </button>
                            <button type="submit" className="btn-submit" disabled={loading}>
                                {loading ? '처리중...' : '신청하기'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default MaintenanceRequestModal;

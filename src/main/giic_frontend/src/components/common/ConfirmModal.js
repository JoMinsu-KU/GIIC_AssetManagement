import React from 'react';
import '../../styles/Modal.css';

const ConfirmModal = ({
    isOpen,
    onClose,
    title,
    message,
    type = 'info',
    onConfirm,
    confirmText = '확인',
    cancelText = '취소'
}) => {
    if (!isOpen) return null;

    const getTitle = () => {
        if (title) return title;
        switch (type) {
            case 'success': return '성공';
            case 'error': return '오류';
            case 'warning': return '주의';
            case 'confirm': return '확인';
            default: return '알림';
        }
    };

    const getIcon = () => {
        // Simple text-based icons or could be SVGs
        switch (type) {
            case 'success': return '✅';
            case 'error': return '❌';
            case 'warning': return '⚠️';
            case 'confirm': return '❓';
            default: return 'ℹ️';
        }
    };

    const handleConfirm = () => {
        if (onConfirm) {
            onConfirm();
        }
        onClose();
    };

    return (
        <div className="modal-overlay">
            <div className="modal-content" style={{ maxWidth: '400px' }}>
                <div className="modal-header">
                    <h2>
                        <span style={{ marginRight: '8px' }}>{getIcon()}</span>
                        {getTitle()}
                    </h2>
                    <button className="modal-close-btn" onClick={onClose}>&times;</button>
                </div>
                <div className="modal-grid">
                    <p style={{ margin: '0', fontSize: '16px', lineHeight: '1.5', whiteSpace: 'pre-wrap' }}>
                        {message}
                    </p>
                </div>
                <div className="modal-actions">
                    {type === 'confirm' && (
                        <button className="btn-secondary" onClick={onClose}>
                            {cancelText}
                        </button>
                    )}
                    <button className="btn-primary" onClick={type === 'confirm' ? handleConfirm : onClose}>
                        {confirmText}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmModal;

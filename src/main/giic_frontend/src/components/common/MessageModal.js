import React from 'react';
import '../../styles/Modal.css';

const MessageModal = ({ isOpen, onClose, title, message, type = 'info', buttonText = '확인' }) => {
    if (!isOpen) return null;

    const getIcon = () => {
        switch (type) {
            case 'warning':
                return '⚠️';
            case 'error':
                return '❌';
            case 'success':
                return '✅';
            default:
                return 'ℹ️';  // info icon
        }
    };

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="modal-content message-modal" onClick={e => e.stopPropagation()} style={{ textAlign: 'center' }}>
                <div className="message-modal-icon" style={{ textAlign: 'center' }}>{getIcon()}</div>
                <h2 className="message-modal-title" style={{ textAlign: 'center', width: '100%' }}>{title || '알림'}</h2>
                <p className="message-modal-text" style={{ textAlign: 'center', width: '100%' }}>{message}</p>
                <div className="message-modal-buttons">
                    <button className="btn btn-primary" onClick={onClose}>{buttonText}</button>
                </div>
            </div>
        </div>
    );
};

export default MessageModal;

import React from 'react';
import '../../styles/Modal.css';

const SessionExtensionModal = ({ show, onExtend, onLogout, remainingTime }) => {
    if (!show) return null;

    const formatTime = (ms) => {
        const totalSeconds = Math.floor(ms / 1000);
        const minutes = Math.floor(totalSeconds / 60);
        const seconds = totalSeconds % 60;
        return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
    };

    return (
        <div className="modal-overlay">
            <div className="message-modal modal-content">
                <div className="message-modal-icon">⏰</div>
                <h3 className="message-modal-title">세션 만료 경고</h3>
                <p className="message-modal-text">
                    세션이 <strong>{formatTime(remainingTime)}</strong> 후에 만료됩니다.<br />
                    로그인을 연장하시겠습니까?
                </p>
                <div className="message-modal-buttons">
                    <button className="btn-secondary" onClick={onLogout}>
                        로그아웃
                    </button>
                    <button className="btn-primary" onClick={onExtend}>
                        연장하기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default SessionExtensionModal;

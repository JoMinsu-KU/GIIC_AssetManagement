import React, { useState } from 'react';
import TransferRequestModal from '../transfer/TransferRequestModal';
import MaintenanceRequestModal from '../maintenance/MaintenanceRequestModal';
import DisposalRequestModal from '../disposal/DisposalRequestModal';
import LossRequestModal from '../loss/LossRequestModal';
import MessageModal from '../common/MessageModal';

const getBlockedMessage = (status) => {
    if (status === 'DISPOSED') return '폐기된 자산은 요청할 수 없습니다.';
    if (status === 'ABNORMAL') return '고장 상태의 자산은 요청할 수 없습니다.';
    if (status === 'UNUSED') return '미사용 자산은 사용중으로 변경한 후 신청할 수 있습니다.';
    if (status === 'LOST') return '손망 처리된 자산은 요청할 수 없습니다.';
    return '현재 진행중인 요청이 있어 신규 요청이 불가능합니다.';
};

const AssetRequestActions = ({ asset, assetId, onSuccess }) => {
    const [activeModal, setActiveModal] = useState(null);
    const [message, setMessage] = useState('');

    const openModal = (modalName) => {
        if (asset.status !== 'NORMAL') {
            setMessage(getBlockedMessage(asset.status));
            return;
        }
        setActiveModal(modalName);
    };

    const closeModal = () => setActiveModal(null);
    const disabledClass = asset.status !== 'NORMAL' ? 'disabled-button' : '';

    return (
        <>
            <button className={`btn-secondary ${disabledClass}`} onClick={() => openModal('transfer')}>
                이관 신청
            </button>
            <button className={`btn-secondary ${disabledClass}`} onClick={() => openModal('maintenance')}>
                유지보수 신청
            </button>
            <button className={`btn-secondary ${disabledClass}`} onClick={() => openModal('disposal')}>
                폐기 신청
            </button>
            <button className={`btn-secondary ${disabledClass}`} onClick={() => openModal('loss')}>
                손망 신청
            </button>

            <TransferRequestModal
                isOpen={activeModal === 'transfer'}
                onClose={closeModal}
                assetId={assetId}
                assetName={asset.name}
                currentLocation={asset.location}
                currentUserName={asset.actualUserName}
                currentUserDepartment={asset.actualUserDepartment}
                currentUserContact={asset.actualUserContact}
                onSuccess={onSuccess}
            />
            <MaintenanceRequestModal
                isOpen={activeModal === 'maintenance'}
                onClose={closeModal}
                asset={asset}
                onSuccess={onSuccess}
            />
            <DisposalRequestModal
                isOpen={activeModal === 'disposal'}
                onClose={closeModal}
                assetId={assetId}
                assetName={asset.name}
                onSuccess={onSuccess}
            />
            <LossRequestModal
                isOpen={activeModal === 'loss'}
                onClose={closeModal}
                assetId={assetId}
                assetName={asset.name}
                onSuccess={onSuccess}
            />
            <MessageModal
                isOpen={Boolean(message)}
                onClose={() => setMessage('')}
                title="알림"
                message={message}
            />
        </>
    );
};

export default AssetRequestActions;

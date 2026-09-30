import React, { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import DashboardLayout from '../components/layout/DashboardLayout';
import {
    addAssetImages,
    deleteAsset,
    deleteAssetImage,
    getAssetDetail,
    getAssetImageUrl,
    setAssetPrimaryImage,
    updateAsset
} from '../api/assets';
import { getAssetTransferHistory } from '../api/transfers';
import AssetRequestActions from '../components/asset/AssetRequestActions';
import AssetImageGallery from '../components/asset/AssetImageGallery';
import TransferHistoryModal from '../components/transfer/TransferHistoryModal';
import ConfirmModal from '../components/common/ConfirmModal';
import MessageModal from '../components/common/MessageModal';
import '../styles/Asset.css';

const getStatusLabel = (status) => {
    switch (status) {
        case 'NORMAL': return '정상';
        case 'ABNORMAL': return '비정상';
        case 'DISPOSED': return '폐기';
        case 'REPAIRING': return '수리중';
        case 'TRANSFER_PENDING': return '이관 대기';
        case 'DISPOSAL_PENDING': return '폐기 대기';
        case 'DISPOSAL_REJECTED': return '폐기 반려';
        case 'LOSS_PENDING': return '손망 승인 대기';
        case 'LOST': return '손망';
        default: return status;
    }
};

const getStatusClass = (status) => {
    switch (status) {
        case 'NORMAL': return 'status-normal';
        case 'ABNORMAL': return 'status-abnormal';
        case 'DISPOSED': return 'status-disposed';
        case 'REPAIRING': return 'status-repairing';
        case 'TRANSFER_PENDING': return 'status-transfer-pending';
        case 'DISPOSAL_PENDING': return 'status-disposal-pending';
        case 'DISPOSAL_REJECTED': return 'status-disposal-rejected';
        case 'LOSS_PENDING': return 'status-loss-pending';
        case 'LOST': return 'status-lost';
        default: return '';
    }
};

const formatDate = (value) => {
    if (!value) return '-';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString();
};

const formatCurrency = (value) => {
    if (value === null || value === undefined || value === '') return '0원';
    return `${Number(value).toLocaleString()}원`;
};

const mapAssetImages = (images = []) => [...images]
    .sort((first, second) => Number(second.isPrimary) - Number(first.isPrimary) || first.id - second.id)
    .map((image, index) => ({
        key: `existing-${image.id}`,
        id: image.id,
        src: getAssetImageUrl(image.imageUrl),
        name: image.isPrimary ? '대표 이미지' : `자산 이미지 ${index + 1}`,
        isPrimary: image.isPrimary,
        isNew: false
    }));

const AssetDetailPage = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const requestedListPath = location.state?.assetListPath;
    const assetListPath = typeof requestedListPath === 'string' && /^\/assets(?:\?|$)/.test(requestedListPath)
        ? requestedListPath
        : '/assets';
    const navigateToAssetList = () => navigate(assetListPath);
    const [loading, setLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    const [asset, setAsset] = useState(null);
    const [formData, setFormData] = useState({});
    const [galleryImages, setGalleryImages] = useState([]);
    const [selectedImageKey, setSelectedImageKey] = useState(null);
    const [removedImageIds, setRemovedImageIds] = useState([]);
    const [transferHistory, setTransferHistory] = useState([]);
    const [isTransferHistoryOpen, setIsTransferHistoryOpen] = useState(false);
    const [messageModal, setMessageModal] = useState({ isOpen: false, title: '', message: '' });
    const [modalConfig, setModalConfig] = useState({
        isOpen: false,
        title: '',
        message: '',
        type: 'info',
        onConfirm: null
    });

    const resetGallery = (assetData) => {
        galleryImages.filter((image) => image.isNew).forEach((image) => URL.revokeObjectURL(image.src));
        const mappedImages = mapAssetImages(assetData?.images || []);
        setGalleryImages(mappedImages);
        setSelectedImageKey(mappedImages.find((image) => image.isPrimary)?.key || mappedImages[0]?.key || null);
        setRemovedImageIds([]);
    };

    const fetchAssetDetail = useCallback(async () => {
        setLoading(true);
        try {
            const [data, history] = await Promise.all([
                getAssetDetail(id),
                getAssetTransferHistory(id).catch((error) => {
                    console.error('Failed to fetch asset transfer history:', error);
                    return [];
                })
            ]);
            setAsset(data);
            setFormData(data);
            const mappedImages = mapAssetImages(data?.images || []);
            setGalleryImages((currentImages) => {
                currentImages
                    .filter((image) => image.isNew)
                    .forEach((image) => URL.revokeObjectURL(image.src));
                return mappedImages;
            });
            setSelectedImageKey(mappedImages.find((image) => image.isPrimary)?.key || mappedImages[0]?.key || null);
            setRemovedImageIds([]);
            setTransferHistory(Array.isArray(history) ? history : []);
        } catch (error) {
            console.error('Failed to fetch asset detail:', error);
            setModalConfig({
                isOpen: true,
                title: '오류',
                message: '자산 정보를 불러오는데 실패했습니다.',
                type: 'error',
                onConfirm: () => navigate(assetListPath)
            });
        } finally {
            setLoading(false);
        }
    }, [assetListPath, id, navigate]);

    useEffect(() => {
        fetchAssetDetail();
    }, [fetchAssetDetail]);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value
        }));
    };

    const startEditing = () => {
        setFormData(asset);
        resetGallery(asset);
        setIsEditing(true);
    };

    const cancelEditing = () => {
        setIsEditing(false);
        setFormData(asset);
        resetGallery(asset);
    };

    const handleGalleryFilesSelected = (files) => {
        const availableSlots = 5 - galleryImages.length;
        const selectedFiles = files.slice(0, availableSlots);
        const oversizedFile = selectedFiles.find((file) => file.size > 10 * 1024 * 1024);
        const hasPrimaryImage = galleryImages.some((image) => image.isPrimary);

        if (oversizedFile) {
            setMessageModal({
                isOpen: true,
                title: '이미지 크기 확인',
                message: '이미지 한 장의 최대 크기는 10MB입니다.'
            });
            return;
        }

        const newImages = selectedFiles.map((file, index) => ({
            key: `new-${Date.now()}-${index}-${file.name}`,
            file,
            src: URL.createObjectURL(file),
            name: file.name,
            isPrimary: !hasPrimaryImage && index === 0,
            isNew: true
        }));

        setGalleryImages((prev) => [...prev, ...newImages]);
        if (newImages.length > 0) {
            setSelectedImageKey(newImages[0].key);
        }
        if (files.length > availableSlots) {
            setMessageModal({
                isOpen: true,
                title: '이미지 개수 확인',
                message: '자산 이미지는 최대 5장까지 등록할 수 있습니다.'
            });
        }
    };

    const handleGalleryRemove = (targetImage) => {
        const remainingImages = galleryImages.filter((image) => image.key !== targetImage.key);
        if (!targetImage.isNew) {
            setRemovedImageIds((prev) => [...prev, targetImage.id]);
        } else {
            URL.revokeObjectURL(targetImage.src);
        }

        if (targetImage.isPrimary && remainingImages.length > 0) {
            remainingImages[0] = { ...remainingImages[0], isPrimary: true };
        }

        setGalleryImages(remainingImages);
        if (selectedImageKey === targetImage.key) {
            setSelectedImageKey(remainingImages[0]?.key || null);
        }
    };

    const handleGallerySetPrimary = (targetImage) => {
        setGalleryImages((prev) => prev.map((image) => ({
            ...image,
            isPrimary: image.key === targetImage.key
        })));
        setSelectedImageKey(targetImage.key);
    };

    const canEditStatus = () => {
        return asset && asset.status === 'NORMAL';
    };

    const handleUpdate = async (e) => {
        e.preventDefault();

        if (formData.status === 'DISPOSED' && asset.status === 'NORMAL') {
            setModalConfig({
                isOpen: true,
                title: '폐기 처리 확인',
                message: '자산을 폐기 처리하시겠습니까?\n폐기 후에는 상태를 되돌릴 수 없습니다.',
                type: 'confirm',
                onConfirm: () => performUpdate()
            });
        } else {
            performUpdate();
        }
    };

    const performUpdate = async () => {
        try {
            await updateAsset(id, formData);

            for (const imageId of removedImageIds) {
                await deleteAssetImage(id, imageId);
            }

            const newImages = galleryImages.filter((image) => image.isNew);
            const primaryImage = galleryImages.find((image) => image.isPrimary);
            const newPrimaryIndex = primaryImage?.isNew
                ? newImages.findIndex((image) => image.key === primaryImage.key)
                : null;

            if (newImages.length > 0) {
                await addAssetImages(
                    id,
                    newImages.map((image) => image.file),
                    newPrimaryIndex !== null && newPrimaryIndex >= 0 ? newPrimaryIndex : null
                );
            }

            if (primaryImage && !primaryImage.isNew) {
                await setAssetPrimaryImage(id, primaryImage.id);
            }

            setModalConfig({
                isOpen: true,
                title: '수정 완료',
                message: '자산 정보가 수정되었습니다.',
                type: 'success',
                onConfirm: () => {
                    galleryImages
                        .filter((image) => image.isNew)
                        .forEach((image) => URL.revokeObjectURL(image.src));
                    setIsEditing(false);
                    fetchAssetDetail();
                }
            });
        } catch (error) {
            console.error('Failed to update asset:', error);
            setModalConfig({
                isOpen: true,
                title: '수정 실패',
                message: '자산 정보 수정 중 일부 작업에 실패했습니다. 화면을 새로 확인해 주세요.',
                type: 'error'
            });
        }
    };

    const handleDeleteClick = () => {
        setModalConfig({
            isOpen: true,
            title: '삭제 확인',
            message: '정말로 이 자산을 삭제하시겠습니까?',
            type: 'confirm',
            onConfirm: performDelete
        });
    };

    const performDelete = async () => {
        try {
            await deleteAsset(id);
            setModalConfig({
                isOpen: true,
                title: '삭제 완료',
                message: '자산이 삭제되었습니다.',
                type: 'success',
                onConfirm: navigateToAssetList
            });
        } catch (error) {
            console.error('Failed to delete asset:', error);
            setModalConfig({
                isOpen: true,
                title: '삭제 실패',
                message: '자산 삭제에 실패했습니다.',
                type: 'error'
            });
        }
    };

    const handleRequestClick = (action) => {
        if (asset?.status === 'DISPOSED') {
            setMessageModal({
                isOpen: true,
                title: '알림',
                message: '폐기된 항목은 요청할 수 없습니다.'
            });
            return;
        }
        if (asset?.status === 'ABNORMAL') {
            setMessageModal({
                isOpen: true,
                title: '알림',
                message: '비정상 상태의 자산은 수정할 수 없습니다.'
            });
            return;
        }
        if (asset?.status === 'LOST') {
            setMessageModal({
                isOpen: true,
                title: '알림',
                message: '손망 처리된 자산은 요청할 수 없습니다.'
            });
            return;
        }
        if (asset?.status !== 'NORMAL') {
            setMessageModal({
                isOpen: true,
                title: '알림',
                message: '현재 진행중인 요청이 있어 신규 요청이 불가능합니다.'
            });
            return;
        }
        action();
    };

    const renderDetailRow = (label, value, className = '') => (
        <div className="asset-detail-row">
            <div className="asset-detail-label">{label}</div>
            <div className={`asset-detail-value ${className}`}>{value || '-'}</div>
        </div>
    );

    if (loading) {
        return (
            <DashboardLayout>
                <div style={{ textAlign: 'center', marginTop: '50px' }}>로딩 중...</div>
            </DashboardLayout>
        );
    }

    if (!asset) return null;

    return (
        <DashboardLayout>
            <div className="asset-detail-shell">
                <div className="asset-page-header asset-detail-page-header">
                    <div>
                        <p className="asset-detail-breadcrumb">자산관리 / 상세정보</p>
                        <h1 className="page-title">{isEditing ? '자산 수정' : '자산 상세'}</h1>
                        <p className="asset-detail-subtitle">
                            {isEditing
                                ? '데이터 구조는 그대로 유지하고, 입력 흐름만 더 읽기 쉬운 구조로 정리했습니다.'
                                : '핵심 정보와 작업 버튼을 먼저 보여주고, 세부 정보는 주제별 섹션으로 나누어 확인할 수 있게 구성했습니다.'}
                        </p>
                    </div>

                    <div className="asset-actions asset-detail-actions">
                        {!isEditing ? (
                            <>
                                <button className="btn-secondary" onClick={navigateToAssetList}>목록</button>
                                <AssetRequestActions asset={asset} assetId={id} onSuccess={fetchAssetDetail} />
                                <button
                                    className={`btn-primary ${asset.status !== 'NORMAL' ? 'disabled-button' : ''}`}
                                    onClick={() => handleRequestClick(startEditing)}
                                >
                                    수정
                                </button>
                                <button
                                    className={`btn-danger ${asset.status !== 'NORMAL' ? 'disabled-button' : ''}`}
                                    onClick={() => handleRequestClick(handleDeleteClick)}
                                >
                                    삭제
                                </button>
                            </>
                        ) : (
                            <>
                                <button
                                    className="btn-secondary"
                                    onClick={cancelEditing}
                                >
                                    취소
                                </button>
                                <button className="btn-primary" onClick={handleUpdate}>저장</button>
                            </>
                        )}
                    </div>
                </div>

                <MessageModal
                    isOpen={messageModal.isOpen}
                    onClose={() => setMessageModal({ ...messageModal, isOpen: false })}
                    title={messageModal.title}
                    message={messageModal.message}
                />

                <ConfirmModal
                    isOpen={modalConfig.isOpen}
                    onClose={() => {
                        setModalConfig((prev) => ({ ...prev, isOpen: false }));
                        if (modalConfig.type !== 'confirm' && modalConfig.onConfirm) {
                            modalConfig.onConfirm();
                        }
                    }}
                    title={modalConfig.title}
                    message={modalConfig.message}
                    type={modalConfig.type}
                    onConfirm={modalConfig.onConfirm}
                />

                <TransferHistoryModal
                    isOpen={isTransferHistoryOpen}
                    onClose={() => setIsTransferHistoryOpen(false)}
                    asset={asset}
                    history={transferHistory}
                />

                {isEditing ? (
                    <div className="form-container asset-edit-form">
                        <form onSubmit={handleUpdate}>
                            <div className="asset-edit-grid">
                                <div className="asset-edit-main">
                                    <div className="form-section">
                                        <h3 className="form-section-title">기본 정보</h3>
                                        <div className="form-grid">
                                            <div className="form-group">
                                                <label>자산번호</label>
                                                <div className="asset-static-field">{formData.assetNo || '-'}</div>
                                            </div>
                                            <div className="form-group">
                                                <label>물품명</label>
                                                <input type="text" name="name" className="form-input" value={formData.name || ''} onChange={handleChange} />
                                            </div>
                                            <div className="form-group">
                                                <label>모델명</label>
                                                <input type="text" name="model" className="form-input" value={formData.model || ''} onChange={handleChange} />
                                            </div>
                                            <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                                                <label>규격</label>
                                                <input type="text" name="spec" className="form-input" value={formData.spec || ''} onChange={handleChange} />
                                            </div>
                                        </div>
                                    </div>

                                    <div className="form-section">
                                        <h3 className="form-section-title">재무 · 관리 정보</h3>
                                        <div className="form-grid">
                                            <div className="form-group">
                                                <label>취득금액</label>
                                                <input type="number" name="value" className="form-input" value={formData.value || ''} onChange={handleChange} />
                                            </div>
                                            <div className="form-group">
                                                <label>취득일자</label>
                                                <input
                                                    type="date"
                                                    name="purchaseDate"
                                                    className="form-input"
                                                    value={formData.purchaseDate ? formData.purchaseDate.toString().split('T')[0] : ''}
                                                    onChange={handleChange}
                                                />
                                            </div>
                                            <div className="form-group">
                                                <label>관리부서</label>
                                                <input type="text" name="managingDept" className="form-input" value={formData.managingDept || ''} onChange={handleChange} />
                                            </div>
                                            <div className="form-group">
                                                <label>비치장소</label>
                                                <input type="text" name="location" className="form-input" value={formData.location || ''} onChange={handleChange} />
                                            </div>
                                            <div className="form-group">
                                                <label>상태</label>
                                                {canEditStatus() ? (
                                                    <select
                                                        name="status"
                                                        className="form-select"
                                                        value={formData.status || 'NORMAL'}
                                                        onChange={handleChange}
                                                    >
                                                        <option value="NORMAL">정상</option>
                                                        <option value="DISPOSED">폐기</option>
                                                    </select>
                                                ) : (
                                                    <div className="asset-static-field">
                                                        <span className={`status-badge ${getStatusClass(formData.status)}`}>
                                                            {getStatusLabel(formData.status)}
                                                        </span>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </div>

                                    <div className="form-section">
                                        <h3 className="form-section-title">실사용자 정보</h3>
                                        <div className="form-grid">
                                            <div className="form-group">
                                                <label>사용자 이름</label>
                                                <div className="asset-static-field">{formData.actualUserName || '-'}</div>
                                            </div>
                                            <div className="form-group">
                                                <label>사용자 부서</label>
                                                <div className="asset-static-field">{formData.actualUserDepartment || '-'}</div>
                                            </div>
                                            <div className="form-group">
                                                <label>연락처</label>
                                                <div className="asset-static-field">{formData.actualUserContact || '-'}</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div className="asset-edit-side">
                                    <div className="detail-card asset-image-card">
                                        <AssetImageGallery
                                            images={galleryImages}
                                            selectedKey={selectedImageKey}
                                            onSelect={setSelectedImageKey}
                                            editing
                                            onFilesSelected={handleGalleryFilesSelected}
                                            onRemove={handleGalleryRemove}
                                            onSetPrimary={handleGallerySetPrimary}
                                        />
                                    </div>
                                </div>
                            </div>
                        </form>
                    </div>
                ) : (
                    <>
                        <div className="asset-detail-hero">
                            <div className="detail-card asset-detail-summary-card">
                                <div className="asset-summary-header">
                                    <div>
                                        <p className="asset-summary-overline">핵심 요약</p>
                                        <h2 className="asset-summary-title">{asset.name || '-'}</h2>
                                        <p className="asset-summary-meta">
                                            자산번호 {asset.assetNo || '-'} · 모델명 {asset.model || '-'}
                                        </p>
                                    </div>
                                    <span className={`status-badge asset-summary-status ${getStatusClass(asset.status)}`}>
                                        {getStatusLabel(asset.status)}
                                    </span>
                                </div>

                                {asset.status === 'LOST' && (
                                    <div className="asset-loss-alert">
                                        <div className="asset-loss-alert-title">손망 자산</div>
                                        <div className="asset-loss-alert-details">
                                            <span>손망 일자 <strong>{formatDate(asset.lossDate)}</strong></span>
                                            <span>신청자 <strong>{asset.lossRequesterName || asset.lossRequesterId || '-'}</strong></span>
                                            <span className="asset-loss-alert-remarks">
                                                비고 <strong>{asset.lossRemarks || '-'}</strong>
                                            </span>
                                        </div>
                                    </div>
                                )}

                                <div className="asset-summary-grid">
                                    <div className="asset-summary-item">
                                        <span className="asset-summary-label">관리부서</span>
                                        <strong className="asset-summary-value-tight">{asset.managingDept || '-'}</strong>
                                    </div>
                                    <div className="asset-summary-item">
                                        <span className="asset-summary-label">취득금액</span>
                                        <strong>{formatCurrency(asset.value)}</strong>
                                    </div>
                                    <div className="asset-summary-item">
                                        <span className="asset-summary-label">취득일자</span>
                                        <strong>{formatDate(asset.purchaseDate)}</strong>
                                    </div>
                                    <div className="asset-summary-item">
                                        <span className="asset-summary-label">등록자</span>
                                        <strong>{asset.registeredByName || '-'}</strong>
                                    </div>
                                </div>
                            </div>

                            <div className="detail-card asset-image-card">
                                <AssetImageGallery
                                    images={galleryImages}
                                    selectedKey={selectedImageKey}
                                    onSelect={setSelectedImageKey}
                                />
                            </div>
                        </div>

                        <div className="asset-detail-grid">
                            <div className="detail-card">
                                <h3 className="form-section-title">기본 정보</h3>
                                <div className="asset-detail-list">
                                    {renderDetailRow('자산번호', asset.assetNo, 'asset-detail-strong')}
                                    {renderDetailRow('물품명', asset.name)}
                                    {renderDetailRow('모델명', asset.model)}
                                    {renderDetailRow('규격', asset.spec)}
                                    {renderDetailRow('등록일', formatDate(asset.createdAt))}
                                </div>
                            </div>

                            <div className="detail-card">
                                <h3 className="form-section-title">재무 · 관리 정보</h3>
                                <div className="asset-detail-list">
                                    {renderDetailRow('취득금액', formatCurrency(asset.value))}
                                    {renderDetailRow('취득일자', formatDate(asset.purchaseDate))}
                                    {renderDetailRow('관리부서', asset.managingDept)}
                                    {renderDetailRow('비치장소', asset.location)}
                                    <div className="asset-detail-row">
                                        <div className="asset-detail-label">상태</div>
                                        <div className="asset-detail-value">
                                            <span className={`status-badge ${getStatusClass(asset.status)}`}>
                                                {getStatusLabel(asset.status)}
                                            </span>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="detail-card">
                                <h3 className="form-section-title">등록 정보</h3>
                                <div className="asset-detail-list">
                                    {renderDetailRow('등록자', asset.registeredByName)}
                                    {renderDetailRow('등록자 사번', asset.registeredById)}
                                    {renderDetailRow('최근 수정', formatDate(asset.updatedAt || asset.createdAt))}
                                </div>
                            </div>

                            <div className="detail-card asset-current-user-card">
                                <div className="asset-detail-card-header">
                                    <div>
                                        <h3 className="form-section-title">실사용자 정보</h3>
                                        <p className="asset-current-user-caption">현재 기준 최신 정보</p>
                                    </div>
                                    {transferHistory.length > 0 && (
                                        <button
                                            className="asset-transfer-history-button"
                                            type="button"
                                            onClick={() => setIsTransferHistoryOpen(true)}
                                        >
                                            이관 이력 <span>{transferHistory.length}</span>
                                        </button>
                                    )}
                                </div>
                                <div className="asset-detail-list">
                                    {renderDetailRow('사용자 이름', asset.actualUserName)}
                                    {renderDetailRow('사용자 부서', asset.actualUserDepartment)}
                                    {renderDetailRow('연락처', asset.actualUserContact)}
                                </div>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </DashboardLayout>
    );
};

export default AssetDetailPage;


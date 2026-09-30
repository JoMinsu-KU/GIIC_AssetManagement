import React, { useRef, useState } from 'react';

const AssetImageGallery = ({
    images = [],
    selectedKey,
    onSelect,
    editing = false,
    onFilesSelected,
    onRemove,
    onSetPrimary,
    maxImages = 5
}) => {
    const fileInputRef = useRef(null);
    const [viewerOpen, setViewerOpen] = useState(false);

    const requestedIndex = images.findIndex((image) => image.key === selectedKey);
    const primaryIndex = images.findIndex((image) => image.isPrimary);
    const selectedIndex = requestedIndex >= 0 ? requestedIndex : primaryIndex >= 0 ? primaryIndex : 0;
    const selectedImage = images[selectedIndex] || null;

    const moveSelection = (direction) => {
        if (images.length < 2) return;
        const nextIndex = (selectedIndex + direction + images.length) % images.length;
        onSelect?.(images[nextIndex].key);
    };

    const handleFiles = (event) => {
        const files = Array.from(event.target.files || []);
        if (files.length > 0) {
            onFilesSelected?.(files);
        }
        event.target.value = '';
    };

    return (
        <div className={`asset-gallery ${editing ? 'asset-gallery-editing' : ''}`}>
            <div className="asset-gallery-header">
                <div>
                    <h3 className="form-section-title">자산 이미지</h3>
                    <p>
                        {editing
                            ? '사진을 추가하거나 제거하고 대표 이미지를 변경할 수 있습니다.'
                            : '썸네일을 선택하면 큰 이미지로 확인할 수 있습니다.'}
                    </p>
                </div>
                <span className="asset-gallery-mode">{editing ? '수정 모드' : `${images.length}장`}</span>
            </div>

            <div className="asset-gallery-layout">
                <div className="asset-gallery-thumbnails" aria-label="자산 이미지 목록">
                    {images.map((image, index) => (
                        <div className="asset-gallery-thumb-wrap" key={image.key}>
                            <button
                                type="button"
                                className={`asset-gallery-thumb ${index === selectedIndex ? 'active' : ''}`}
                                onClick={() => onSelect?.(image.key)}
                                aria-label={`${image.name}${image.isPrimary ? ', 대표 이미지' : ''}`}
                            >
                                <img src={image.src} alt="" />
                                {image.isPrimary && <span className="asset-gallery-primary-badge">대표</span>}
                            </button>
                            {editing && (
                                <button
                                    type="button"
                                    className="asset-gallery-remove"
                                    onClick={() => onRemove?.(image)}
                                    aria-label={`${image.name} 삭제`}
                                >
                                    ×
                                </button>
                            )}
                        </div>
                    ))}

                    {editing && images.length < maxImages && (
                        <button
                            type="button"
                            className="asset-gallery-add"
                            onClick={() => fileInputRef.current?.click()}
                        >
                            <strong>+</strong>
                            <span>사진 추가</span>
                        </button>
                    )}
                </div>

                <div className="asset-gallery-main">
                    {selectedImage ? (
                        <>
                            <img src={selectedImage.src} alt={selectedImage.name} />
                            {images.length > 1 && (
                                <>
                                    <button
                                        type="button"
                                        className="asset-gallery-arrow previous"
                                        onClick={() => moveSelection(-1)}
                                        aria-label="이전 이미지"
                                    >
                                        ‹
                                    </button>
                                    <button
                                        type="button"
                                        className="asset-gallery-arrow next"
                                        onClick={() => moveSelection(1)}
                                        aria-label="다음 이미지"
                                    >
                                        ›
                                    </button>
                                </>
                            )}
                            <button
                                type="button"
                                className="asset-gallery-expand"
                                onClick={() => setViewerOpen(true)}
                                aria-label="이미지 확대"
                            >
                                ⛶
                            </button>
                            <span className="asset-gallery-count">{selectedIndex + 1} / {images.length}</span>
                        </>
                    ) : (
                        <div className="asset-gallery-empty">
                            <strong>등록된 이미지가 없습니다.</strong>
                            <span>{editing ? '사진 추가 버튼으로 이미지를 등록해 주세요.' : '수정 화면에서 사진을 등록할 수 있습니다.'}</span>
                        </div>
                    )}
                </div>
            </div>

            <div className="asset-gallery-footer">
                <div className="asset-gallery-caption">
                    {selectedImage ? (
                        <><strong>{selectedImage.isPrimary ? '대표 이미지' : '추가 이미지'}</strong>{selectedImage.name}</>
                    ) : '이미지 미등록'}
                </div>
                {editing && selectedImage && !selectedImage.isPrimary && (
                    <button
                        type="button"
                        className="btn-secondary asset-gallery-primary-button"
                        onClick={() => onSetPrimary?.(selectedImage)}
                    >
                        대표 이미지로 지정
                    </button>
                )}
            </div>

            <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/jpg,image/png,image/webp"
                multiple
                hidden
                onChange={handleFiles}
            />

            {viewerOpen && selectedImage && (
                <div
                    className="asset-gallery-viewer"
                    role="dialog"
                    aria-modal="true"
                    aria-label="이미지 확대 보기"
                    onClick={() => setViewerOpen(false)}
                >
                    <div className="asset-gallery-viewer-dialog" onClick={(event) => event.stopPropagation()}>
                        <button type="button" onClick={() => setViewerOpen(false)} aria-label="닫기">×</button>
                        <img src={selectedImage.src} alt={selectedImage.name} />
                    </div>
                </div>
            )}
        </div>
    );
};

export default AssetImageGallery;

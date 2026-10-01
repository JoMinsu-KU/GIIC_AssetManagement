import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/layout/DashboardLayout';
import { addAssetImages, createAsset, getAssetCategories, uploadAssetImage, ocrAssetImage } from '../api/assets';
import AssetCategoryFields from '../components/asset/AssetCategoryFields';
import '../styles/Asset.css';

const MAX_ADDITIONAL_IMAGES = 4;

const AssetCreatePage = () => {
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);
    const [ocrLoading, setOcrLoading] = useState(false);
    const [errors, setErrors] = useState({});
    const [imageFile, setImageFile] = useState(null);
    const [imagePreview, setImagePreview] = useState(null);
    const [additionalImages, setAdditionalImages] = useState([]);
    const [primaryImageKey, setPrimaryImageKey] = useState(null);
    const [autoFilledFields, setAutoFilledFields] = useState([]);
    const [categories, setCategories] = useState([]);

    const [formData, setFormData] = useState({
        assetNo: '',
        name: '',
        model: '',
        spec: '',
        majorCategoryId: '',
        categoryId: '',
        value: '',
        purchaseDate: '',
        managingDept: '',
        location: '',
        status: 'NORMAL',
        actualUserName: '',
        actualUserDepartment: '',
        actualUserContact: ''
    });

    useEffect(() => {
        getAssetCategories().then(setCategories).catch((error) => {
            console.error('Failed to load asset categories:', error);
        });
    }, []);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value,
            ...(name === 'majorCategoryId' ? { categoryId: '' } : {})
        }));

        if (errors[name]) {
            setErrors(prev => ({ ...prev, [name]: null }));
        }
        if (name === 'majorCategoryId') {
            setErrors(prev => ({ ...prev, categoryId: null }));
        }
    };

    const handleImageChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            if (file.size > 10 * 1024 * 1024) {
                alert('이미지 한 장의 최대 크기는 10MB입니다.');
                e.target.value = '';
                return;
            }

            const isFirstOcrImage = !imageFile;
            setImageFile(file);
            const reader = new FileReader();
            reader.onloadend = () => setImagePreview(reader.result);
            reader.readAsDataURL(file);
            setAutoFilledFields([]);
            if (isFirstOcrImage || !primaryImageKey) {
                setPrimaryImageKey('ocr');
            }
        }
        e.target.value = '';
    };

    const readFilePreview = (file) => new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = reject;
        reader.readAsDataURL(file);
    });

    const handleAdditionalImagesChange = async (e) => {
        const availableSlots = MAX_ADDITIONAL_IMAGES - additionalImages.length;
        const requestedFiles = Array.from(e.target.files || []);
        const selectedFiles = requestedFiles.slice(0, availableSlots);
        e.target.value = '';

        if (selectedFiles.length === 0) return;

        const oversizedFile = selectedFiles.find((file) => file.size > 10 * 1024 * 1024);
        if (oversizedFile) {
            alert('이미지 한 장의 최대 크기는 10MB입니다.');
            return;
        }

        let newImages;
        try {
            newImages = await Promise.all(selectedFiles.map(async (file, index) => ({
                key: `new-${Date.now()}-${index}-${file.name}`,
                file,
                preview: await readFilePreview(file)
            })));
        } catch (error) {
            console.error('Failed to preview additional images:', error);
            alert('선택한 이미지 미리보기를 불러오지 못했습니다.');
            return;
        }

        setAdditionalImages((prev) => [...prev, ...newImages]);
        if (!primaryImageKey && !imageFile) {
            setPrimaryImageKey(newImages[0].key);
        }

        if (requestedFiles.length > availableSlots) {
            alert(`추가 사진은 최대 ${MAX_ADDITIONAL_IMAGES}장까지 등록할 수 있습니다.`);
        }
    };

    const handleRemoveAdditionalImage = (imageKey) => {
        const remainingImages = additionalImages.filter((image) => image.key !== imageKey);
        setAdditionalImages(remainingImages);
        if (primaryImageKey === imageKey) {
            setPrimaryImageKey(imageFile ? 'ocr' : remainingImages[0]?.key || null);
        }
    };

    const handleOcrProcess = async () => {
        if (!imageFile) {
            alert('먼저 이미지를 업로드해 주세요.');
            return;
        }

        setOcrLoading(true);
        try {
            const response = await ocrAssetImage(imageFile);
            const ocrData = response?.data || response;

            if (!ocrData) {
                throw new Error('OCR 응답 데이터가 없습니다.');
            }

            const updatedFields = [];
            const newFormData = { ...formData };

            if (ocrData.assetNo) {
                newFormData.assetNo = ocrData.assetNo;
                updatedFields.push('assetNo');
            }
            if (ocrData.name) {
                newFormData.name = ocrData.name;
                updatedFields.push('name');
            }
            if (ocrData.managingDept) {
                newFormData.managingDept = ocrData.managingDept;
                updatedFields.push('managingDept');
            }
            if (ocrData.purchaseDate) {
                const formattedDate = ocrData.purchaseDate.replace(/\./g, '-');
                newFormData.purchaseDate = formattedDate;
                updatedFields.push('purchaseDate');
            }
            if (ocrData.spec) {
                newFormData.spec = ocrData.spec;
                updatedFields.push('spec');
            }

            setFormData(newFormData);
            setAutoFilledFields(updatedFields);

            if (updatedFields.length > 0) {
                alert(`OCR 완료: ${updatedFields.length}개 필드가 자동 입력되었습니다.`);
            } else {
                alert('이미지에서 정보를 추출하지 못했습니다. 직접 입력해 주세요.');
            }
        } catch (error) {
            console.error('OCR failed:', error);
            const errorMessage = error.response?.data?.error?.message ||
                'OCR 처리에 실패했습니다. 직접 입력해 주세요.';
            alert(errorMessage);
        } finally {
            setOcrLoading(false);
        }
    };

    const getInputClassName = (fieldName) => {
        const baseClass = 'form-input';
        return autoFilledFields.includes(fieldName)
            ? `${baseClass} auto-filled`
            : baseClass;
    };

    const validate = () => {
        const newErrors = {};
        if (!formData.assetNo) newErrors.assetNo = '자산번호는 필수입니다.';
        if (!formData.name) newErrors.name = '물품명은 필수입니다.';
        if (!formData.majorCategoryId) newErrors.majorCategoryId = '대분류를 선택해 주세요.';
        if (!formData.categoryId) newErrors.categoryId = '중분류를 선택해 주세요.';
        if (!formData.model) newErrors.model = '모델명은 필수입니다.';
        if (!formData.managingDept) newErrors.managingDept = '관리부서는 필수입니다.';
        if (!formData.purchaseDate) newErrors.purchaseDate = '취득일자는 필수입니다.';
        if (formData.status === 'UNUSED' && !formData.location.trim()) {
            newErrors.location = '미사용 자산은 비치장소를 입력해 주세요.';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validate()) return;

        setLoading(true);
        let createdAssetId = null;
        try {
            const dataToSubmit = {
                ...formData,
                categoryId: Number(formData.categoryId),
                value: formData.value ? parseInt(formData.value, 10) : 0
            };
            delete dataToSubmit.majorCategoryId;

            const response = await createAsset(dataToSubmit);
            console.log('Create asset response:', response);

            const assetId = response;
            createdAssetId = assetId;

            if (imageFile && assetId) {
                await uploadAssetImage(assetId, imageFile, primaryImageKey === 'ocr');
            }

            if (additionalImages.length > 0 && assetId) {
                const selectedPrimaryIndex = additionalImages.findIndex((image) => image.key === primaryImageKey);
                await addAssetImages(
                    assetId,
                    additionalImages.map((image) => image.file),
                    selectedPrimaryIndex >= 0 ? selectedPrimaryIndex : null
                );
            }

            navigate('/assets');
        } catch (error) {
            console.error('Failed to create asset:', error);
            if (createdAssetId) {
                alert('자산 정보는 등록되었지만 일부 이미지 저장에 실패했습니다. 상세 화면에서 이미지를 다시 등록해 주세요.');
                navigate(`/assets/${createdAssetId}`);
            } else {
                alert('자산 등록에 실패했습니다. 다시 시도해 주세요.');
            }
        } finally {
            setLoading(false);
        }
    };

    const renderAutoBadge = (fieldName) => (
        autoFilledFields.includes(fieldName) ? <span className="auto-filled-badge">AI 입력</span> : null
    );

    return (
        <DashboardLayout>
            <form onSubmit={handleSubmit} className="asset-create-shell">
                <div className="asset-create-page-header">
                    <div>
                        <div className="asset-create-overline">Asset Create</div>
                        <h1 className="page-title">자산 등록</h1>
                        <p className="asset-create-subtitle">
                            OCR 보조 입력은 유지하되, 실제 등록 흐름이 더 또렷하게 보이도록 입력 섹션 중심으로 정리했습니다.
                        </p>
                    </div>
                    <div className="asset-create-top-actions">
                        <button
                            type="button"
                            className="btn-secondary"
                            onClick={() => navigate('/assets')}
                            disabled={loading}
                        >
                            취소
                        </button>
                        <button
                            type="submit"
                            className="btn-primary asset-create-submit-top"
                            disabled={loading}
                        >
                            {loading ? '등록 중...' : '등록'}
                        </button>
                    </div>
                </div>

                <div className="asset-create-layout">
                    <div className="asset-create-main">
                        <section className="detail-card asset-create-hero-card asset-create-images-card">
                            <div className="asset-create-images-head">
                                <div>
                                    <h3 className="form-section-title">자산 이미지</h3>
                                    <p>OCR 대상과 추가 사진의 역할을 분리했습니다. 추가 사진 등록은 선택 사항입니다.</p>
                                </div>
                                <span className="asset-create-image-count">
                                    {(imageFile ? 1 : 0) + additionalImages.length} / 5장
                                </span>
                            </div>

                            <div className="asset-create-image-workspace">
                                <section className="asset-create-image-step">
                                    <div className="asset-create-image-step-head">
                                        <div>
                                            <span className="asset-create-step-number">1</span>
                                            <strong>OCR 대상 이미지</strong>
                                        </div>
                                        <span>1장</span>
                                    </div>

                                    <div className="asset-create-ocr-preview">
                                        {imagePreview ? (
                                            <>
                                                <img src={imagePreview} alt="OCR 대상 자산 라벨" />
                                                <div className="asset-create-image-badges">
                                                    <span>OCR 대상</span>
                                                    {primaryImageKey === 'ocr' && <span className="primary">대표</span>}
                                                </div>
                                            </>
                                        ) : (
                                            <div className="asset-create-image-empty">
                                                <strong>자산 라벨 이미지를 올려주세요</strong>
                                                <span>OCR 결과는 아래 입력 필드에 자동으로 반영됩니다.</span>
                                            </div>
                                        )}
                                    </div>

                                    <div className="asset-create-ocr-controls">
                                        <span className={`asset-create-ocr-status ${autoFilledFields.length > 0 ? 'complete' : ''}`}>
                                            {autoFilledFields.length > 0 ? `${autoFilledFields.length}개 항목 추출 완료` : 'OCR 추출 전'}
                                        </span>
                                        <div>
                                            {imageFile && primaryImageKey !== 'ocr' && (
                                                <button type="button" className="btn-secondary" onClick={() => setPrimaryImageKey('ocr')}>
                                                    대표로 지정
                                                </button>
                                            )}
                                            <label htmlFor="ocr-image-input" className="btn-secondary asset-create-file-button">
                                                {imageFile ? '이미지 교체' : '파일 선택'}
                                            </label>
                                            <button
                                                type="button"
                                                className="btn-primary asset-create-ocr-button"
                                                onClick={handleOcrProcess}
                                                disabled={ocrLoading || !imageFile}
                                            >
                                                {ocrLoading ? 'OCR 처리 중...' : autoFilledFields.length > 0 ? '다시 추출' : 'AI 자동 추출'}
                                            </button>
                                        </div>
                                        <input
                                            type="file"
                                            accept="image/jpeg,image/jpg,image/png,image/webp"
                                            onChange={handleImageChange}
                                            className="asset-create-file-input"
                                            id="ocr-image-input"
                                        />
                                    </div>
                                </section>

                                <section className="asset-create-image-step">
                                    <div className="asset-create-image-step-head">
                                        <div>
                                            <span className="asset-create-step-number">2</span>
                                            <strong>추가 사진</strong>
                                        </div>
                                        <span>{additionalImages.length} / {MAX_ADDITIONAL_IMAGES}장 · 선택</span>
                                    </div>

                                    <div className="asset-create-additional-grid">
                                        {additionalImages.map((image) => (
                                            <article
                                                key={image.key}
                                                className={`asset-create-additional-item ${primaryImageKey === image.key ? 'primary' : ''}`}
                                            >
                                                <img src={image.preview} alt={image.file.name} />
                                                <button
                                                    type="button"
                                                    className="asset-create-image-remove"
                                                    onClick={() => handleRemoveAdditionalImage(image.key)}
                                                    aria-label={`${image.file.name} 삭제`}
                                                >
                                                    ×
                                                </button>
                                                {primaryImageKey === image.key && (
                                                    <div className="asset-create-image-badges"><span className="primary">대표</span></div>
                                                )}
                                                <div className="asset-create-additional-footer">
                                                    <span>{image.file.name}</span>
                                                    {primaryImageKey !== image.key && (
                                                        <button type="button" onClick={() => setPrimaryImageKey(image.key)}>대표로 지정</button>
                                                    )}
                                                </div>
                                            </article>
                                        ))}

                                        {additionalImages.length < MAX_ADDITIONAL_IMAGES && (
                                            <label htmlFor="additional-image-input" className="asset-create-additional-add">
                                                <strong>＋</strong>
                                                <span>사진 추가</span>
                                            </label>
                                        )}
                                    </div>

                                    <input
                                        id="additional-image-input"
                                        type="file"
                                        accept="image/jpeg,image/jpg,image/png,image/webp"
                                        multiple
                                        className="asset-create-file-input"
                                        onChange={handleAdditionalImagesChange}
                                    />
                                    <p className="asset-create-additional-help">
                                        자산 전체 모습, 라벨 위치, 설치 환경 등을 최대 4장까지 추가할 수 있습니다.
                                    </p>
                                </section>
                            </div>
                        </section>

                        <section className="detail-card asset-create-section-card">
                            <div className="asset-create-section-head">
                                <h3 className="form-section-title">기본 정보</h3>
                                <span className="asset-create-section-caption">등록에 필요한 핵심 식별 정보입니다.</span>
                            </div>
                            <div className="form-grid asset-create-form-grid">
                                <div className="form-group">
                                    <label>
                                        자산번호 <span className="required">*</span>
                                        {renderAutoBadge('assetNo')}
                                    </label>
                                    <input type="text" name="assetNo" className={getInputClassName('assetNo')} value={formData.assetNo} onChange={handleChange} />
                                    {errors.assetNo && <span className="error-message">{errors.assetNo}</span>}
                                </div>
                                <div className="form-group">
                                    <label>
                                        물품명 <span className="required">*</span>
                                        {renderAutoBadge('name')}
                                    </label>
                                    <input type="text" name="name" className={getInputClassName('name')} value={formData.name} onChange={handleChange} />
                                    {errors.name && <span className="error-message">{errors.name}</span>}
                                </div>
                                <div className="form-group">
                                    <label>모델명 <span className="required">*</span></label>
                                    <input type="text" name="model" className={getInputClassName('model')} value={formData.model} onChange={handleChange} />
                                    {errors.model && <span className="error-message">{errors.model}</span>}
                                </div>
                                <div className="form-group asset-create-full-width">
                                    <label>
                                        규격
                                        {renderAutoBadge('spec')}
                                    </label>
                                    <input type="text" name="spec" className={getInputClassName('spec')} value={formData.spec} onChange={handleChange} />
                                </div>
                                <AssetCategoryFields
                                    categories={categories}
                                    majorCategoryId={formData.majorCategoryId}
                                    categoryId={formData.categoryId}
                                    onChange={handleChange}
                                    errors={errors}
                                />
                            </div>
                        </section>

                        <section className="detail-card asset-create-section-card">
                            <div className="asset-create-section-head">
                                <h3 className="form-section-title">재무 · 관리 정보</h3>
                                <span className="asset-create-section-caption">회계와 운영 기준에 필요한 필드입니다.</span>
                            </div>
                            <div className="form-grid asset-create-form-grid">
                                <div className="form-group">
                                    <label>취득금액</label>
                                    <input type="number" name="value" className="form-input" value={formData.value} onChange={handleChange} placeholder="0" />
                                </div>
                                <div className="form-group">
                                    <label>
                                        취득일자 <span className="required">*</span>
                                        {renderAutoBadge('purchaseDate')}
                                    </label>
                                    <input type="date" name="purchaseDate" className={getInputClassName('purchaseDate')} value={formData.purchaseDate} onChange={handleChange} />
                                    {errors.purchaseDate && <span className="error-message">{errors.purchaseDate}</span>}
                                </div>
                                <div className="form-group">
                                    <label>상태</label>
                                    <select name="status" className="form-select" value={formData.status} onChange={handleChange}>
                                        <option value="NORMAL">정상/사용중</option>
                                        <option value="ABNORMAL">고장</option>
                                        <option value="UNUSED">미사용</option>
                                        <option value="REPAIRING">수리 중</option>
                                        <option value="DISPOSED">폐기</option>
                                    </select>
                                    {formData.status === 'UNUSED' && <p className="asset-status-help">미사용 자산은 비치장소가 필요하며 실사용자 정보는 저장하지 않습니다.</p>}
                                </div>
                                <div className="form-group">
                                    <label>
                                        관리부서 <span className="required">*</span>
                                        {renderAutoBadge('managingDept')}
                                    </label>
                                    <input type="text" name="managingDept" className={getInputClassName('managingDept')} value={formData.managingDept} onChange={handleChange} />
                                    {errors.managingDept && <span className="error-message">{errors.managingDept}</span>}
                                </div>
                                <div className="form-group asset-create-span-two">
                                    <label>비치장소</label>
                                    <input type="text" name="location" className="form-input" value={formData.location} onChange={handleChange} />
                                    {errors.location && <span className="error-message">{errors.location}</span>}
                                </div>
                            </div>
                        </section>

                        <section className="detail-card asset-create-section-card">
                            <div className="asset-create-section-head">
                                <h3 className="form-section-title">실사용자 정보</h3>
                                <span className="asset-create-section-caption">배정 전 자산이면 비워두고 등록할 수 있습니다.</span>
                            </div>
                            <div className="form-grid asset-create-form-grid">
                                <div className="form-group">
                                    <label>사용자 이름</label>
                                    <input type="text" name="actualUserName" className="form-input" value={formData.actualUserName} onChange={handleChange} disabled={formData.status === 'UNUSED'} />
                                </div>
                                <div className="form-group">
                                    <label>사용자 부서</label>
                                    <input type="text" name="actualUserDepartment" className="form-input" value={formData.actualUserDepartment} onChange={handleChange} disabled={formData.status === 'UNUSED'} />
                                </div>
                                <div className="form-group">
                                    <label>연락처</label>
                                    <input type="text" name="actualUserContact" className="form-input" value={formData.actualUserContact} onChange={handleChange} disabled={formData.status === 'UNUSED'} />
                                </div>
                            </div>
                        </section>

                        <div className="asset-create-bottom-bar">
                            <div className="asset-create-bottom-copy">
                                필수 입력값과 OCR 자동 입력값을 함께 검토한 뒤 자산을 등록하는 흐름입니다.
                            </div>
                            <div className="asset-create-bottom-actions">
                                <button
                                    type="button"
                                    className="btn-secondary"
                                    onClick={() => navigate('/assets')}
                                    disabled={loading}
                                >
                                    취소
                                </button>
                                <button type="submit" className="btn-primary asset-create-submit-bottom" disabled={loading}>
                                    {loading ? '등록 중...' : '자산 등록'}
                                </button>
                            </div>
                        </div>
                    </div>

                    <aside className="asset-create-side">
                        <section className="detail-card asset-create-summary-card">
                            <h3 className="asset-create-summary-title">등록 체크</h3>
                            <div className="asset-create-summary-list">
                                <div className="asset-create-summary-item"><span>필수 항목</span><strong>자산번호, 물품명, 분류, 모델명, 관리부서, 취득일자</strong></div>
                                <div className="asset-create-summary-item"><span>전체 이미지</span><strong>{(imageFile ? 1 : 0) + additionalImages.length}장</strong></div>
                                <div className="asset-create-summary-item"><span>대표 이미지</span><strong>{primaryImageKey === 'ocr' ? 'OCR 대상' : primaryImageKey ? '추가 사진' : '없음'}</strong></div>
                                <div className="asset-create-summary-item"><span>추가 사진</span><strong>{additionalImages.length}장</strong></div>
                                <div className="asset-create-summary-item"><span>자동 입력 필드</span><strong>{autoFilledFields.length}개</strong></div>
                                <div className="asset-create-summary-item"><span>기본 상태</span><strong>{formData.status}</strong></div>
                            </div>
                        </section>

                        <section className="detail-card asset-create-note-card">
                            <h3 className="asset-create-summary-title">화면 의도</h3>
                            <p>
                                OCR 이미지는 기본 대표로 시작하며, 추가 사진을 대표로 바꿀 수 있습니다. 추가 사진을 등록하지 않아도 기존과 동일하게 한 장만 저장됩니다.
                            </p>
                        </section>
                    </aside>
                </div>
            </form>
        </DashboardLayout>
    );
};

export default AssetCreatePage;

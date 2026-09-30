import React, { startTransition, useEffect, useRef, useState } from 'react';
import { exportAssets, getAssetExportPreview } from '../api/assets';

const COLUMN_DEFINITIONS = [
    { key: 'assetNo', label: '자산번호', width: 132 },
    { key: 'name', label: '물품명', width: 150 },
    { key: 'model', label: '모델명', width: 150 },
    { key: 'spec', label: '규격', width: 180 },
    { key: 'value', label: '취득금액', width: 128 },
    { key: 'purchaseDate', label: '취득일자', width: 124 },
    { key: 'managingDept', label: '관리부서', width: 210 },
    { key: 'location', label: '비치장소', width: 230 },
    { key: 'status', label: '상태', width: 112 },
    { key: 'actualUserName', label: '실사용자', width: 120 },
    { key: 'actualUserDepartment', label: '사용자 부서', width: 160 },
    { key: 'actualUserContact', label: '연락처', width: 140 },
    { key: 'registeredByName', label: '등록자', width: 120 },
    { key: 'createdAt', label: '등록일', width: 150 }
];

const DEFAULT_COLUMNS = [
    'assetNo',
    'name',
    'model',
    'spec',
    'value',
    'purchaseDate',
    'managingDept',
    'location',
    'status'
];

const COLUMN_PRESETS = [
    { label: '기본', columns: ['assetNo', 'name', 'model', 'status'] },
    { label: '재무', columns: ['assetNo', 'name', 'value', 'purchaseDate'] },
    { label: '관리', columns: ['assetNo', 'name', 'managingDept', 'location', 'actualUserName', 'status'] },
    { label: '전체', columns: COLUMN_DEFINITIONS.map(column => column.key) }
];

const EMPTY_FILTERS = {
    keyword: '',
    assetNo: '',
    name: '',
    model: '',
    status: '',
    dept: '',
    location: '',
    actualUserName: '',
    purchaseDateFrom: '',
    purchaseDateTo: '',
    valueMin: '',
    valueMax: ''
};

const STATUS_LABELS = {
    NORMAL: '정상',
    ABNORMAL: '고장',
    UNUSED: '미사용',
    DISPOSED: '폐기',
    REPAIRING: '수리중',
    TRANSFER_PENDING: '이관 대기',
    DISPOSAL_PENDING: '폐기 대기',
    DISPOSAL_REJECTED: '폐기 반려',
    LOSS_PENDING: '손망 승인 대기',
    LOST: '손망'
};

const getInitialFilters = (initialFilters) => ({
    ...EMPTY_FILTERS,
    keyword: initialFilters?.keyword || '',
    status: initialFilters?.status || '',
    dept: initialFilters?.dept || '',
    location: initialFilters?.location || ''
});

const cleanFilters = (filters) => Object.entries(filters).reduce((result, [key, value]) => {
    if (value === '' || value === null || value === undefined) return result;

    if (key === 'valueMin' || key === 'valueMax') {
        result[key] = Number(value);
    } else {
        result[key] = typeof value === 'string' ? value.trim() : value;
    }
    return result;
}, {});

const formatDate = (value) => {
    if (!value) return '-';
    const datePart = String(value).split('T')[0];
    return datePart.replace(/-/g, '. ');
};

const formatDateTime = (value) => {
    if (!value) return '-';
    return new Date(value).toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
};

const renderPreviewValue = (asset, key) => {
    const value = asset[key];
    if (key === 'actualUserName' && asset.sharedUse) return '공용';
    if (key === 'value') return value === null || value === undefined ? '-' : `${Number(value).toLocaleString('ko-KR')}원`;
    if (key === 'purchaseDate') return formatDate(value);
    if (key === 'createdAt') return formatDateTime(value);
    if (key === 'status') return STATUS_LABELS[value] || value || '-';
    return value || '-';
};

const AssetExportModal = ({ isOpen, onClose, initialFilters }) => {
    const latestPreviewRequestId = useRef(0);
    const previewLoadedRef = useRef(false);
    const [selectedColumns, setSelectedColumns] = useState(DEFAULT_COLUMNS);
    const [filters, setFilters] = useState(() => getInitialFilters(initialFilters));
    const [previewRows, setPreviewRows] = useState([]);
    const [totalElements, setTotalElements] = useState(0);
    const [previewLoading, setPreviewLoading] = useState(false);
    const [downloadLoading, setDownloadLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');

    const listKeyword = initialFilters?.keyword || '';
    const listStatus = initialFilters?.status || '';
    const listDept = initialFilters?.dept || '';
    const listLocation = initialFilters?.location || '';

    useEffect(() => {
        if (!isOpen) return;
        setSelectedColumns(DEFAULT_COLUMNS);
        setFilters(getInitialFilters({
            keyword: listKeyword,
            status: listStatus,
            dept: listDept,
            location: listLocation
        }));
        setPreviewRows([]);
        setTotalElements(0);
        previewLoadedRef.current = false;
        setErrorMessage('');
    }, [isOpen, listKeyword, listStatus, listDept, listLocation]);

    useEffect(() => {
        if (!isOpen) return undefined;

        const previousOverflow = document.body.style.overflow;
        const handleKeyDown = (event) => {
            if (event.key === 'Escape' && !downloadLoading) onClose();
        };

        document.body.style.overflow = 'hidden';
        window.addEventListener('keydown', handleKeyDown);

        return () => {
            document.body.style.overflow = previousOverflow;
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [isOpen, downloadLoading, onClose]);

    useEffect(() => {
        if (!isOpen) return undefined;

        const requestId = latestPreviewRequestId.current + 1;
        latestPreviewRequestId.current = requestId;
        setPreviewLoading(true);
        setErrorMessage('');
        let transitionScheduled = false;

        const timerId = setTimeout(async () => {
            try {
                const pageResponse = await getAssetExportPreview(cleanFilters(filters));

                if (requestId !== latestPreviewRequestId.current) return;

                const pageInfo = pageResponse?.pageInfo || pageResponse?.page_info;
                transitionScheduled = true;
                previewLoadedRef.current = true;
                startTransition(() => {
                    setPreviewRows(pageResponse?.content || []);
                    setTotalElements(pageInfo?.totalElements ?? pageInfo?.total_elements ?? 0);
                    setPreviewLoading(false);
                });
            } catch (error) {
                if (requestId !== latestPreviewRequestId.current) return;
                console.error('Failed to load export preview:', error);
                setPreviewRows([]);
                setTotalElements(0);
                setErrorMessage('미리보기를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
            } finally {
                if (requestId === latestPreviewRequestId.current && !transitionScheduled) {
                    setPreviewLoading(false);
                }
            }
        }, previewLoadedRef.current ? 350 : 80);

        return () => clearTimeout(timerId);
    }, [isOpen, filters]);

    if (!isOpen) return null;

    const selectedColumnDefinitions = COLUMN_DEFINITIONS.filter(column => selectedColumns.includes(column.key));

    const handleColumnToggle = (columnKey) => {
        setSelectedColumns(current => {
            const nextColumns = current.includes(columnKey)
                ? current.filter(key => key !== columnKey)
                : [...current, columnKey];
            return COLUMN_DEFINITIONS
                .filter(column => nextColumns.includes(column.key))
                .map(column => column.key);
        });
    };

    const handleFilterChange = (event) => {
        const { name, value } = event.target;
        setFilters(current => ({ ...current, [name]: value }));
    };

    const handleDownload = async () => {
        if (selectedColumns.length === 0 || totalElements === 0) return;

        setDownloadLoading(true);
        setErrorMessage('');
        try {
            const fileBlob = await exportAssets({
                columns: selectedColumns,
                filters: cleanFilters(filters)
            });
            const downloadUrl = window.URL.createObjectURL(fileBlob);
            const anchor = document.createElement('a');
            const date = new Date().toISOString().slice(0, 10).replace(/-/g, '');

            anchor.href = downloadUrl;
            anchor.download = `자산목록_${date}.xlsx`;
            document.body.appendChild(anchor);
            anchor.click();
            anchor.remove();
            window.URL.revokeObjectURL(downloadUrl);
        } catch (error) {
            console.error('Failed to export assets:', error);
            setErrorMessage('Excel 파일을 생성하지 못했습니다. 필터 조건을 확인하고 다시 시도해주세요.');
        } finally {
            setDownloadLoading(false);
        }
    };

    return (
        <div
            className="asset-export-overlay"
            role="presentation"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget && !downloadLoading) onClose();
            }}
        >
            <section className="asset-export-modal" role="dialog" aria-modal="true" aria-labelledby="asset-export-title">
                <header className="asset-export-header">
                    <div>
                        <span className="asset-export-overline">Excel Export</span>
                        <h2 id="asset-export-title">자산 목록 내려받기</h2>
                        <p>필요한 컬럼과 조회 조건을 선택한 뒤, 미리보기 그대로 Excel 파일을 생성합니다.</p>
                    </div>
                    <button
                        type="button"
                        className="asset-export-close"
                        onClick={onClose}
                        disabled={downloadLoading}
                        aria-label="닫기"
                    >
                        ×
                    </button>
                </header>

                <div className="asset-export-body">
                    <section className="asset-export-section">
                        <div className="asset-export-section-head">
                            <div>
                                <span className="asset-export-step">01</span>
                                <h3>포함할 정보</h3>
                            </div>
                            <div className="asset-export-presets" aria-label="컬럼 빠른 선택">
                                {COLUMN_PRESETS.map(preset => (
                                    <button
                                        type="button"
                                        key={preset.label}
                                        onClick={() => setSelectedColumns(preset.columns)}
                                    >
                                        {preset.label}
                                    </button>
                                ))}
                            </div>
                        </div>

                        <div className="asset-export-column-grid">
                            {COLUMN_DEFINITIONS.map(column => {
                                const checked = selectedColumns.includes(column.key);
                                return (
                                    <label
                                        key={column.key}
                                        className={`asset-export-column-option ${checked ? 'selected' : ''}`}
                                    >
                                        <input
                                            type="checkbox"
                                            checked={checked}
                                            onChange={() => handleColumnToggle(column.key)}
                                        />
                                        <span className="asset-export-checkmark" aria-hidden="true">{checked ? '✓' : ''}</span>
                                        <span>{column.label}</span>
                                    </label>
                                );
                            })}
                        </div>
                        <p className={`asset-export-selection-count ${selectedColumns.length === 0 ? 'warning' : ''}`}>
                            {selectedColumns.length > 0
                                ? `${selectedColumns.length}개 컬럼을 선택했습니다.`
                                : 'Excel에 포함할 컬럼을 하나 이상 선택해주세요.'}
                        </p>
                    </section>

                    <section className="asset-export-section">
                        <div className="asset-export-section-head">
                            <div>
                                <span className="asset-export-step">02</span>
                                <h3>내려받을 자산 조건</h3>
                            </div>
                            <button
                                type="button"
                                className="asset-export-reset"
                                onClick={() => setFilters(EMPTY_FILTERS)}
                            >
                                조건 초기화
                            </button>
                        </div>

                        <div className="asset-export-filter-grid">
                            <label className="asset-export-field asset-export-field-wide">
                                <span>통합 검색</span>
                                <input name="keyword" value={filters.keyword} onChange={handleFilterChange} placeholder="자산번호, 물품명, 모델명" />
                            </label>
                            <label className="asset-export-field">
                                <span>상태</span>
                                <select name="status" value={filters.status} onChange={handleFilterChange}>
                                    <option value="">전체 상태</option>
                                    <option value="NORMAL">정상</option>
                                    <option value="ABNORMAL">고장</option>
                                    <option value="UNUSED">미사용</option>
                                    <option value="REPAIRING">수리중</option>
                                    <option value="TRANSFER_PENDING">이관 대기</option>
                                    <option value="DISPOSAL_PENDING">폐기 대기</option>
                                    <option value="DISPOSAL_REJECTED">폐기 반려</option>
                                    <option value="DISPOSED">폐기</option>
                                    <option value="LOSS_PENDING">손망 승인 대기</option>
                                    <option value="LOST">손망</option>
                                </select>
                            </label>
                            <label className="asset-export-field">
                                <span>관리부서</span>
                                <input name="dept" value={filters.dept} onChange={handleFilterChange} placeholder="부서명" />
                            </label>
                            <label className="asset-export-field">
                                <span>자산번호</span>
                                <input name="assetNo" value={filters.assetNo} onChange={handleFilterChange} placeholder="예: 2411309" />
                            </label>
                            <label className="asset-export-field">
                                <span>물품명</span>
                                <input name="name" value={filters.name} onChange={handleFilterChange} placeholder="물품명 포함 검색" />
                            </label>
                            <label className="asset-export-field">
                                <span>모델명</span>
                                <input name="model" value={filters.model} onChange={handleFilterChange} placeholder="모델명 포함 검색" />
                            </label>
                            <label className="asset-export-field">
                                <span>비치장소</span>
                                <input name="location" value={filters.location} onChange={handleFilterChange} placeholder="현재 비치장소" />
                            </label>
                            <label className="asset-export-field">
                                <span>실사용자</span>
                                <input name="actualUserName" value={filters.actualUserName} onChange={handleFilterChange} placeholder="사용자 이름" />
                            </label>
                            <label className="asset-export-field asset-export-range-field">
                                <span>취득일자</span>
                                <div>
                                    <input type="date" name="purchaseDateFrom" value={filters.purchaseDateFrom} onChange={handleFilterChange} />
                                    <i>~</i>
                                    <input type="date" name="purchaseDateTo" value={filters.purchaseDateTo} onChange={handleFilterChange} />
                                </div>
                            </label>
                            <label className="asset-export-field asset-export-range-field">
                                <span>취득금액</span>
                                <div>
                                    <input type="number" min="0" name="valueMin" value={filters.valueMin} onChange={handleFilterChange} placeholder="최소 금액" />
                                    <i>~</i>
                                    <input type="number" min="0" name="valueMax" value={filters.valueMax} onChange={handleFilterChange} placeholder="최대 금액" />
                                </div>
                            </label>
                        </div>
                    </section>

                    <section className="asset-export-section asset-export-preview-section">
                        <div className="asset-export-section-head">
                            <div>
                                <span className="asset-export-step">03</span>
                                <h3>Excel 미리보기</h3>
                            </div>
                            <div className="asset-export-result-count">
                                <strong>{totalElements.toLocaleString('ko-KR')}</strong>건 조회
                                {totalElements > 20 && <span> · 미리보기는 20건까지 표시</span>}
                                {previewLoading && previewRows.length > 0 && <span> · 조건 반영 중</span>}
                            </div>
                        </div>

                        <div className="asset-export-preview-wrap">
                            {previewLoading && previewRows.length === 0 ? (
                                <div className="asset-export-preview-state">조회 조건을 반영하고 있습니다.</div>
                            ) : selectedColumns.length === 0 ? (
                                <div className="asset-export-preview-state">표시할 컬럼을 선택해주세요.</div>
                            ) : previewRows.length === 0 ? (
                                <div className="asset-export-preview-state">조건에 맞는 자산이 없습니다.</div>
                            ) : (
                                <table className="asset-export-preview-table">
                                    <colgroup>
                                        <col className="asset-export-row-number-column" />
                                        {selectedColumnDefinitions.map(column => (
                                            <col key={column.key} />
                                        ))}
                                    </colgroup>
                                    <thead>
                                        <tr>
                                            <th className="asset-export-row-number">#</th>
                                            {selectedColumnDefinitions.map(column => (
                                                <th key={column.key}>{column.label}</th>
                                            ))}
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {previewRows.map((asset, index) => (
                                            <tr key={asset.id || `${asset.assetNo}-${index}`}>
                                                <td className="asset-export-row-number">{index + 1}</td>
                                                {selectedColumnDefinitions.map(column => {
                                                    const previewValue = renderPreviewValue(asset, column.key);
                                                    return (
                                                        <td key={column.key} title={String(previewValue)}>
                                                            {previewValue}
                                                        </td>
                                                    );
                                                })}
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    </section>
                </div>

                <footer className="asset-export-footer">
                    <div>
                        {errorMessage && <p className="asset-export-error" role="alert">{errorMessage}</p>}
                        {!errorMessage && <p>현재 선택한 조건에 해당하는 전체 자산이 내려받아집니다.</p>}
                    </div>
                    <div className="asset-export-footer-actions">
                        <button type="button" className="btn-secondary" onClick={onClose} disabled={downloadLoading}>취소</button>
                        <button
                            type="button"
                            className="btn-primary asset-export-download"
                            onClick={handleDownload}
                            disabled={downloadLoading || previewLoading || selectedColumns.length === 0 || totalElements === 0}
                        >
                            {downloadLoading ? 'Excel 생성 중...' : `Excel 다운로드 (${totalElements.toLocaleString('ko-KR')}건)`}
                        </button>
                    </div>
                </footer>
            </section>
        </div>
    );
};

export default AssetExportModal;

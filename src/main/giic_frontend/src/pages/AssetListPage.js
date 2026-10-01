import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useCallback, useRef } from 'react';
import DashboardLayout from '../components/layout/DashboardLayout';
import AssetExportModal from '../components/AssetExportModal';
import { getAssets, getAssetCategories, getAssetImageUrl } from '../api/assets';
import { getSubcategories } from '../components/asset/AssetCategoryFields';
import '../styles/Asset.css';

const DEFAULT_FILTERS = {
    keyword: '',
    status: '',
    majorCategoryId: '',
    categoryId: '',
    dept: '',
    location: ''
};

const PAGE_SIZE = 10;

const getPageFromSearchParams = (searchParams) => {
    const page = Number.parseInt(searchParams.get('page'), 10);
    return Number.isInteger(page) && page > 0 ? page - 1 : 0;
};

const getFiltersFromSearchParams = (searchParams) => ({
    keyword: searchParams.get('keyword') || '',
    status: searchParams.get('status') || '',
    majorCategoryId: searchParams.get('majorCategoryId') || '',
    categoryId: searchParams.get('categoryId') || '',
    dept: searchParams.get('dept') || '',
    location: searchParams.get('location') || ''
});

const buildListSearchParams = (page, filters) => {
    const params = new URLSearchParams();
    if (page > 0) params.set('page', String(page + 1));
    if (filters.keyword.trim()) params.set('keyword', filters.keyword.trim());
    if (filters.status) params.set('status', filters.status);
    if (filters.majorCategoryId) params.set('majorCategoryId', filters.majorCategoryId);
    if (filters.categoryId) params.set('categoryId', filters.categoryId);
    if (filters.dept.trim()) params.set('dept', filters.dept.trim());
    if (filters.location.trim()) params.set('location', filters.location.trim());
    return params;
};

const areFiltersEqual = (left, right) => (
    left.keyword === right.keyword
    && left.status === right.status
    && left.majorCategoryId === right.majorCategoryId
    && left.categoryId === right.categoryId
    && left.dept === right.dept
    && left.location === right.location
);

const getPrimaryAssetImage = (images = []) => (
    images.find((image) => image.isPrimary) || images[0] || null
);

const AssetListPage = () => {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const latestRequestId = useRef(0);
    const filterTimerRef = useRef(null);
    const initialPageRef = useRef(getPageFromSearchParams(searchParams));
    const initialFiltersRef = useRef(getFiltersFromSearchParams(searchParams));
    const lastScheduledFiltersRef = useRef(initialFiltersRef.current);
    const initialLoadCompletedRef = useRef(false);
    const [assets, setAssets] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(true);
    const [pageInfo, setPageInfo] = useState({
        total_elements: 0,
        total_pages: 0,
        current_page: initialPageRef.current,
        size: PAGE_SIZE
    });

    const [filters, setFilters] = useState(initialFiltersRef.current);
    const [isExportModalOpen, setIsExportModalOpen] = useState(false);

    useEffect(() => {
        getAssetCategories().then(setCategories).catch((error) => {
            console.error('Failed to load asset categories:', error);
        });
    }, []);

    const getStatusLabel = (status) => {
        const labels = {
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
        return labels[status] || status;
    };

    const getStatusClass = (status) => {
        const classes = {
            NORMAL: 'status-normal',
            ABNORMAL: 'status-abnormal',
            UNUSED: 'status-unused',
            DISPOSED: 'status-disposed',
            REPAIRING: 'status-repairing',
            TRANSFER_PENDING: 'status-transfer-pending',
            DISPOSAL_PENDING: 'status-disposal-pending',
            DISPOSAL_REJECTED: 'status-disposal-rejected',
            LOSS_PENDING: 'status-loss-pending',
            LOST: 'status-lost'
        };
        return classes[status] || '';
    };

    const fetchAssets = useCallback(async (page = 0, activeFilters = DEFAULT_FILTERS) => {
        const requestId = latestRequestId.current + 1;
        latestRequestId.current = requestId;
        setLoading(true);
        try {
            const params = {
                page: page,
                size: PAGE_SIZE,
                keyword: activeFilters.keyword.trim(),
                status: activeFilters.status,
                majorCategoryId: activeFilters.majorCategoryId,
                categoryId: activeFilters.categoryId,
                dept: activeFilters.dept.trim(),
                location: activeFilters.location.trim()
            };

            Object.keys(params).forEach(key => {
                if (params[key] === '') delete params[key];
            });

            const pageResponse = await getAssets(params);
            if (requestId !== latestRequestId.current) return;

            if (pageResponse) {
                setAssets(pageResponse.content || []);
                const info = pageResponse.pageInfo || pageResponse.page_info;
                if (info) {
                    const responsePage = info.page ?? 0;
                    setPageInfo(prev => ({
                        ...prev,
                        total_elements: info.totalElements ?? info.total_elements ?? 0,
                        total_pages: info.totalPages ?? info.total_pages ?? 0,
                        current_page: responsePage > 0 ? responsePage - 1 : page
                    }));
                }
            }
        } catch (error) {
            if (requestId === latestRequestId.current) {
                console.error('Failed to fetch assets:', error);
            }
        } finally {
            if (requestId === latestRequestId.current) {
                setLoading(false);
            }
        }
    }, []);

    const syncListUrl = useCallback((page, activeFilters) => {
        setSearchParams(buildListSearchParams(page, activeFilters), { replace: true });
    }, [setSearchParams]);

    useEffect(() => {
        const filtersChanged = !areFiltersEqual(filters, lastScheduledFiltersRef.current);
        if (initialLoadCompletedRef.current && !filtersChanged) {
            return undefined;
        }

        lastScheduledFiltersRef.current = filters;
        const shouldRestoreInitialPage = !initialLoadCompletedRef.current
            && areFiltersEqual(filters, initialFiltersRef.current);
        const pageToLoad = shouldRestoreInitialPage ? initialPageRef.current : 0;

        filterTimerRef.current = setTimeout(() => {
            initialLoadCompletedRef.current = true;
            syncListUrl(pageToLoad, filters);
            fetchAssets(pageToLoad, filters);
        }, 350);

        return () => clearTimeout(filterTimerRef.current);
    }, [fetchAssets, filters, syncListUrl]);

    const handleSearch = (e) => {
        e.preventDefault();
        clearTimeout(filterTimerRef.current);
        initialLoadCompletedRef.current = true;
        syncListUrl(0, filters);
        fetchAssets(0, filters);
    };

    const handleFilterChange = (e) => {
        const { name, value } = e.target;
        setFilters(prev => ({
            ...prev,
            [name]: value,
            ...(name === 'majorCategoryId' ? { categoryId: '' } : {})
        }));
    };

    const handleResetFilters = () => {
        setFilters(DEFAULT_FILTERS);
    };

    const handleCloseExportModal = useCallback(() => {
        setIsExportModalOpen(false);
    }, []);

    const handlePageChange = (newPage) => {
        if (newPage >= 0 && newPage < pageInfo.total_pages) {
            clearTimeout(filterTimerRef.current);
            initialLoadCompletedRef.current = true;
            setPageInfo(prev => ({ ...prev, current_page: newPage }));
            syncListUrl(newPage, filters);
            fetchAssets(newPage, filters);
        }
    };

    const handleAssetOpen = (assetId) => {
        const params = buildListSearchParams(pageInfo.current_page, filters).toString();
        const assetListPath = `/assets${params ? `?${params}` : ''}`;
        navigate(`/assets/${assetId}`, { state: { assetListPath } });
    };

    const formatDate = (dateString) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleDateString('ko-KR');
    };

    const hasActiveFilters = Object.values(filters).some(value => value !== '');

    return (
        <DashboardLayout>
            <div className="asset-list-shell">
                <div className="asset-list-page-header">
                    <div>
                        <div className="asset-list-overline">Asset Directory</div>
                        <h1 className="page-title">자산 목록</h1>
                        <p className="asset-list-subtitle">
                            자산 현황을 확인하고 검색 조건에 맞는 항목을 빠르게 찾아볼 수 있습니다.
                        </p>
                    </div>
                    <div className="asset-actions">
                        <button
                            type="button"
                            className="btn-secondary asset-list-export-button"
                            onClick={() => setIsExportModalOpen(true)}
                        >
                            <span className="asset-list-export-icon" aria-hidden="true">↓</span>
                            엑셀 내려받기
                        </button>
                        <button
                            className="btn-primary asset-list-create-button"
                            onClick={() => navigate('/assets/new')}
                        >
                            <span>+</span> 자산 등록
                        </button>
                    </div>
                </div>

                <form className="asset-list-filter-panel" onSubmit={handleSearch}>
                    <div className="asset-list-filter-grid">
                        <div className="filter-group asset-list-filter-group">
                            <label className="filter-label">검색어</label>
                            <input
                                type="text"
                                name="keyword"
                                className="form-input asset-list-input"
                                placeholder="자산번호, 물품명, 모델명, 실사용자명, 공용"
                                value={filters.keyword}
                                onChange={handleFilterChange}
                            />
                        </div>
                        <div className="filter-group asset-list-filter-group">
                            <label className="filter-label">상태</label>
                            <select
                                name="status"
                                className="form-select asset-list-select"
                                value={filters.status}
                                onChange={handleFilterChange}
                            >
                                <option value="">전체</option>
                                <option value="NORMAL">정상</option>
                                <option value="ABNORMAL">고장</option>
                                <option value="UNUSED">미사용</option>
                                <option value="DISPOSED">폐기</option>
                                <option value="REPAIRING">수리중</option>
                                <option value="TRANSFER_PENDING">이관 대기</option>
                                <option value="DISPOSAL_PENDING">폐기 대기</option>
                                <option value="DISPOSAL_REJECTED">폐기 반려</option>
                                <option value="LOSS_PENDING">손망 승인 대기</option>
                                <option value="LOST">손망</option>
                            </select>
                        </div>
                        <div className="filter-group asset-list-filter-group">
                            <label className="filter-label">관리부서</label>
                            <input
                                type="text"
                                name="dept"
                                className="form-input asset-list-input"
                                placeholder="부서명"
                                value={filters.dept}
                                onChange={handleFilterChange}
                            />
                        </div>
                        <div className="filter-group asset-list-filter-group">
                            <label className="filter-label">대분류</label>
                            <select name="majorCategoryId" className="form-select asset-list-select" value={filters.majorCategoryId} onChange={handleFilterChange}>
                                <option value="">전체 대분류</option>
                                {categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}
                            </select>
                        </div>
                        <div className="filter-group asset-list-filter-group">
                            <label className="filter-label">중분류</label>
                            <select name="categoryId" className="form-select asset-list-select" value={filters.categoryId} onChange={handleFilterChange} disabled={!filters.majorCategoryId}>
                                <option value="">전체 중분류</option>
                                {getSubcategories(categories, filters.majorCategoryId).map((category) => (
                                    <option key={category.id} value={category.id}>{category.name}</option>
                                ))}
                            </select>
                        </div>
                        <div className="filter-group asset-list-filter-group">
                            <label className="filter-label">비치 장소</label>
                            <input
                                type="text"
                                name="location"
                                className="form-input asset-list-input"
                                placeholder="장소명"
                                value={filters.location}
                                onChange={handleFilterChange}
                            />
                        </div>
                        <div className="asset-list-filter-actions">
                            <button
                                type="button"
                                className="btn-secondary asset-list-reset-button"
                                onClick={handleResetFilters}
                                disabled={!hasActiveFilters}
                            >
                                초기화
                            </button>
                            <button type="submit" className="btn-primary asset-list-search-button">검색</button>
                        </div>
                    </div>
                </form>

                <div className="asset-list-table-card">
                    <div className="asset-list-table-header">
                        <div>
                            <h2 className="asset-list-table-title">전체 자산</h2>
                            <p className="asset-list-table-caption">
                                총 {pageInfo.total_elements || assets.length}개의 자산이 조회되었습니다.
                            </p>
                        </div>
                    </div>

                    <div className="asset-list-table-wrap">
                        <table className="asset-table asset-list-table">
                            <colgroup>
                                <col className="asset-list-col-image" />
                                <col className="asset-list-col-number" />
                                <col className="asset-list-col-name" />
                                <col className="asset-list-col-model" />
                                <col className="asset-list-col-status" />
                                <col className="asset-list-col-department" />
                                <col className="asset-list-col-location" />
                                <col className="asset-list-col-user" />
                                <col className="asset-list-col-date" />
                            </colgroup>
                            <thead>
                                <tr>
                                    <th>이미지</th>
                                    <th>자산번호</th>
                                    <th>물품명</th>
                                    <th>모델명</th>
                                    <th>상태</th>
                                    <th>관리부서</th>
                                    <th>비치 장소</th>
                                    <th>실사용자</th>
                                    <th>등록일</th>
                                </tr>
                            </thead>
                            <tbody>
                                {loading ? (
                                    <tr>
                                        <td colSpan="9" className="asset-list-empty-cell">
                                            자산 정보를 불러오는 중입니다.
                                        </td>
                                    </tr>
                                ) : assets.length === 0 ? (
                                    <tr>
                                        <td colSpan="9" className="asset-list-empty-cell">
                                            검색 조건에 맞는 자산이 없습니다.
                                        </td>
                                    </tr>
                                ) : (
                                    assets.map((asset) => {
                                        const primaryImage = getPrimaryAssetImage(asset.images);
                                        return (
                                        <tr
                                            key={asset.id}
                                            className="asset-list-row"
                                            onClick={() => handleAssetOpen(asset.id)}
                                        >
                                            <td>
                                                {primaryImage ? (
                                                    <img
                                                        src={getAssetImageUrl(primaryImage.imageUrl)}
                                                        alt={asset.name}
                                                        className="asset-thumbnail asset-list-thumbnail"
                                                    />
                                                ) : (
                                                    <div className="asset-list-thumbnail-placeholder">NO IMAGE</div>
                                                )}
                                            </td>
                                            <td>
                                                <span className="asset-list-asset-no">{asset.assetNo}</span>
                                            </td>
                                            <td>
                                                <span className="asset-list-asset-name">{asset.name}</span>
                                                <span className="asset-list-category">{[asset.majorCategory, asset.subcategory].filter(Boolean).join(' / ') || '-'}</span>
                                            </td>
                                            <td title={asset.model || ''}>
                                                <span className="asset-list-clamped-text">{asset.model || '-'}</span>
                                            </td>
                                            <td className="asset-list-status-cell">
                                                <span className={`status-badge ${getStatusClass(asset.status)}`}>
                                                    {getStatusLabel(asset.status)}
                                                </span>
                                            </td>
                                            <td title={asset.managingDept || ''}>
                                                <span className="asset-list-clamped-text">{asset.managingDept || '-'}</span>
                                            </td>
                                            <td className="asset-list-location" title={asset.location || ''}>
                                                <span className="asset-list-clamped-text">{asset.location || '-'}</span>
                                            </td>
                                            <td className="asset-list-user-cell" title={asset.sharedUse ? '공용 자산' : (asset.actualUserName || '')}>
                                                {asset.sharedUse
                                                    ? <span className="asset-usage-badge">공용</span>
                                                    : (asset.actualUserName || '-')}
                                            </td>
                                            <td className="asset-list-date-cell">{formatDate(asset.createdAt)}</td>
                                        </tr>
                                        );
                                    })
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>

                {!loading && pageInfo.total_pages > 0 && (
                    <div className="pagination asset-list-pagination">
                        <button
                            className="pagination-btn pagination-nav-btn"
                            disabled={pageInfo.current_page === 0}
                            onClick={() => handlePageChange(pageInfo.current_page - 1)}
                        >
                            이전
                        </button>

                        {[...Array(pageInfo.total_pages)].map((_, i) => {
                            if (
                                i === 0 ||
                                i === pageInfo.total_pages - 1 ||
                                (i >= pageInfo.current_page - 2 && i <= pageInfo.current_page + 2)
                            ) {
                                return (
                                    <button
                                        key={i}
                                        className={`pagination-btn ${i === pageInfo.current_page ? 'active' : ''}`}
                                        onClick={() => handlePageChange(i)}
                                    >
                                        {i + 1}
                                    </button>
                                );
                            } else if (
                                i === pageInfo.current_page - 3 ||
                                i === pageInfo.current_page + 3
                            ) {
                                return <span key={i} className="asset-list-pagination-ellipsis">...</span>;
                            }
                            return null;
                        })}

                        <button
                            className="pagination-btn pagination-nav-btn"
                            disabled={pageInfo.current_page === pageInfo.total_pages - 1}
                            onClick={() => handlePageChange(pageInfo.current_page + 1)}
                        >
                            다음
                        </button>
                    </div>
                )}

                <AssetExportModal
                    isOpen={isExportModalOpen}
                    onClose={handleCloseExportModal}
                    initialFilters={filters}
                    categories={categories}
                />
            </div>
        </DashboardLayout>
    );
};

export default AssetListPage;

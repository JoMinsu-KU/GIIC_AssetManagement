import React from 'react';
import { Link } from 'react-router-dom';
import '../../styles/Dashboard.css';

import { useState, useEffect } from 'react';
import { getRecentAssets } from '../../api/assets';

const RecentAssets = () => {
    const [assets, setAssets] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchAssets = async () => {
            try {
                const result = await getRecentAssets(5);
                if (result && result.content) {
                    setAssets(result.content);
                }
            } catch (error) {
                console.error('Failed to fetch recent assets', error);
            } finally {
                setLoading(false);
            }
        };

        fetchAssets();
    }, []);

    const getStatusLabel = (status) => {
        const labels = {
            NORMAL: '정상',
            ABNORMAL: '비정상',
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

    if (loading) {
        return (
            <div className="widget-card dashboard-widget-card">
                <div className="widget-header dashboard-widget-header">
                    <div>
                        <h3 className="widget-title">최근 등록 자산</h3>
                        <p className="dashboard-widget-subtitle">가장 최근 등록된 자산 5건</p>
                    </div>
                </div>
                <div className="dashboard-widget-empty">최근 자산 정보를 불러오는 중입니다.</div>
            </div>
        );
    }

    return (
        <div className="widget-card dashboard-widget-card">
            <div className="widget-header dashboard-widget-header">
                <div>
                    <h3 className="widget-title">최근 등록 자산</h3>
                    <p className="dashboard-widget-subtitle">최근 등록 흐름을 빠르게 확인할 수 있습니다.</p>
                </div>
                <Link to="/assets" className="widget-link">더보기</Link>
            </div>
            <table className="recent-assets-table">
                <thead>
                    <tr>
                        <th>자산명</th>
                        <th>상태</th>
                        <th>등록일</th>
                        <th>실사용자</th>
                    </tr>
                </thead>
                <tbody>
                    {assets.length === 0 ? (
                        <tr>
                            <td colSpan="4" className="dashboard-table-empty">
                                등록된 자산이 없습니다.
                            </td>
                        </tr>
                    ) : (
                        assets.map(asset => (
                            <tr key={asset.id}>
                                <td className="dashboard-asset-name-cell">{asset.name}</td>
                                <td>
                                    <span className={`status-badge ${getStatusClass(asset.status)}`}>
                                        {getStatusLabel(asset.status)}
                                    </span>
                                </td>
                                <td>{asset.createdAt ? asset.createdAt.split('T')[0] : '-'}</td>
                                <td>{asset.actualUserName || '-'}</td>
                            </tr>
                        ))
                    )}
                </tbody>
            </table>
        </div>
    );
};

export default RecentAssets;

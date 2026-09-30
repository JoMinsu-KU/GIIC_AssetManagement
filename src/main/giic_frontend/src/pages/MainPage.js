import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import DashboardLayout from '../components/layout/DashboardLayout';
import RecentAssets from '../components/dashboard/RecentAssets';
import AssetStatusChart from '../components/dashboard/AssetStatusChart';
import { getAssets } from '../api/assets';
import '../styles/Dashboard.css';

const MainPage = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [stats, setStats] = useState({
        total: 0,
        normal: 0,
        abnormal: 0,
        unused: 0,
        repairing: 0,
        disposed: 0,
        transferPending: 0,
        disposalPending: 0,
        disposalRejected: 0,
        lossPending: 0,
        lost: 0
    });
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchStats = async () => {
            try {
                const response = await getAssets({ size: 2000 });
                const assets = response.content || [];

                const total = assets.length;
                const normal = assets.filter(a => a.status === 'NORMAL').length;
                const abnormal = assets.filter(a => a.status === 'ABNORMAL').length;
                const unused = assets.filter(a => a.status === 'UNUSED').length;
                const repairing = assets.filter(a => a.status === 'REPAIRING').length;
                const disposed = assets.filter(a => a.status === 'DISPOSED').length;
                const transferPending = assets.filter(a => a.status === 'TRANSFER_PENDING').length;
                const disposalPending = assets.filter(a => a.status === 'DISPOSAL_PENDING').length;
                const disposalRejected = assets.filter(a => a.status === 'DISPOSAL_REJECTED').length;
                const lossPending = assets.filter(a => a.status === 'LOSS_PENDING').length;
                const lost = assets.filter(a => a.status === 'LOST').length;

                setStats({
                    total,
                    normal,
                    abnormal,
                    unused,
                    repairing,
                    disposed,
                    transferPending,
                    disposalPending,
                    disposalRejected,
                    lossPending,
                    lost
                });
            } catch (error) {
                console.error('Failed to fetch dashboard stats', error);
            } finally {
                setLoading(false);
            }
        };

        fetchStats();
    }, []);

    const healthyRate = stats.total > 0 ? Math.round((stats.normal / stats.total) * 100) : 0;
    const pendingCount = stats.transferPending + stats.disposalPending + stats.lossPending;
    const inProgressCount = stats.repairing + pendingCount;
    const attentionCount = stats.abnormal + stats.disposalRejected;
    const closedCount = stats.disposed + stats.lost;

    return (
        <DashboardLayout>
            <div className="dashboard-shell">
                <section className="dashboard-hero">
                    <div className="dashboard-hero-copy">
                        <div className="dashboard-overline">Overview</div>
                        <h1 className="dashboard-title">대시보드</h1>
                        <p className="dashboard-subtitle">
                            안녕하세요, <span>{user?.name || '관리자'}</span>님. 오늘 확인해야 할 자산 현황과 최근 흐름을 한 화면에서 볼 수 있도록 정리했습니다.
                        </p>
                    </div>

                    <div className="dashboard-hero-side">
                        <div className="dashboard-health-card">
                            <div className="dashboard-health-label">정상 운영 비율</div>
                            <div className="dashboard-health-value">{loading ? '-' : `${healthyRate}%`}</div>
                            <div className="dashboard-health-meta">
                                전체 {stats.total}건 중 정상/사용중 {stats.normal}건
                            </div>
                        </div>
                        <div className="dashboard-quick-actions">
                            <button className="dashboard-quick-btn dashboard-quick-btn-primary" onClick={() => navigate('/assets/new')}>
                                자산 등록
                            </button>
                            <button className="dashboard-quick-btn" onClick={() => navigate('/assets')}>
                                자산 목록 보기
                            </button>
                            <button className="dashboard-quick-btn" onClick={() => navigate('/notifications')}>
                                알림 확인
                            </button>
                        </div>
                    </div>
                </section>

                <section className="dashboard-summary-strip">
                    <div className="dashboard-summary-item dashboard-summary-total">
                        <span>전체 자산</span>
                        <strong>{loading ? '-' : stats.total}</strong>
                        <div className="dashboard-summary-detail dashboard-summary-breakdown">
                            {loading ? (
                                <span>집계 중</span>
                            ) : (
                                <>
                                    <span>운영 대상 {stats.total - closedCount}</span>
                                    <span>종료 {closedCount}</span>
                                </>
                            )}
                        </div>
                    </div>
                    <div className="dashboard-summary-item dashboard-summary-normal">
                        <span>정상 운영</span>
                        <strong>{loading ? '-' : stats.normal}</strong>
                        <div className="dashboard-summary-detail">
                            전체의 {loading ? '-' : `${healthyRate}%`}
                        </div>
                    </div>
                    <div className="dashboard-summary-item dashboard-summary-progress">
                        <span>업무 처리중</span>
                        <strong>{loading ? '-' : inProgressCount}</strong>
                        <div className="dashboard-summary-detail dashboard-summary-breakdown">
                            {loading ? (
                                <span>집계 중</span>
                            ) : (
                                <>
                                    <span>수리 {stats.repairing}</span>
                                    <span>이관 {stats.transferPending}</span>
                                    <span>폐기 {stats.disposalPending}</span>
                                    <span>손망 {stats.lossPending}</span>
                                </>
                            )}
                        </div>
                    </div>
                    <div className="dashboard-summary-item dashboard-summary-attention">
                        <span>확인 필요</span>
                        <strong>{loading ? '-' : attentionCount}</strong>
                        <div className="dashboard-summary-detail dashboard-summary-breakdown">
                            {loading ? (
                                <span>집계 중</span>
                            ) : (
                                <>
                                    <span>고장 {stats.abnormal}</span>
                                    <span>반려 {stats.disposalRejected}</span>
                                </>
                            )}
                        </div>
                    </div>
                </section>

                <div className="dashboard-widgets dashboard-widget-grid">
                    <RecentAssets />
                    <AssetStatusChart stats={stats} loading={loading} />
                </div>
            </div>
        </DashboardLayout>
    );
};

export default MainPage;

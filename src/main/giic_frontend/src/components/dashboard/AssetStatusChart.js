import React from 'react';
import { Link } from 'react-router-dom';
import '../../styles/Dashboard.css';

const AssetStatusChart = ({ stats, loading }) => {
    if (loading || !stats) {
        return (
            <div className="widget-card dashboard-widget-card">
                <div className="widget-header dashboard-widget-header">
                    <div>
                        <h3 className="widget-title">자산 현황</h3>
                        <p className="dashboard-widget-subtitle">상태 비율을 계산하는 중입니다.</p>
                    </div>
                </div>
                <div className="chart-container dashboard-chart-loading">불러오는 중...</div>
            </div>
        );
    }

    const {
        total,
        normal,
        unused,
        repairing,
        disposed,
        abnormal,
        transferPending,
        disposalPending,
        disposalRejected,
        lossPending,
        lost
    } = stats;

    if (total === 0) {
        return (
            <div className="widget-card dashboard-widget-card">
                <div className="widget-header dashboard-widget-header">
                    <div>
                        <h3 className="widget-title">자산 현황</h3>
                        <p className="dashboard-widget-subtitle">현재 등록된 자산 데이터가 없습니다.</p>
                    </div>
                    <Link to="/assets" className="widget-link">상세보기</Link>
                </div>
                <div className="chart-container dashboard-chart-loading">데이터 없음</div>
            </div>
        );
    }

    const inProgress = repairing + transferPending + disposalPending + lossPending;
    const attention = abnormal + disposalRejected;
    const closed = disposed + lost;

    const normalPct = (normal / total) * 100;
    const unusedPct = (unused / total) * 100;
    const inProgressPct = (inProgress / total) * 100;
    const attentionPct = (attention / total) * 100;
    const closedPct = (closed / total) * 100;

    const normalStop = normalPct;
    const unusedStop = normalStop + unusedPct;
    const progressStop = unusedStop + inProgressPct;
    const attentionStop = progressStop + attentionPct;
    const closedStop = attentionStop + closedPct;

    const gradient = `conic-gradient(
        #10B981 0% ${normalStop}%,
        #94A3B8 ${normalStop}% ${unusedStop}%,
        #3B82F6 ${unusedStop}% ${progressStop}%,
        #E28A22 ${progressStop}% ${attentionStop}%,
        #64748B ${attentionStop}% ${closedStop}%,
        #E2E8F0 ${closedStop}% 100%
    )`;

    return (
        <div className="widget-card dashboard-widget-card">
            <div className="widget-header dashboard-widget-header">
                <div>
                    <h3 className="widget-title">운영 상태</h3>
                    <p className="dashboard-widget-subtitle">사용중과 미사용을 구분해 자산의 현재 상태를 보여줍니다.</p>
                </div>
                <Link to="/assets" className="widget-link">상세보기</Link>
            </div>

            <div className="chart-container dashboard-chart-container">
                <div className="donut-chart" style={{ background: gradient }}>
                    <div className="donut-center">
                        <span className="dashboard-donut-total">{total}</span>
                        <span className="dashboard-donut-label">전체 자산</span>
                    </div>
                </div>

                <div className="chart-legend dashboard-chart-legend">
                    <div className="legend-item">
                        <div className="legend-color" style={{ backgroundColor: '#10B981' }}></div>
                        <span>정상 운영 {normal}건 ({Math.round(normalPct)}%)</span>
                    </div>
                    <div className="legend-item">
                        <div className="legend-color" style={{ backgroundColor: '#94A3B8' }}></div>
                        <span>미사용 {unused}건 ({Math.round(unusedPct)}%)</span>
                    </div>
                    <div className="legend-item">
                        <div className="legend-color" style={{ backgroundColor: '#3B82F6' }}></div>
                        <span>업무 처리중 {inProgress}건 ({Math.round(inProgressPct)}%)</span>
                    </div>
                    <div className="legend-item">
                        <div className="legend-color" style={{ backgroundColor: '#E28A22' }}></div>
                        <span>확인 필요 {attention}건 ({Math.round(attentionPct)}%)</span>
                    </div>
                    <div className="legend-item">
                        <div className="legend-color" style={{ backgroundColor: '#64748B' }}></div>
                        <span>종료 {closed}건 ({Math.round(closedPct)}%)</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AssetStatusChart;

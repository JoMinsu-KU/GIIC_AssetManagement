import React from 'react';
import '../../styles/Dashboard.css';

const StatCard = ({ title, value, icon, color, bgColor, loading }) => {
    return (
        <div className="stat-card dashboard-stat-card">
            <div className="stat-icon-wrapper" style={{ backgroundColor: bgColor || `${color}20`, color: color }}>
                {icon}
            </div>
            <div className="stat-content">
                <div className="dashboard-stat-label-top">실시간 집계</div>
                <div className="stat-value" style={{ color: color }}>
                    {loading ? (
                        <span className="loading-spinner-sm">...</span>
                    ) : (
                        value
                    )}
                </div>
                <div className="stat-label">{title}</div>
            </div>
        </div>
    );
};

export default StatCard;

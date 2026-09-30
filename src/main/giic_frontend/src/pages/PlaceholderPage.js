import React from 'react';
import DashboardLayout from '../components/layout/DashboardLayout';

const PlaceholderPage = ({ title }) => {
    return (
        <DashboardLayout>
            <div style={{ padding: '20px' }}>
                <h1 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '20px' }}>{title}</h1>
                <div style={{ padding: '40px', backgroundColor: 'white', borderRadius: '8px', textAlign: 'center', color: '#64748b' }}>
                    <p>페이지 준비 중입니다.</p>
                </div>
            </div>
        </DashboardLayout>
    );
};

export default PlaceholderPage;

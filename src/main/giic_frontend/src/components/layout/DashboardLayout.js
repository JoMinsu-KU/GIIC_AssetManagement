import React from 'react';
import Header from './Header';
import '../../styles/Dashboard.css';

const DashboardLayout = ({ children }) => {
    return (
        <div className="dashboard-layout">
            <Header />
            <main className="main-content">
                {children}
            </main>
        </div>
    );
};

export default DashboardLayout;

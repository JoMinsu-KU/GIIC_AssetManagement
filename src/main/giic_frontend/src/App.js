import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import MainPage from './pages/MainPage';
import PlaceholderPage from './pages/PlaceholderPage';
import UserListPage from './pages/UserListPage';
import AssetListPage from './pages/AssetListPage';
import AssetCreatePage from './pages/AssetCreatePage';
import AssetDetailPage from './pages/AssetDetailPage';
import TransferListPage from './pages/TransferListPage';
import MaintenanceListPage from './pages/MaintenanceListPage';
import DisposalListPage from './pages/DisposalListPage';
import LossListPage from './pages/LossListPage';
import NotificationListPage from './pages/NotificationListPage';
import ProtectedRoute from './components/ProtectedRoute';
import './App.css';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          <Route path="/main" element={
            <ProtectedRoute>
              <MainPage />
            </ProtectedRoute>
          } />

          <Route path="/assets" element={
            <ProtectedRoute>
              <AssetListPage />
            </ProtectedRoute>
          } />

          <Route path="/assets/new" element={
            <ProtectedRoute>
              <AssetCreatePage />
            </ProtectedRoute>
          } />

          <Route path="/assets/:id" element={
            <ProtectedRoute>
              <AssetDetailPage />
            </ProtectedRoute>
          } />

          <Route path="/transfers" element={
            <ProtectedRoute>
              <TransferListPage />
            </ProtectedRoute>
          } />

          <Route path="/disposals" element={
            <ProtectedRoute>
              <DisposalListPage />
            </ProtectedRoute>
          } />

          <Route path="/losses" element={
            <ProtectedRoute>
              <LossListPage />
            </ProtectedRoute>
          } />

          <Route path="/main/maintenance-list" element={
            <ProtectedRoute>
              <MaintenanceListPage />
            </ProtectedRoute>
          } />

          <Route path="/users" element={
            <ProtectedRoute>
              <UserListPage />
            </ProtectedRoute>
          } />

          <Route path="/settings" element={
            <ProtectedRoute>
              <PlaceholderPage title="시스템 설정" />
            </ProtectedRoute>
          } />

          <Route path="/notifications" element={
            <ProtectedRoute>
              <NotificationListPage />
            </ProtectedRoute>
          } />

          <Route path="/" element={<Navigate to="/login" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
}

export default App;

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import DashboardLayout from '../components/layout/DashboardLayout';
import {
    createCategory, getAccessDashboard, getAdminCategories, renameCategory, setCategoryActive
} from '../api/settings';
import '../styles/SystemSettings.css';

const roleLabels = { USER: '사용자', ADMIN: '관리자', SUPER: '슈퍼관리자' };
const statusLabels = { APPROVED: '승인', PENDING: '대기', REJECTED: '거절', INACTIVE: '비활성' };
const formatDate = (value) => value ? new Date(value).toLocaleString('ko-KR') : '-';
const errorText = (error) => error?.response?.data?.error?.message
    || error?.response?.data?.message || '요청을 처리하지 못했습니다.';

const SystemSettingsPage = () => {
    const [tab, setTab] = useState('access');
    const [dashboard, setDashboard] = useState(null);
    const [categories, setCategories] = useState([]);
    const [loadingAccess, setLoadingAccess] = useState(true);
    const [loadingCategories, setLoadingCategories] = useState(true);
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState('');
    const [search, setSearch] = useState('');
    const [majorName, setMajorName] = useState('');
    const [minorName, setMinorName] = useState('');
    const [parentId, setParentId] = useState('');
    const [editingId, setEditingId] = useState(null);
    const [editName, setEditName] = useState('');

    const refreshAccess = useCallback(async () => {
        try {
            setDashboard(await getAccessDashboard());
        } catch (error) {
            setMessage(errorText(error));
        } finally {
            setLoadingAccess(false);
        }
    }, []);

    const refreshCategories = useCallback(async () => {
        try {
            setCategories(await getAdminCategories());
        } catch (error) {
            setMessage(errorText(error));
        } finally {
            setLoadingCategories(false);
        }
    }, []);

    useEffect(() => {
        refreshAccess();
        refreshCategories();
    }, [refreshAccess, refreshCategories]);

    useEffect(() => {
        if (tab !== 'access') return undefined;
        const timer = setInterval(refreshAccess, 30000);
        return () => clearInterval(timer);
    }, [tab, refreshAccess]);

    const majors = useMemo(() => categories.filter((category) => category.parentId == null), [categories]);
    const users = useMemo(() => {
        const query = search.trim().toLowerCase();
        if (!query) return dashboard?.users || [];
        return (dashboard?.users || []).filter((user) => [
            user.name, user.employeeNo, user.department, user.lastActivityIp, user.lastLoginIp
        ].some((value) => String(value || '').toLowerCase().includes(query)));
    }, [dashboard, search]);

    const runCategoryAction = async (action, successMessage) => {
        setBusy(true);
        setMessage('');
        try {
            await action();
            await refreshCategories();
            setMessage(successMessage);
            return true;
        } catch (error) {
            setMessage(errorText(error));
            return false;
        } finally {
            setBusy(false);
        }
    };

    const handleCreateMajor = async (event) => {
        event.preventDefault();
        if (await runCategoryAction(() => createCategory({ name: majorName.trim(), parentId: null }),
            '대분류를 추가했습니다.')) setMajorName('');
    };

    const handleCreateMinor = async (event) => {
        event.preventDefault();
        if (await runCategoryAction(() => createCategory({ name: minorName.trim(), parentId: Number(parentId) }),
            '중분류를 추가했습니다.')) setMinorName('');
    };

    const handleRename = async (category) => {
        if (await runCategoryAction(() => renameCategory(category.id, editName.trim()),
            '분류 이름을 변경했습니다.')) setEditingId(null);
    };

    const handleToggleActive = async (category) => {
        const nextActive = !category.active;
        if (!nextActive && !window.confirm(
            '이 분류를 사용 중지할까요? 기존 자산의 분류 기록은 유지되며 새 등록에는 선택할 수 없습니다.'
        )) return;
        await runCategoryAction(() => setCategoryActive(category.id, nextActive),
            nextActive ? '분류를 다시 활성화했습니다.' : '분류를 사용 중지했습니다.');
    };

    const categoryRow = (category) => (
        <div className={`settings-category-row ${category.active ? '' : 'is-inactive'}`} key={category.id}>
            {editingId === category.id ? (
                <form className="settings-inline-edit" onSubmit={(event) => {
                    event.preventDefault();
                    handleRename(category);
                }}>
                    <input value={editName} onChange={(event) => setEditName(event.target.value)}
                        maxLength={100} required aria-label="분류 이름" />
                    <button type="submit" disabled={busy}>저장</button>
                    <button type="button" onClick={() => setEditingId(null)}>취소</button>
                </form>
            ) : (
                <>
                    <span className="settings-category-name">{category.name}</span>
                    <span className={`settings-category-state ${category.active ? 'active' : ''}`}>
                        {category.active ? '사용 중' : '사용 중지'}
                    </span>
                    <button type="button" onClick={() => { setEditingId(category.id); setEditName(category.name); }}>
                        이름 변경
                    </button>
                    <button type="button" onClick={() => handleToggleActive(category)} disabled={busy}>
                        {category.active ? '사용 중지' : '활성화'}
                    </button>
                </>
            )}
        </div>
    );

    return (
        <DashboardLayout>
            <div className="settings-page">
                <div className="settings-heading">
                    <div><p className="settings-eyebrow">시스템 관리</p><h1>시스템 설정</h1>
                        <p>접속 현황을 확인하고 자산 분류를 관리합니다.</p></div>
                </div>
                <div className="settings-tabs" role="tablist" aria-label="시스템 설정 메뉴">
                    <button type="button" className={tab === 'access' ? 'selected' : ''}
                        onClick={() => setTab('access')}>접속 현황</button>
                    <button type="button" className={tab === 'categories' ? 'selected' : ''}
                        onClick={() => setTab('categories')}>자산 분류 관리</button>
                </div>
                {message && <div className="settings-message" role="status">{message}</div>}

                {tab === 'access' && <>
                    <div className="settings-section-title"><h2>사용자 접속 현황</h2>
                        <button type="button" onClick={refreshAccess}>새로고침</button></div>
                    <p className="settings-note">최근 2분 이내 요청이 있으면 접속 중으로 표시합니다. 접속 기록은 이 기능이 적용된 이후부터 수집됩니다. 현재 Docker Desktop 경유 접속은 실제 사용자 IP가 전달되지 않아 IP가 비어 있을 수 있습니다.</p>
                    <div className="settings-stats">
                        <div><span>전체 사용자</span><strong>{dashboard?.totalUsers ?? '-'}</strong></div>
                        <div><span>최근 활동 사용자</span><strong>{dashboard?.activeUsers ?? '-'}</strong></div>
                        <div><span>최근 로그인 기록</span><strong>{dashboard?.recentLogins?.length ?? '-'}</strong></div>
                    </div>
                    <div className="settings-panel">
                        <div className="settings-section-title"><h3>사용자별 활동</h3>
                            <input type="search" value={search} onChange={(event) => setSearch(event.target.value)}
                                placeholder="이름, 사번, 부서, IP 검색" aria-label="사용자 검색" /></div>
                        {loadingAccess ? <p className="settings-empty">불러오는 중...</p> :
                            <div className="settings-table-wrap"><table className="settings-table"><thead><tr>
                                <th>사용자</th><th>권한 · 상태</th><th>접속</th><th>마지막 로그인</th>
                                <th>마지막 활동</th><th>접속 IP</th><th>로그인 횟수</th>
                            </tr></thead><tbody>{users.map((user) => <tr key={user.employeeNo}>
                                <td><strong>{user.name}</strong><small>{user.employeeNo} · {user.department || '-'}</small></td>
                                <td>{roleLabels[user.role] || user.role}<small>{statusLabels[user.status] || user.status}</small></td>
                                <td><span className={`settings-online ${user.online ? 'yes' : ''}`}>
                                    {user.online ? '최근 활동' : '오프라인'}</span></td>
                                <td>{formatDate(user.lastLoginAt)}</td><td>{formatDate(user.lastActivityAt)}</td>
                                <td>{user.lastActivityIp || user.lastLoginIp || '-'}</td><td>{user.loginCount}</td>
                            </tr>)}</tbody></table>{users.length === 0 && <p className="settings-empty">표시할 사용자가 없습니다.</p>}</div>}
                    </div>
                    <div className="settings-panel"><h3>최근 로그인</h3>
                        <div className="settings-table-wrap"><table className="settings-table"><thead><tr>
                            <th>사용자</th><th>로그인 시각</th><th>접속 IP</th>
                        </tr></thead><tbody>{(dashboard?.recentLogins || []).map((event, index) => <tr key={`${event.employeeNo}-${event.loggedInAt}-${index}`}>
                            <td>{event.name}<small>{event.employeeNo}</small></td>
                            <td>{formatDate(event.loggedInAt)}</td><td>{event.ipAddress || '-'}</td>
                        </tr>)}</tbody></table>{!dashboard?.recentLogins?.length && <p className="settings-empty">아직 수집된 로그인 기록이 없습니다.</p>}</div>
                    </div>
                </>}

                {tab === 'categories' && <>
                    <div className="settings-section-title"><h2>자산 분류 관리</h2>
                        <button type="button" onClick={refreshCategories}>새로고침</button></div>
                    <p className="settings-note">대분류 아래 중분류를 추가합니다. 사용 중지한 분류는 신규 자산 등록 선택지에서 제외되고 기존 자산 기록은 유지됩니다.</p>
                    <div className="settings-create-grid">
                        <form className="settings-panel" onSubmit={handleCreateMajor}>
                            <h3>대분류 추가</h3><label>대분류 이름<input value={majorName}
                                onChange={(event) => setMajorName(event.target.value)} maxLength={100}
                                placeholder="예: 연구 장비" required /></label>
                            <button type="submit" disabled={busy}>대분류 추가</button>
                        </form>
                        <form className="settings-panel" onSubmit={handleCreateMinor}>
                            <h3>중분류 추가</h3><label>상위 대분류<select value={parentId}
                                onChange={(event) => setParentId(event.target.value)} required>
                                <option value="">대분류 선택</option>
                                {majors.filter((major) => major.active).map((major) =>
                                    <option key={major.id} value={major.id}>{major.name}</option>)}</select></label>
                            <label>중분류 이름<input value={minorName}
                                onChange={(event) => setMinorName(event.target.value)} maxLength={100}
                                placeholder="예: 계측 장비" required /></label>
                            <button type="submit" disabled={busy}>중분류 추가</button>
                        </form>
                    </div>
                    {loadingCategories ? <p className="settings-empty">불러오는 중...</p> :
                        <div className="settings-category-list">{majors.map((major) =>
                            <div className="settings-panel" key={major.id}>
                                <div className="settings-major-row">{categoryRow(major)}</div>
                                <div className="settings-minor-list">{categories.filter((item) => item.parentId === major.id)
                                    .map(categoryRow)}</div>
                            </div>)}</div>}
                </>}
            </div>
        </DashboardLayout>
    );
};

export default SystemSettingsPage;

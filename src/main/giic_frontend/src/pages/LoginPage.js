import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import '../styles/Auth.css';

const LoginPage = () => {
    const [employeeNo, setEmployeeNo] = useState('');
    const [password, setPassword] = useState('');
    const [saveId, setSaveId] = useState(false);
    const [error, setError] = useState('');
    const { login } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        const savedId = localStorage.getItem('savedEmployeeNo');
        if (savedId) {
            setEmployeeNo(savedId);
            setSaveId(true);
        }
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            await login(employeeNo, password);

            if (saveId) {
                localStorage.setItem('savedEmployeeNo', employeeNo);
            } else {
                localStorage.removeItem('savedEmployeeNo');
            }

            navigate('/main');
        } catch (err) {
            console.error(err);
            setError('로그인에 실패했습니다. 사번과 비밀번호를 확인해주세요.');
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-header">
                    <img src="/logo.jpg" alt="Logo" className="auth-logo" />
                    <h2>자산 관리 시스템</h2>
                </div>
                <form onSubmit={handleSubmit} className="auth-form">
                    <div className="form-group">
                        <input
                            type="text"
                            placeholder="사번"
                            value={employeeNo}
                            onChange={(e) => setEmployeeNo(e.target.value)}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <input
                            type="password"
                            placeholder="비밀번호"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>
                    <div className="form-options">
                        <label>
                            <input
                                type="checkbox"
                                checked={saveId}
                                onChange={(e) => setSaveId(e.target.checked)}
                            />
                            아이디 저장
                        </label>
                    </div>
                    {error && <div className="error-message">{error}</div>}
                    <button type="submit" className="auth-button">로그인</button>
                </form>
                <div className="auth-footer">
                    <Link to="/signup">회원가입</Link>
                </div>
                <div className="auth-notes">
                    <p>문의사항이 있으시면 관리자에게 연락바랍니다.</p>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;

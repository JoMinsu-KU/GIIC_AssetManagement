import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
    normalizeSignupForm,
    SIGNUP_FIELD_NAMES,
    validateSignupForm
} from '../utils/signupValidation';
import '../styles/Auth.css';

const SignupPage = () => {
    const navigate = useNavigate();
    const { signup } = useAuth();

    const [formData, setFormData] = useState({
        employeeNo: '',
        password: '',
        confirmPassword: '',
        name: '',
        department: '',
        email: ''
    });
    const [error, setError] = useState('');
    const [fieldErrors, setFieldErrors] = useState({});
    const [touched, setTouched] = useState({});

    const handleChange = (e) => {
        const { name, value } = e.target;
        const nextFormData = { ...formData, [name]: value };
        setFormData(nextFormData);
        setError('');

        if (touched[name] || fieldErrors[name]) {
            setFieldErrors(validateSignupForm(nextFormData));
        }
    };

    const handleBlur = (e) => {
        const { name } = e.target;
        setTouched(prev => ({ ...prev, [name]: true }));
        setFieldErrors(validateSignupForm(formData));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        const normalizedFormData = normalizeSignupForm(formData);
        const validationErrors = validateSignupForm(normalizedFormData);
        if (Object.keys(validationErrors).length > 0) {
            setFormData(normalizedFormData);
            setFieldErrors(validationErrors);
            setTouched(Object.fromEntries(SIGNUP_FIELD_NAMES.map(field => [field, true])));

            const firstInvalidField = SIGNUP_FIELD_NAMES.find(field => validationErrors[field]);
            requestAnimationFrame(() => {
                document.querySelector(`[name="${firstInvalidField}"]`)?.focus();
            });
            return;
        }

        try {
            const { confirmPassword, ...signupData } = normalizedFormData;
            await signup(signupData);
            alert('회원가입 신청이 완료되었습니다. 관리자 승인 후 로그인이 가능합니다.');
            navigate('/login');
        } catch (err) {
            console.error(err);
            const serverMessage = err.response?.data?.error?.message;
            setError(serverMessage || '회원가입에 실패했습니다. 이미 존재하는 사번이거나 서버 오류일 수 있습니다.');
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-header">
                    <img src="/logo.jpg" alt="Logo" className="auth-logo" />

                    <h2>회원가입</h2>
                </div>
                <form onSubmit={handleSubmit} className="auth-form" noValidate>
                    <div className={`form-group${touched.employeeNo && fieldErrors.employeeNo ? ' has-error' : ''}`}>
                        <input
                            type="text"
                            name="employeeNo"
                            placeholder="사번 7~8자리 (필수)"
                            value={formData.employeeNo}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            inputMode="numeric"
                            autoComplete="username"
                            minLength={7}
                            maxLength={8}
                            pattern="[0-9]{7,8}"
                            aria-invalid={Boolean(touched.employeeNo && fieldErrors.employeeNo)}
                            aria-describedby="employeeNo-error"
                            required
                        />
                        {touched.employeeNo && fieldErrors.employeeNo && (
                            <span id="employeeNo-error" className="field-error-message">{fieldErrors.employeeNo}</span>
                        )}
                    </div>
                    <div className={`form-group${touched.password && fieldErrors.password ? ' has-error' : ''}`}>
                        <input
                            type="password"
                            name="password"
                            placeholder="비밀번호 (필수)"
                            value={formData.password}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            autoComplete="new-password"
                            minLength={8}
                            maxLength={64}
                            aria-invalid={Boolean(touched.password && fieldErrors.password)}
                            aria-describedby="password-help password-error"
                            required
                        />
                        <span id="password-help" className="field-help-text">영문과 숫자를 포함한 8~64자</span>
                        {touched.password && fieldErrors.password && (
                            <span id="password-error" className="field-error-message">{fieldErrors.password}</span>
                        )}
                    </div>
                    <div className={`form-group${touched.confirmPassword && fieldErrors.confirmPassword ? ' has-error' : ''}`}>
                        <input
                            type="password"
                            name="confirmPassword"
                            placeholder="비밀번호 확인 (필수)"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            autoComplete="new-password"
                            minLength={8}
                            maxLength={64}
                            aria-invalid={Boolean(touched.confirmPassword && fieldErrors.confirmPassword)}
                            aria-describedby="confirmPassword-error"
                            required
                        />
                        {touched.confirmPassword && fieldErrors.confirmPassword && (
                            <span id="confirmPassword-error" className="field-error-message">{fieldErrors.confirmPassword}</span>
                        )}
                    </div>
                    <div className={`form-group${touched.name && fieldErrors.name ? ' has-error' : ''}`}>
                        <input
                            type="text"
                            name="name"
                            placeholder="이름 (필수)"
                            value={formData.name}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            autoComplete="name"
                            minLength={2}
                            maxLength={50}
                            aria-invalid={Boolean(touched.name && fieldErrors.name)}
                            aria-describedby="name-error"
                            required
                        />
                        {touched.name && fieldErrors.name && (
                            <span id="name-error" className="field-error-message">{fieldErrors.name}</span>
                        )}
                    </div>
                    <div className={`form-group${touched.department && fieldErrors.department ? ' has-error' : ''}`}>
                        <input
                            type="text"
                            name="department"
                            placeholder="부서 (선택)"
                            value={formData.department}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            autoComplete="organization"
                            maxLength={50}
                            aria-invalid={Boolean(touched.department && fieldErrors.department)}
                            aria-describedby="department-error"
                        />
                        {touched.department && fieldErrors.department && (
                            <span id="department-error" className="field-error-message">{fieldErrors.department}</span>
                        )}
                    </div>
                    <div className={`form-group${touched.email && fieldErrors.email ? ' has-error' : ''}`}>
                        <input
                            id="signup-email"
                            type="email"
                            name="email"
                            placeholder="이메일 (필수)"
                            value={formData.email}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            autoComplete="email"
                            maxLength={100}
                            aria-label="이메일 (필수)"
                            aria-invalid={Boolean(touched.email && fieldErrors.email)}
                            aria-describedby="email-error"
                            required
                        />
                        {touched.email && fieldErrors.email && (
                            <span id="email-error" className="field-error-message">{fieldErrors.email}</span>
                        )}
                    </div>
                    {error && <div className="error-message">{error}</div>}
                    <button type="submit" className="auth-button">회원가입</button>
                </form>
                <div className="auth-footer">
                    <Link to="/login">로그인으로 돌아가기</Link>
                </div>
                <div className="auth-notes">
                    <p>문의사항이 있으시면 관리자에게 연락바랍니다.</p>
                </div>
            </div>
        </div>
    );
};

export default SignupPage;

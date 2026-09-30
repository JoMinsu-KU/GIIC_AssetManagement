export const SIGNUP_FIELD_NAMES = [
    'employeeNo',
    'password',
    'confirmPassword',
    'name',
    'department',
    'email'
];

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const NAME_PATTERN = /^[가-힣A-Za-z][가-힣A-Za-z .·'-]*$/;
const DEPARTMENT_PATTERN = /^(?=.*[가-힣A-Za-z0-9])[가-힣A-Za-z0-9 ()（）·._&/,-]+$/;

export const normalizeSignupForm = (formData) => ({
    ...formData,
    employeeNo: formData.employeeNo.trim(),
    name: formData.name.trim(),
    department: formData.department.trim(),
    email: formData.email.trim()
});

export const validateSignupForm = (formData) => {
    const errors = {};
    const employeeNo = formData.employeeNo.trim();
    const name = formData.name.trim();
    const department = formData.department.trim();
    const email = formData.email.trim();

    if (!employeeNo) {
        errors.employeeNo = '사번을 입력해 주세요.';
    } else if (!/^\d{7,8}$/.test(employeeNo)) {
        errors.employeeNo = '사번은 숫자 7~8자리로 입력해 주세요.';
    }

    if (!formData.password) {
        errors.password = '비밀번호를 입력해 주세요.';
    } else if (formData.password.length < 8 || formData.password.length > 64) {
        errors.password = '비밀번호는 8~64자로 입력해 주세요.';
    } else if (/\s/.test(formData.password)) {
        errors.password = '비밀번호에는 공백을 사용할 수 없습니다.';
    } else if (!/[A-Za-z]/.test(formData.password) || !/\d/.test(formData.password)) {
        errors.password = '비밀번호에는 영문과 숫자가 각각 하나 이상 필요합니다.';
    }

    if (!formData.confirmPassword) {
        errors.confirmPassword = '비밀번호 확인을 입력해 주세요.';
    } else if (formData.password !== formData.confirmPassword) {
        errors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    }

    if (!name) {
        errors.name = '이름을 입력해 주세요.';
    } else if (name.length < 2 || name.length > 50) {
        errors.name = '이름은 2~50자로 입력해 주세요.';
    } else if (!NAME_PATTERN.test(name)) {
        errors.name = '이름에는 한글, 영문, 공백, 점, 하이픈만 사용할 수 있습니다.';
    }

    if (department.length > 50) {
        errors.department = '부서는 50자 이하로 입력해 주세요.';
    } else if (department && !DEPARTMENT_PATTERN.test(department)) {
        errors.department = '올바른 부서명을 입력해 주세요.';
    }

    if (!email) {
        errors.email = '이메일을 입력해 주세요.';
    } else if (email.length > 100) {
        errors.email = '이메일은 100자 이하로 입력해 주세요.';
    } else if (!EMAIL_PATTERN.test(email)) {
        errors.email = '올바른 이메일 주소를 입력해 주세요.';
    }

    return errors;
};

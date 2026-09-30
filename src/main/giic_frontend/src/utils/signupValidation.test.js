import { normalizeSignupForm, validateSignupForm } from './signupValidation';

const validForm = (overrides = {}) => ({
    employeeNo: '20193200',
    password: 'asset1234',
    confirmPassword: 'asset1234',
    name: '조민수',
    department: '경남지능화혁신사업단',
    email: 'user@example.com',
    ...overrides
});

describe('validateSignupForm', () => {
    test('accepts a valid signup form', () => {
        expect(validateSignupForm(validForm())).toEqual({});
    });

    test.each([
        ['employeeNo', { employeeNo: 'ABC1234' }],
        ['password', { password: 'onlyletters', confirmPassword: 'onlyletters' }],
        ['confirmPassword', { confirmPassword: 'different1' }],
        ['name', { name: '조민수123' }],
        ['department', { department: '가'.repeat(51) }],
        ['department', { department: '.' }],
        ['email', { email: 'invalid-email' }]
    ])('rejects an invalid %s', (field, override) => {
        expect(validateSignupForm(validForm(override))).toHaveProperty(field);
    });

    test('allows an empty optional department', () => {
        expect(validateSignupForm(validForm({ department: '' }))).not.toHaveProperty('department');
    });
});

describe('normalizeSignupForm', () => {
    test('trims text fields without changing passwords', () => {
        const normalized = normalizeSignupForm(validForm({
            employeeNo: ' 20193200 ',
            name: ' 조민수 ',
            department: ' 사업단 ',
            email: ' user@example.com ',
            password: ' asset1234 ',
            confirmPassword: ' asset1234 '
        }));

        expect(normalized).toMatchObject({
            employeeNo: '20193200',
            name: '조민수',
            department: '사업단',
            email: 'user@example.com',
            password: ' asset1234 ',
            confirmPassword: ' asset1234 '
        });
    });
});

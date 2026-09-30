// 현재 호스트를 기반으로 API URL 동적 생성
const getApiBaseUrl = () => {
    // 환경 변수가 있으면 사용
    if (process.env.REACT_APP_API_BASE_URL) {
        console.log('Using env API URL:', process.env.REACT_APP_API_BASE_URL);
        return process.env.REACT_APP_API_BASE_URL;
    }

    // 현재 접속한 호스트의 9090 포트 사용
    const host = window.location.hostname;
    const apiUrl = `http://${host}:9090/api`;
    console.log('Using dynamic API URL:', apiUrl);
    return apiUrl;
};

export const API_BASE_URL = getApiBaseUrl();
console.log('Final API_BASE_URL:', API_BASE_URL);

package org.example.giic_asset.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "E400", "잘못된 요청 파라미터입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "E401", "인증에 실패하였습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "E403", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "E404", "리소스를 찾을 수 없습니다."),
    DUPLICATE(HttpStatus.CONFLICT, "E409", "이미 존재하는 데이터입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500", "서버 내부 오류가 발생했습니다."),

    // OCR 관련 에러
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "E400_IMG", "지원하지 않는 이미지 형식입니다."),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "E400_SIZE", "이미지 크기가 너무 큽니다. (최대 10MB)"),
    EXTERNAL_API_ERROR(HttpStatus.SERVICE_UNAVAILABLE, "E503", "외부 서비스에 일시적인 문제가 발생했습니다."),
    OCR_PARSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E500_OCR", "OCR 결과 처리 중 오류가 발생했습니다."),
    API_KEY_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "O005", "API 키가 설정되지 않았습니다"),

    // 요청 관련 에러
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "E400_REQ", "진행중인 요청이 있어 신규 신청이 불가합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

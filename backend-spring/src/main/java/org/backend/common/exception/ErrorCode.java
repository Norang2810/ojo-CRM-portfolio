package org.backend.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 고객
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    MEMBERS_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원이 포함되어 있습니다."),

    // 구독
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "활성화된 구독 정보가 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),

    // 청구서
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 월의 청구 정보가 존재하지 않습니다."),

    // rfm 관련
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "분석 데이터를 찾을 수 없습니다."),
    KPI_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 월의 KPI 데이터가 존재하지 않습니다."),

    // 인증/인가
    ADMIN_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 이메일입니다."),
    INACTIVE_ADMIN(HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),
    GOOGLE_LOGIN_REQUIRED(HttpStatus.BAD_REQUEST, "구글 로그인 계정입니다. 구글 로그인을 이용하세요."),
    PASSWORD_NOT_SET(HttpStatus.BAD_REQUEST, "비밀번호가 설정되지 않은 계정입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었거나 로그아웃 상태입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 일치하지 않습니다. 다시 로그인하세요."),
    ADMIN_NOT_FOUND_FOR_ME(HttpStatus.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다. 로그인이 필요합니다."),

    // 채널
    CHANNEL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 채널입니다."),
    CHANNEL_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "채널에 해당 고객이 존재하지 않습니다."),
    CHANNEL_MEMBERS_NOT_FOUND(HttpStatus.NOT_FOUND, "채널에 존재하지 않는 고객이 포함되어 있습니다."),

    // 배치
    BATCH_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배치 작업 실행 중 오류가 발생했습니다."),
    DATA_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 삭제 작업에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
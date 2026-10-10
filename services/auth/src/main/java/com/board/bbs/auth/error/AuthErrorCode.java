package com.board.bbs.auth.error;

import org.springframework.http.HttpStatus;

/** 인증 서비스가 쓰는 오류 코드. 프로젝트 SRS §5.1의 공통 코드와 이름·상태·메시지가 같다. */
public enum AuthErrorCode {
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다.");

  private final HttpStatus status;
  private final String defaultMessage;

  AuthErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }
}

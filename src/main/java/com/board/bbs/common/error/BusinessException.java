package com.board.bbs.common.error;

import lombok.Getter;

/** 예상된 실패를 나타내는 예외. 스택트레이스는 의미가 없으므로 수집하지 않는다. */
@Getter
public class BusinessException extends RuntimeException {

  private final transient ErrorCode errorCode;

  /**
   * 에러 코드의 기본 메시지로 예외를 만든다.
   *
   * @param errorCode 에러 코드
   */
  public BusinessException(ErrorCode errorCode) {
    this(errorCode, errorCode.getDefaultMessage());
  }

  /**
   * 상황에 맞는 메시지로 예외를 만든다.
   *
   * @param errorCode 에러 코드
   * @param message 응답에 담을 메시지
   */
  public BusinessException(ErrorCode errorCode, String message) {
    super(message, null, false, false);
    this.errorCode = errorCode;
  }
}

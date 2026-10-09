package com.board.bbs.auth.forward;

/** 진입점이 전달한 원래 요청을 하나의 뜻으로 해석할 수 없다. */
public class AmbiguousForwardedRequestException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  AmbiguousForwardedRequestException(String message) {
    super(message);
  }
}

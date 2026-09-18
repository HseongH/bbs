package com.board.bbs.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 애플리케이션이 반환하는 모든 에러의 단일 목록. 예외를 던지는 쪽은 HTTP를 알 필요가 없다. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  ACCESS_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
  POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
  COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글에는 답글을 달 수 없습니다."),
  ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요한 게시글입니다."),
  NOT_LIKED(HttpStatus.CONFLICT, "좋아요하지 않은 게시글입니다."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String defaultMessage;
}

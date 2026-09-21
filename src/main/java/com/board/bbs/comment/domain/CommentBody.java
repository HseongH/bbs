package com.board.bbs.comment.domain;

/**
 * 댓글 본문.
 *
 * @param value 앞뒤 공백이 제거된 본문
 */
public record CommentBody(String value) {

  private static final int MAX_LENGTH = 1_000;

  /** 생성 시점에 불변식을 보장한다. */
  public CommentBody {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("댓글 본문은 비어 있을 수 없습니다.");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("댓글 본문은 " + MAX_LENGTH + "자를 넘을 수 없습니다.");
    }
  }
}

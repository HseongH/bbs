package com.board.bbs.post.domain;

/**
 * 게시글 본문.
 *
 * @param value 본문 내용
 */
public record Content(String value) {

  private static final int MAX_LENGTH = 10_000;

  /** 생성 시점에 불변식을 보장한다. */
  public Content {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("본문은 비어 있을 수 없습니다.");
    }
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("본문은 " + MAX_LENGTH + "자를 넘을 수 없습니다.");
    }
  }
}

package com.board.bbs.post.domain;

/**
 * 게시글 제목.
 *
 * @param value 앞뒤 공백이 제거된 제목
 */
public record Title(String value) {

  private static final int MAX_LENGTH = 100;

  /** 생성 시점에 불변식을 보장한다. */
  public Title {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("제목은 비어 있을 수 없습니다.");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("제목은 " + MAX_LENGTH + "자를 넘을 수 없습니다.");
    }
  }
}

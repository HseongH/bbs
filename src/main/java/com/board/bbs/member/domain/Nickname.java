package com.board.bbs.member.domain;

/**
 * 회원 닉네임.
 *
 * @param value 앞뒤 공백이 제거된 닉네임
 */
public record Nickname(String value) {

  private static final int MAX_LENGTH = 50;

  /** 생성 시점에 불변식을 보장한다. */
  public Nickname {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("닉네임은 비어 있을 수 없습니다.");
    }
    value = value.trim();
    if (value.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("닉네임은 " + MAX_LENGTH + "자를 넘을 수 없습니다.");
    }
  }
}

package com.board.bbs.member.domain;

/**
 * 회원 식별자.
 *
 * @param value 1 이상의 식별자 값
 */
public record MemberId(Long value) {

  /** 생성 시점에 불변식을 보장한다. */
  public MemberId {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException("회원 식별자가 올바르지 않습니다.");
    }
  }
}

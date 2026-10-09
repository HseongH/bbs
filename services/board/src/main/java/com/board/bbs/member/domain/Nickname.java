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

  /**
   * 외부 인증 서버에서 받은 이름으로 닉네임을 만든다. 길이 제한을 넘으면 거부하지 않고 잘라서, 이름이 길다는 이유로 로그인이 막히지 않게 한다.
   *
   * @param value 외부에서 받은 이름
   * @return 최대 길이 이내로 자른 닉네임
   */
  public static Nickname truncating(String value) {
    String trimmed = value.trim();
    if (trimmed.length() <= MAX_LENGTH) {
      return new Nickname(trimmed);
    }
    int end = MAX_LENGTH;
    // 두 개의 char로 이루어진 문자(이모지 등)를 가운데에서 자르지 않는다.
    if (Character.isHighSurrogate(trimmed.charAt(end - 1))) {
      end--;
    }
    return new Nickname(trimmed.substring(0, end));
  }
}

package com.board.bbs.member.domain;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Keycloak 사용자에 대응하는 로컬 회원. */
public class Member {

  @Nullable private final MemberId id;
  private final String subject;
  private Nickname nickname;
  private final String email;

  private Member(@Nullable MemberId id, String subject, Nickname nickname, String email) {
    this.id = id;
    this.subject = Objects.requireNonNull(subject, "subject는 필수입니다.");
    this.nickname = Objects.requireNonNull(nickname, "nickname은 필수입니다.");
    this.email = Objects.requireNonNull(email, "email은 필수입니다.");
  }

  /**
   * Keycloak 최초 로그인 시 신규 회원을 만든다. 아직 식별자가 없다.
   *
   * @param subject Keycloak 사용자 식별자
   * @param nickname 닉네임
   * @param email 이메일
   * @return 식별자가 부여되지 않은 회원
   */
  public static Member provision(String subject, Nickname nickname, String email) {
    return new Member(null, subject, nickname, email);
  }

  /**
   * 영속화된 회원을 복원한다. 어댑터에서만 사용한다.
   *
   * @param id 회원 식별자
   * @param subject Keycloak 사용자 식별자
   * @param nickname 닉네임
   * @param email 이메일
   * @return 복원된 회원
   */
  public static Member restore(MemberId id, String subject, Nickname nickname, String email) {
    return new Member(Objects.requireNonNull(id), subject, nickname, email);
  }

  /**
   * 닉네임을 변경한다.
   *
   * @param newNickname 새 닉네임
   */
  public void changeNickname(Nickname newNickname) {
    this.nickname = Objects.requireNonNull(newNickname);
  }

  @Nullable
  public MemberId getId() {
    return id;
  }

  public String getSubject() {
    return subject;
  }

  public Nickname getNickname() {
    return nickname;
  }

  public String getEmail() {
    return email;
  }
}

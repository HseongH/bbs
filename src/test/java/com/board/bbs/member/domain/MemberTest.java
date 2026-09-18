package com.board.bbs.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MemberTest {

  @Test
  void 신규_회원은_식별자가_없는_상태로_생성된다() {
    Member member = Member.provision("keycloak-sub-1", new Nickname("테스터"), "tester@example.com");

    assertThat(member.getId()).isNull();
    assertThat(member.getSubject()).isEqualTo("keycloak-sub-1");
    assertThat(member.getNickname().value()).isEqualTo("테스터");
  }

  @Test
  void 닉네임은_비어있을_수_없다() {
    assertThatThrownBy(() -> new Nickname(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 닉네임은_50자를_넘을_수_없다() {
    String tooLong = "가".repeat(51);

    assertThatThrownBy(() -> new Nickname(tooLong)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 회원_식별자는_양수여야_한다() {
    assertThatThrownBy(() -> new MemberId(0L)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new MemberId(null)).isInstanceOf(IllegalArgumentException.class);
  }
}

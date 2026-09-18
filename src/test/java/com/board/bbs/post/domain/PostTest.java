package com.board.bbs.post.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.board.bbs.member.domain.MemberId;
import org.junit.jupiter.api.Test;

class PostTest {

  private static final MemberId AUTHOR = new MemberId(1L);

  @Test
  void 게시글을_작성하면_조회수와_좋아요는_0에서_시작한다() {
    Post post = Post.write(new Title("제목"), new Content("본문"), AUTHOR);

    assertThat(post.getViewCount()).isZero();
    assertThat(post.getLikeCount()).isZero();
    assertThat(post.isDeleted()).isFalse();
  }

  @Test
  void 작성한_게시글은_아직_식별자가_없다() {
    Post post = Post.write(new Title("제목"), new Content("본문"), AUTHOR);

    assertThat(post.getId()).isNull();
    assertThat(post.getAuthorId()).isEqualTo(AUTHOR);
  }

  @Test
  void 제목은_비어있을_수_없다() {
    assertThatThrownBy(() -> new Title("  ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 제목은_100자를_넘을_수_없다() {
    assertThatThrownBy(() -> new Title("가".repeat(101)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 제목의_앞뒤_공백은_제거된다() {
    assertThat(new Title("  제목  ").value()).isEqualTo("제목");
  }

  @Test
  void 본문은_10000자를_넘을_수_없다() {
    assertThatThrownBy(() -> new Content("가".repeat(10_001)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 본문은_비어있을_수_없다() {
    assertThatThrownBy(() -> new Content(" ")).isInstanceOf(IllegalArgumentException.class);
  }
}

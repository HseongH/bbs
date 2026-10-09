package com.board.bbs.post.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.domain.MemberId;
import java.time.Instant;
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

  @Test
  void 작성자는_게시글을_수정할_수_있다() {
    Post post = 저장된_게시글(AUTHOR);

    post.updateBy(AUTHOR, new Title("바뀐 제목"), new Content("바뀐 본문"));

    assertThat(post.getTitle().value()).isEqualTo("바뀐 제목");
    assertThat(post.getContent().value()).isEqualTo("바뀐 본문");
  }

  @Test
  void 작성자가_아니면_수정할_수_없다() {
    Post post = 저장된_게시글(AUTHOR);
    MemberId 다른사람 = new MemberId(2L);

    assertThatThrownBy(() -> post.updateBy(다른사람, new Title("제목"), new Content("본문")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void 작성자는_게시글을_삭제할_수_있다() {
    Post post = 저장된_게시글(AUTHOR);

    post.deleteBy(AUTHOR, false, Instant.now());

    assertThat(post.isDeleted()).isTrue();
  }

  @Test
  void 작성자가_아니면_삭제할_수_없다() {
    Post post = 저장된_게시글(AUTHOR);

    assertThatThrownBy(() -> post.deleteBy(new MemberId(2L), false, Instant.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void 관리자는_다른_사람의_글을_삭제할_수_있다() {
    Post post = 저장된_게시글(AUTHOR);

    post.deleteBy(new MemberId(99L), true, Instant.now());

    assertThat(post.isDeleted()).isTrue();
  }

  @Test
  void 이미_삭제된_게시글은_수정할_수_없다() {
    Post post = 저장된_게시글(AUTHOR);
    post.deleteBy(AUTHOR, false, Instant.now());

    assertThatThrownBy(() -> post.updateBy(AUTHOR, new Title("제목"), new Content("본문")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.POST_NOT_FOUND);
  }

  @Test
  void 이미_삭제된_게시글은_다시_삭제할_수_없다() {
    Post post = 저장된_게시글(AUTHOR);
    post.deleteBy(AUTHOR, false, Instant.now());

    assertThatThrownBy(() -> post.deleteBy(AUTHOR, false, Instant.now()))
        .isInstanceOf(BusinessException.class);
  }

  private Post 저장된_게시글(MemberId author) {
    return Post.restore(
        new PostId(1L), new Title("제목"), new Content("본문"), author, Instant.now(), null, 0L, 0L);
  }
}

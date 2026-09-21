package com.board.bbs.comment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CommentTest {

  private static final PostId POST = new PostId(1L);
  private static final MemberId AUTHOR = new MemberId(1L);

  @Test
  void 원댓글의_깊이는_0이다() {
    Comment comment = Comment.write(POST, AUTHOR, new CommentBody("댓글"), null, 0);

    assertThat(comment.getDepth()).isZero();
    assertThat(comment.getParentId()).isNull();
  }

  @Test
  void 원댓글에_달린_답글의_깊이는_1이다() {
    Comment reply = Comment.write(POST, AUTHOR, new CommentBody("답글"), new CommentId(10L), 0);

    assertThat(reply.getDepth()).isEqualTo(1);
    assertThat(reply.getParentId()).isEqualTo(new CommentId(10L));
  }

  @Test
  void 대댓글에는_답글을_달_수_없다() {
    assertThatThrownBy(
            () -> Comment.write(POST, AUTHOR, new CommentBody("답답글"), new CommentId(10L), 1))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMENT_DEPTH_EXCEEDED);
  }

  @Test
  void 본문은_비어있을_수_없다() {
    assertThatThrownBy(() -> new CommentBody(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 본문은_1000자를_넘을_수_없다() {
    assertThatThrownBy(() -> new CommentBody("가".repeat(1_001)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 작성자는_댓글을_수정할_수_있다() {
    Comment comment = 저장된_댓글();

    comment.updateBy(AUTHOR, new CommentBody("수정된 댓글"));

    assertThat(comment.getBody().value()).isEqualTo("수정된 댓글");
  }

  @Test
  void 작성자가_아니면_수정할_수_없다() {
    Comment comment = 저장된_댓글();

    assertThatThrownBy(() -> comment.updateBy(new MemberId(2L), new CommentBody("수정")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void 관리자는_다른_사람의_댓글을_삭제할_수_있다() {
    Comment comment = 저장된_댓글();

    comment.deleteBy(new MemberId(99L), true, Instant.now());

    assertThat(comment.isDeleted()).isTrue();
  }

  @Test
  void 작성자가_아니면_삭제할_수_없다() {
    Comment comment = 저장된_댓글();

    assertThatThrownBy(() -> comment.deleteBy(new MemberId(2L), false, Instant.now()))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ACCESS_DENIED);
  }

  @Test
  void 이미_삭제된_댓글은_수정할_수_없다() {
    Comment comment = 저장된_댓글();
    comment.deleteBy(AUTHOR, false, Instant.now());

    assertThatThrownBy(() -> comment.updateBy(AUTHOR, new CommentBody("수정")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.COMMENT_NOT_FOUND);
  }

  private Comment 저장된_댓글() {
    return Comment.restore(
        new CommentId(1L), POST, AUTHOR, new CommentBody("댓글"), null, 0, Instant.now(), null);
  }
}

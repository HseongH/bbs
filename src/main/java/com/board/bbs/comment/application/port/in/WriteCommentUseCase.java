package com.board.bbs.comment.application.port.in;

import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;
import org.jspecify.annotations.Nullable;

/** 댓글 작성 유스케이스. */
public interface WriteCommentUseCase {

  /**
   * 댓글 또는 답글을 작성한다.
   *
   * @param postId 대상 게시글 식별자
   * @param author 작성자 식별자
   * @param body 본문
   * @param parentCommentId 부모 댓글 식별자. 원댓글이면 null
   * @return 작성된 댓글의 식별자
   */
  CommentId write(PostId postId, MemberId author, String body, @Nullable Long parentCommentId);
}

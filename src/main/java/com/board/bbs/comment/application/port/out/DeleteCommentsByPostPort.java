package com.board.bbs.comment.application.port.out;

import com.board.bbs.post.domain.PostId;
import java.time.Instant;

/** 게시글 삭제에 따른 댓글 일괄 삭제 포트. */
public interface DeleteCommentsByPostPort {

  /**
   * 게시글에 달린 모든 댓글을 같은 시각으로 소프트 삭제한다.
   *
   * @param postId 게시글 식별자
   * @param now 삭제 시각
   */
  void softDeleteAllByPost(PostId postId, Instant now);
}

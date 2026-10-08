package com.board.bbs.comment.application.port.in;

import com.board.bbs.post.domain.PostId;
import java.time.Instant;

/** 게시글에 달린 댓글 일괄 삭제 유스케이스. */
public interface DeleteCommentsOfPostUseCase {

  /**
   * 게시글에 달린 댓글을 모두 삭제한다.
   *
   * @param postId 게시글 식별자
   * @param deletedAt 삭제 시각
   */
  void deleteAllOfPost(PostId postId, Instant deletedAt);
}

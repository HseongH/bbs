package com.board.bbs.comment.application.port.out;

import com.board.bbs.comment.domain.Comment;

/** 댓글 저장 포트. */
public interface SaveCommentPort {

  /**
   * 댓글을 저장하고 식별자가 부여된 댓글을 반환한다.
   *
   * @param comment 저장할 댓글
   * @return 저장된 댓글
   */
  Comment save(Comment comment);
}

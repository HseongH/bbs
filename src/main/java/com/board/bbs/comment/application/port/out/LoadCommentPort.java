package com.board.bbs.comment.application.port.out;

import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentId;

/** 댓글 조회 포트. */
public interface LoadCommentPort {

  /**
   * 삭제되지 않은 댓글을 읽는다.
   *
   * @param id 댓글 식별자
   * @return 댓글
   * @throws com.board.bbs.common.error.BusinessException 댓글이 없으면 COMMENT_NOT_FOUND
   */
  Comment load(CommentId id);
}

package com.board.bbs.comment.application.port.in;

import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.member.domain.MemberId;

/** 댓글 수정 유스케이스. */
public interface UpdateCommentUseCase {

  /**
   * 댓글을 수정한다.
   *
   * @param id 댓글 식별자
   * @param requester 요청한 회원 식별자
   * @param body 새 본문
   */
  void update(CommentId id, MemberId requester, String body);
}

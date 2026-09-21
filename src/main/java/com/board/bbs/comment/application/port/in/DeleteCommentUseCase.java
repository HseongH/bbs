package com.board.bbs.comment.application.port.in;

import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.member.domain.MemberId;

/** 댓글 삭제 유스케이스. */
public interface DeleteCommentUseCase {

  /**
   * 댓글을 삭제한다.
   *
   * @param id 댓글 식별자
   * @param requester 요청한 회원 식별자
   * @param admin 관리자 여부
   */
  void delete(CommentId id, MemberId requester, boolean admin);
}

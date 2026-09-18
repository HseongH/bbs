package com.board.bbs.post.application.port.in;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;

/** 게시글 삭제 유스케이스. */
public interface DeletePostUseCase {

  /**
   * 게시글을 삭제한다.
   *
   * @param id 게시글 식별자
   * @param requester 요청한 회원 식별자
   * @param admin 관리자 여부
   */
  void delete(PostId id, MemberId requester, boolean admin);
}

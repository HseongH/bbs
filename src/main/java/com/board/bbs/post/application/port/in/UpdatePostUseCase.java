package com.board.bbs.post.application.port.in;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;

/** 게시글 수정 유스케이스. */
public interface UpdatePostUseCase {

  /**
   * 게시글을 수정한다.
   *
   * @param id 게시글 식별자
   * @param requester 요청한 회원 식별자
   * @param title 새 제목
   * @param content 새 본문
   */
  void update(PostId id, MemberId requester, String title, String content);
}

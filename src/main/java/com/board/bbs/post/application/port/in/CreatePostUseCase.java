package com.board.bbs.post.application.port.in;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;

/** 게시글 작성 유스케이스. */
public interface CreatePostUseCase {

  /**
   * 게시글을 작성한다.
   *
   * @param author 작성자 식별자
   * @param title 제목
   * @param content 본문
   * @return 작성된 게시글의 식별자
   */
  PostId create(MemberId author, String title, String content);
}

package com.board.bbs.post.application.port.out;

import com.board.bbs.post.domain.Post;

/** 게시글 저장 포트. */
public interface SavePostPort {

  /**
   * 게시글을 저장하고 식별자가 부여된 게시글을 반환한다.
   *
   * @param post 저장할 게시글
   * @return 저장된 게시글
   */
  Post save(Post post);
}

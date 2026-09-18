package com.board.bbs.post.application.port.in;

import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;

/** 게시글 단건 조회 유스케이스. */
public interface GetPostUseCase {

  /**
   * 게시글을 조회한다.
   *
   * @param id 게시글 식별자
   * @return 게시글
   */
  Post getById(PostId id);
}

package com.board.bbs.post.application.port.in;

import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;

/** 게시글 조회와 조회수 집계 유스케이스. */
public interface ViewPostUseCase {

  /**
   * 게시글을 조회하고 최초 조회인 경우 조회수를 올린다.
   *
   * @param id 게시글 식별자
   * @param viewerKey 조회자 식별 키
   * @return 게시글
   */
  Post getAndCountView(PostId id, String viewerKey);
}

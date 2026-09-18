package com.board.bbs.post.application.port.out;

import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;

/** 게시글 조회 포트. */
public interface LoadPostPort {

  /**
   * 삭제되지 않은 게시글을 읽는다.
   *
   * @param id 게시글 식별자
   * @return 게시글
   * @throws com.board.bbs.common.error.BusinessException 게시글이 없으면 POST_NOT_FOUND
   */
  Post load(PostId id);
}

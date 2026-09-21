package com.board.bbs.post.application.port.out;

import com.board.bbs.post.domain.PostId;

/** 게시글 카운터 증감 포트. 경합에서 값을 잃지 않도록 원자적으로 처리한다. */
public interface PostCounterPort {

  /**
   * 조회수를 1 증가시킨다.
   *
   * @param postId 게시글 식별자
   */
  void increaseViewCount(PostId postId);

  /**
   * 좋아요 수를 1 증가시킨다.
   *
   * @param postId 게시글 식별자
   */
  void increaseLikeCount(PostId postId);

  /**
   * 좋아요 수를 1 감소시킨다. 0 미만으로 내려가지 않는다.
   *
   * @param postId 게시글 식별자
   */
  void decreaseLikeCount(PostId postId);
}

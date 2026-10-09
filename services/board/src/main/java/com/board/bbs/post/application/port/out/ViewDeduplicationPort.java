package com.board.bbs.post.application.port.out;

import com.board.bbs.post.domain.PostId;

/** 조회수 중복 집계 방지 포트. */
public interface ViewDeduplicationPort {

  /**
   * 조회 사실을 기록하고 이번이 최초인지 알려준다.
   *
   * @param postId 게시글 식별자
   * @param viewerKey 조회자 식별 키
   * @return 일정 기간 내 최초 조회이면 true
   */
  boolean markViewed(PostId postId, String viewerKey);

  /**
   * 기록한 조회 사실을 지운다. 조회수 반영이 롤백되었을 때 다음 조회가 다시 집계되도록 쓴다.
   *
   * @param postId 게시글 식별자
   * @param viewerKey 조회자 식별 키
   */
  void unmarkViewed(PostId postId, String viewerKey);
}

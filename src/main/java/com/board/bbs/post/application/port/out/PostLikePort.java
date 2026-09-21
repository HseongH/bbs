package com.board.bbs.post.application.port.out;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;

/** 좋아요 기록 포트. */
public interface PostLikePort {

  /**
   * 좋아요를 추가한다.
   *
   * @param postId 게시글 식별자
   * @param memberId 회원 식별자
   * @return 새로 추가되었으면 true, 이미 있었으면 false
   */
  boolean like(PostId postId, MemberId memberId);

  /**
   * 좋아요를 취소한다.
   *
   * @param postId 게시글 식별자
   * @param memberId 회원 식별자
   * @return 삭제되었으면 true, 원래 없었으면 false
   */
  boolean unlike(PostId postId, MemberId memberId);
}

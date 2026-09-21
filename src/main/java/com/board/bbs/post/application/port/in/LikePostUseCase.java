package com.board.bbs.post.application.port.in;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;

/** 좋아요 유스케이스. */
public interface LikePostUseCase {

  /**
   * 게시글에 좋아요를 누른다.
   *
   * @param postId 게시글 식별자
   * @param memberId 회원 식별자
   */
  void like(PostId postId, MemberId memberId);

  /**
   * 좋아요를 취소한다.
   *
   * @param postId 게시글 식별자
   * @param memberId 회원 식별자
   */
  void unlike(PostId postId, MemberId memberId);
}

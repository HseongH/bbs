package com.board.bbs.post.application.service;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.port.out.PostLikeRepository;
import com.board.bbs.post.application.port.out.PostRepository;
import com.board.bbs.post.domain.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 좋아요 유스케이스 구현. 카운터는 기록이 실제로 바뀐 경우에만 움직인다. */
@Service
@RequiredArgsConstructor
public class PostLikeService {

  private final PostRepository postRepository;
  private final PostLikeRepository postLikeRepository;

  /**
   * 게시글에 좋아요를 누른다.
   *
   * @param postId 게시글 식별자
   * @param memberId 회원 식별자
   */
  @Transactional
  public void like(PostId postId, MemberId memberId) {
    postRepository.load(postId);
    if (!postLikeRepository.like(postId, memberId)) {
      throw new BusinessException(ErrorCode.ALREADY_LIKED);
    }
    postRepository.increaseLikeCount(postId);
  }

  /**
   * 좋아요를 취소한다.
   *
   * @param postId 게시글 식별자
   * @param memberId 회원 식별자
   */
  @Transactional
  public void unlike(PostId postId, MemberId memberId) {
    postRepository.load(postId);
    if (!postLikeRepository.unlike(postId, memberId)) {
      throw new BusinessException(ErrorCode.NOT_LIKED);
    }
    postRepository.decreaseLikeCount(postId);
  }
}

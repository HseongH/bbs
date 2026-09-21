package com.board.bbs.post.application.service;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.port.in.LikePostUseCase;
import com.board.bbs.post.application.port.out.LoadPostPort;
import com.board.bbs.post.application.port.out.PostCounterPort;
import com.board.bbs.post.application.port.out.PostLikePort;
import com.board.bbs.post.domain.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 좋아요 유스케이스 구현. 카운터는 기록이 실제로 바뀐 경우에만 움직인다. */
@Service
@RequiredArgsConstructor
public class PostLikeService implements LikePostUseCase {

  private final LoadPostPort loadPostPort;
  private final PostLikePort postLikePort;
  private final PostCounterPort postCounterPort;

  @Override
  @Transactional
  public void like(PostId postId, MemberId memberId) {
    loadPostPort.load(postId);
    if (!postLikePort.like(postId, memberId)) {
      throw new BusinessException(ErrorCode.ALREADY_LIKED);
    }
    postCounterPort.increaseLikeCount(postId);
  }

  @Override
  @Transactional
  public void unlike(PostId postId, MemberId memberId) {
    loadPostPort.load(postId);
    if (!postLikePort.unlike(postId, memberId)) {
      throw new BusinessException(ErrorCode.NOT_LIKED);
    }
    postCounterPort.decreaseLikeCount(postId);
  }
}

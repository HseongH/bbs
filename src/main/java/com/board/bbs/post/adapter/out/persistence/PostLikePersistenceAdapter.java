package com.board.bbs.post.adapter.out.persistence;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.port.out.PostLikePort;
import com.board.bbs.post.domain.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 좋아요 기록 어댑터. */
@Component
@RequiredArgsConstructor
public class PostLikePersistenceAdapter implements PostLikePort {

  private final PostLikeJpaRepository repository;

  @Override
  public boolean like(PostId postId, MemberId memberId) {
    return repository.insertIfAbsent(postId.value(), memberId.value()) > 0;
  }

  @Override
  public boolean unlike(PostId postId, MemberId memberId) {
    return repository.deleteIfPresent(postId.value(), memberId.value()) > 0;
  }
}

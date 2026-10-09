package com.board.bbs.post.adapter.out.persistence;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.port.out.PostLikeRepository;
import com.board.bbs.post.domain.PostId;
import org.springframework.stereotype.Component;

/** 좋아요 기록 어댑터. */
@Component
public class PostLikePersistenceAdapter implements PostLikeRepository {

  private final PostLikeJpaRepository repository;

  PostLikePersistenceAdapter(PostLikeJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean like(PostId postId, MemberId memberId) {
    return repository.insertIfAbsent(postId.value(), memberId.value()) > 0;
  }

  @Override
  public boolean unlike(PostId postId, MemberId memberId) {
    return repository.deleteIfPresent(postId.value(), memberId.value()) > 0;
  }
}

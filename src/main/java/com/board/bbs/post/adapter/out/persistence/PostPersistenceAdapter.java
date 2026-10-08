package com.board.bbs.post.adapter.out.persistence;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.post.application.PostSearchCondition;
import com.board.bbs.post.application.PostSummary;
import com.board.bbs.post.application.port.out.PostRepository;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** 게시글 영속성 어댑터. */
@Component
public class PostPersistenceAdapter implements PostRepository {

  private final PostJpaRepository repository;
  private final PostQueryRepository queryRepository;

  PostPersistenceAdapter(PostJpaRepository repository, PostQueryRepository queryRepository) {
    this.repository = repository;
    this.queryRepository = queryRepository;
  }

  @Override
  public Post save(Post post) {
    return PostMapper.toDomain(repository.save(PostMapper.toEntity(post)));
  }

  @Override
  public Post load(PostId id) {
    return repository
        .findByIdAndDeletedAtIsNull(id.value())
        .map(PostMapper::toDomain)
        .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
  }

  @Override
  public Page<PostSummary> search(PostSearchCondition condition, Pageable pageable) {
    return queryRepository.search(condition, pageable);
  }

  @Override
  public void increaseViewCount(PostId postId) {
    repository.increaseViewCount(postId.value());
  }

  @Override
  public void increaseLikeCount(PostId postId) {
    repository.increaseLikeCount(postId.value());
  }

  @Override
  public void decreaseLikeCount(PostId postId) {
    repository.decreaseLikeCount(postId.value());
  }
}

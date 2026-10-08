package com.board.bbs.comment.adapter.out.persistence;

import com.board.bbs.comment.application.port.out.CommentRepository;
import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** 댓글 영속성 어댑터. */
@Component
public class CommentPersistenceAdapter implements CommentRepository {

  private final CommentJpaRepository repository;

  CommentPersistenceAdapter(CommentJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Comment save(Comment comment) {
    return CommentMapper.toDomain(repository.save(CommentMapper.toEntity(comment)));
  }

  @Override
  public Comment load(CommentId id) {
    return repository
        .findByIdAndDeletedAtIsNull(id.value())
        .map(CommentMapper::toDomain)
        .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
  }

  @Override
  public Page<Comment> listByPost(PostId postId, Pageable pageable) {
    return repository
        .findByPostIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(postId.value(), pageable)
        .map(CommentMapper::toDomain);
  }

  @Override
  public void softDeleteAllByPost(PostId postId, Instant now) {
    repository.softDeleteAllByPostId(postId.value(), now);
  }
}

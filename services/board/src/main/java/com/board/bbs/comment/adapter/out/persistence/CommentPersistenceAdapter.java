package com.board.bbs.comment.adapter.out.persistence;

import com.board.bbs.comment.application.port.out.CommentRepository;
import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
  public Page<Comment> listRoots(PostId postId, Pageable pageable) {
    Pageable withoutSort = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    return repository.findRootsForListing(postId.value(), withoutSort).map(CommentMapper::toDomain);
  }

  @Override
  public List<Comment> listRepliesOf(List<CommentId> rootIds) {
    if (rootIds.isEmpty()) {
      return List.of();
    }
    List<Long> ids = rootIds.stream().map(CommentId::value).toList();
    return repository
        .findByParentCommentIdInAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(ids)
        .stream()
        .map(CommentMapper::toDomain)
        .toList();
  }

  @Override
  public void softDeleteAllByPost(PostId postId, Instant now) {
    repository.softDeleteAllByPostId(postId.value(), now);
  }
}

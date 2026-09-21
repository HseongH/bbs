package com.board.bbs.comment.adapter.out.persistence;

import com.board.bbs.comment.application.port.out.DeleteCommentsByPostPort;
import com.board.bbs.comment.application.port.out.ListCommentPort;
import com.board.bbs.comment.application.port.out.LoadCommentPort;
import com.board.bbs.comment.application.port.out.SaveCommentPort;
import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/** 댓글 영속성 어댑터. */
@Component
@RequiredArgsConstructor
public class CommentPersistenceAdapter
    implements SaveCommentPort, LoadCommentPort, ListCommentPort, DeleteCommentsByPostPort {

  private final CommentJpaRepository repository;

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

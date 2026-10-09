package com.board.bbs.comment.adapter.in.web.dto;

import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentId;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * 댓글 응답. 삭제된 댓글은 자리만 남기고 본문과 작성자를 담지 않는다.
 *
 * @param id 댓글 식별자
 * @param postId 게시글 식별자
 * @param authorId 작성자 식별자. 삭제된 댓글이면 null
 * @param body 본문. 삭제된 댓글이면 null
 * @param deleted 삭제 여부
 * @param parentCommentId 부모 댓글 식별자. 원댓글이면 null
 * @param depth 깊이
 * @param createdAt 작성 시각
 */
public record CommentResponse(
    Long id,
    Long postId,
    @Nullable Long authorId,
    @Nullable String body,
    boolean deleted,
    @Nullable Long parentCommentId,
    int depth,
    Instant createdAt) {

  /**
   * 도메인 댓글을 응답으로 바꾼다.
   *
   * @param comment 댓글
   * @return 댓글 응답
   */
  public static CommentResponse from(Comment comment) {
    CommentId id = Objects.requireNonNull(comment.getId(), "저장된 댓글은 식별자를 가진다.");
    CommentId parentId = comment.getParentId();
    boolean deleted = comment.isDeleted();
    return new CommentResponse(
        id.value(),
        comment.getPostId().value(),
        deleted ? null : comment.getAuthorId().value(),
        deleted ? null : comment.getBody().value(),
        deleted,
        parentId == null ? null : parentId.value(),
        comment.getDepth(),
        Objects.requireNonNull(comment.getCreatedAt(), "저장된 댓글은 작성 시각을 가진다."));
  }
}

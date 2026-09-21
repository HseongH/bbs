package com.board.bbs.comment.adapter.out.persistence;

import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentBody;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;
import java.util.Objects;

/** 도메인과 영속성 모델 사이의 유일한 변환 지점. */
final class CommentMapper {

  private CommentMapper() {}

  static Comment toDomain(CommentJpaEntity entity) {
    Long parentId = entity.getParentCommentId();
    return Comment.restore(
        new CommentId(Objects.requireNonNull(entity.getId(), "저장된 댓글은 식별자를 가진다.")),
        new PostId(entity.getPostId()),
        new MemberId(entity.getAuthorId()),
        new CommentBody(entity.getBody()),
        parentId == null ? null : new CommentId(parentId),
        entity.getDepth(),
        Objects.requireNonNull(entity.getCreatedAt(), "저장된 댓글은 작성 시각을 가진다."),
        entity.getDeletedAt());
  }

  static CommentJpaEntity toEntity(Comment comment) {
    CommentId id = comment.getId();
    CommentId parentId = comment.getParentId();
    return new CommentJpaEntity(
        id == null ? null : id.value(),
        comment.getPostId().value(),
        comment.getAuthorId().value(),
        comment.getBody().value(),
        parentId == null ? null : parentId.value(),
        (short) comment.getDepth(),
        comment.getCreatedAt(),
        comment.getDeletedAt());
  }
}

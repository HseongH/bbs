package com.board.bbs.comment.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 댓글 영속성 매핑. 게시글과 부모 댓글은 식별자 컬럼으로만 참조한다. */
@Entity
@Table(name = "comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class CommentJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable private Long id;

  @Column(name = "post_id", nullable = false, updatable = false)
  private Long postId;

  @Column(name = "author_id", nullable = false, updatable = false)
  private Long authorId;

  @Column(nullable = false, length = 1_000)
  private String body;

  @Column(name = "parent_comment_id", updatable = false)
  @Nullable private Long parentCommentId;

  @Column(nullable = false, updatable = false)
  private short depth;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  @Nullable private Instant createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  @Nullable private Instant updatedAt;

  @Nullable private Instant deletedAt;

  CommentJpaEntity(
      @Nullable Long id,
      Long postId,
      Long authorId,
      String body,
      @Nullable Long parentCommentId,
      short depth,
      @Nullable Instant createdAt,
      @Nullable Instant deletedAt) {

    this.id = id;
    this.postId = postId;
    this.authorId = authorId;
    this.body = body;
    this.parentCommentId = parentCommentId;
    this.depth = depth;
    this.createdAt = createdAt;
    this.deletedAt = deletedAt;
  }
}

package com.board.bbs.post.adapter.out.persistence;

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

/** 게시글 영속성 매핑. */
@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PostJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable private Long id;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, length = 10_000)
  private String content;

  @Column(name = "author_id", nullable = false, updatable = false)
  private Long authorId;

  @Column(nullable = false)
  private long viewCount;

  @Column(nullable = false)
  private long likeCount;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  @Nullable private Instant createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  @Nullable private Instant updatedAt;

  @Nullable private Instant deletedAt;

  PostJpaEntity(
      @Nullable Long id,
      String title,
      String content,
      Long authorId,
      long viewCount,
      long likeCount,
      @Nullable Instant createdAt,
      @Nullable Instant deletedAt) {

    this.id = id;
    this.title = title;
    this.content = content;
    this.authorId = authorId;
    this.viewCount = viewCount;
    this.likeCount = likeCount;
    this.createdAt = createdAt;
    this.deletedAt = deletedAt;
  }
}

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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** 좋아요 기록. (post_id, member_id) 유니크 제약이 중복의 최종 방어선이다. */
@Entity
@Table(name = "post_like")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PostLikeJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Nullable
  private Long id;

  @Column(name = "post_id", nullable = false, updatable = false)
  private Long postId;

  @Column(name = "member_id", nullable = false, updatable = false)
  private Long memberId;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  @Nullable
  private Instant createdAt;

  PostLikeJpaEntity(Long postId, Long memberId) {
    this.postId = postId;
    this.memberId = memberId;
  }
}

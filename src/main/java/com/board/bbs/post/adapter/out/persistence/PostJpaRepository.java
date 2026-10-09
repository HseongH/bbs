package com.board.bbs.post.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {

  Optional<PostJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  /** 공유 잠금(FOR SHARE)은 다른 트랜잭션의 삭제(UPDATE)와 충돌하고, 다른 공유 잠금과는 충돌하지 않는다. */
  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("SELECT p.id FROM PostJpaEntity p WHERE p.id = :id AND p.deletedAt IS NULL")
  Optional<Long> lockAliveId(@Param("id") Long id);

  /** 엔티티를 읽어 더하는 방식은 경합에서 값을 잃는다. 데이터베이스가 직접 더하게 한다. */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE PostJpaEntity p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
  void increaseViewCount(@Param("id") Long id);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("UPDATE PostJpaEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
  void increaseLikeCount(@Param("id") Long id);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "UPDATE PostJpaEntity p SET p.likeCount = p.likeCount - 1"
          + " WHERE p.id = :id AND p.likeCount > 0")
  void decreaseLikeCount(@Param("id") Long id);
}

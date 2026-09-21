package com.board.bbs.post.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {

  Optional<PostJpaEntity> findByIdAndDeletedAtIsNull(Long id);

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

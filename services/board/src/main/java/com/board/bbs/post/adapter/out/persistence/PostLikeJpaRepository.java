package com.board.bbs.post.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostLikeJpaRepository extends JpaRepository<PostLikeJpaEntity, Long> {

  /**
   * 경합을 데이터베이스가 판정하게 한다. 사전 조회 후 저장하는 방식은 두 요청이 동시에 통과할 수 있고, 제약 위반 예외가 나면 진행 중인 트랜잭션까지 못 쓰게 된다.
   *
   * @return 실제로 추가된 행 수. 이미 있었으면 0
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          "INSERT INTO post_like (post_id, member_id, created_at)"
              + " VALUES (:postId, :memberId, now()) ON CONFLICT DO NOTHING",
      nativeQuery = true)
  int insertIfAbsent(@Param("postId") Long postId, @Param("memberId") Long memberId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value = "DELETE FROM post_like WHERE post_id = :postId AND member_id = :memberId",
      nativeQuery = true)
  int deleteIfPresent(@Param("postId") Long postId, @Param("memberId") Long memberId);
}

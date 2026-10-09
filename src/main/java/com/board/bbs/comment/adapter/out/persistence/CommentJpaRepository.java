package com.board.bbs.comment.adapter.out.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, Long> {

  Optional<CommentJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  Page<CommentJpaEntity> findByPostIdAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(
      Long postId, Pageable pageable);

  String ROOTS_FOR_LISTING =
      " FROM CommentJpaEntity c"
          + " WHERE c.postId = :postId AND c.depth = 0"
          + " AND (c.deletedAt IS NULL OR EXISTS ("
          + "   SELECT r.id FROM CommentJpaEntity r"
          + "   WHERE r.parentCommentId = c.id AND r.deletedAt IS NULL))";

  /** 목록에 나올 원댓글: 삭제되지 않았거나, 살아 있는 대댓글이 있는 원댓글. */
  @Query(
      value = "SELECT c" + ROOTS_FOR_LISTING + " ORDER BY c.createdAt ASC, c.id ASC",
      countQuery = "SELECT count(c)" + ROOTS_FOR_LISTING)
  Page<CommentJpaEntity> findRootsForListing(@Param("postId") Long postId, Pageable pageable);

  List<CommentJpaEntity> findByParentCommentIdInAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(
      Collection<Long> parentCommentIds);

  // 벌크 연산은 영속성 컨텍스트를 우회하므로, 아직 반영되지 않은 변경을 먼저 flush한 뒤
  // 실행해야 한다. flush 없이 clear하면 같은 트랜잭션의 변경이 유실된다.
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "UPDATE CommentJpaEntity c SET c.deletedAt = :now"
          + " WHERE c.postId = :postId AND c.deletedAt IS NULL")
  void softDeleteAllByPostId(@Param("postId") Long postId, @Param("now") Instant now);
}

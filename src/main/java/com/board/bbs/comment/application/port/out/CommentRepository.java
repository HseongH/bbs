package com.board.bbs.comment.application.port.out;

import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 댓글 저장소 포트. */
public interface CommentRepository {
  /**
   * 댓글을 저장하고 식별자가 부여된 댓글을 반환한다.
   *
   * @param comment 저장할 댓글
   * @return 저장된 댓글
   */
  Comment save(Comment comment);

  /**
   * 삭제되지 않은 댓글을 읽는다.
   *
   * @param id 댓글 식별자
   * @return 댓글
   * @throws com.board.bbs.common.error.BusinessException 댓글이 없으면 COMMENT_NOT_FOUND
   */
  Comment load(CommentId id);

  /**
   * 게시글의 삭제되지 않은 댓글을 작성 순서대로 조회한다.
   *
   * @param postId 게시글 식별자
   * @param pageable 페이지 정보
   * @return 댓글 페이지
   */
  Page<Comment> listByPost(PostId postId, Pageable pageable);

  /**
   * 목록에 나올 원댓글을 한 페이지 읽는다. 삭제되지 않았거나, 살아 있는 대댓글이 있는 원댓글이다.
   *
   * <p>작성 순(같으면 식별자 순)으로 고정하며 {@code pageable}의 정렬은 무시한다.
   *
   * @param postId 게시글 식별자
   * @param pageable 페이지 번호와 크기
   * @return 원댓글 페이지. 삭제된 원댓글은 삭제된 상태 그대로 담긴다
   */
  Page<Comment> listRoots(PostId postId, Pageable pageable);

  /**
   * 주어진 원댓글들의 삭제되지 않은 대댓글을 작성 순(같으면 식별자 순)으로 읽는다.
   *
   * @param rootIds 원댓글 식별자 목록
   * @return 대댓글 목록. 입력이 비어 있으면 빈 목록
   */
  List<Comment> listRepliesOf(List<CommentId> rootIds);

  /**
   * 게시글에 달린 모든 댓글을 같은 시각으로 소프트 삭제한다.
   *
   * @param postId 게시글 식별자
   * @param now 삭제 시각
   */
  void softDeleteAllByPost(PostId postId, Instant now);
}

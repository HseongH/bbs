package com.board.bbs.post.application.port.out;

import com.board.bbs.post.application.PostSearchCondition;
import com.board.bbs.post.application.PostSummary;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 게시글 저장소 포트. */
public interface PostRepository {
  /**
   * 게시글을 저장하고 식별자가 부여된 게시글을 반환한다.
   *
   * @param post 저장할 게시글
   * @return 저장된 게시글
   */
  Post save(Post post);

  /**
   * 삭제되지 않은 게시글을 읽는다.
   *
   * @param id 게시글 식별자
   * @return 게시글
   * @throws com.board.bbs.common.error.BusinessException 게시글이 없으면 POST_NOT_FOUND
   */
  Post load(PostId id);

  /**
   * 조건에 맞는 게시글 요약을 페이지 단위로 조회한다.
   *
   * @param condition 검색 조건
   * @param pageable 페이지 정보
   * @return 게시글 요약 페이지
   */
  Page<PostSummary> search(PostSearchCondition condition, Pageable pageable);

  /**
   * 조회수를 1 증가시킨다.
   *
   * @param postId 게시글 식별자
   */
  void increaseViewCount(PostId postId);

  /**
   * 좋아요 수를 1 증가시킨다.
   *
   * @param postId 게시글 식별자
   */
  void increaseLikeCount(PostId postId);

  /**
   * 좋아요 수를 1 감소시킨다. 0 미만으로 내려가지 않는다.
   *
   * @param postId 게시글 식별자
   */
  void decreaseLikeCount(PostId postId);
}

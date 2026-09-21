package com.board.bbs.comment.application.port.out;

import com.board.bbs.comment.domain.Comment;
import com.board.bbs.post.domain.PostId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 게시글별 댓글 목록 조회 포트. */
public interface ListCommentPort {

  /**
   * 게시글의 삭제되지 않은 댓글을 작성 순서대로 조회한다.
   *
   * @param postId 게시글 식별자
   * @param pageable 페이지 정보
   * @return 댓글 페이지
   */
  Page<Comment> listByPost(PostId postId, Pageable pageable);
}

package com.board.bbs.comment.application.port.in;

import com.board.bbs.comment.domain.Comment;
import com.board.bbs.post.domain.PostId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 댓글 목록 조회 유스케이스. */
public interface ListCommentsUseCase {

  /**
   * 게시글의 댓글 목록을 조회한다.
   *
   * @param postId 게시글 식별자
   * @param pageable 페이지 정보
   * @return 댓글 페이지
   */
  Page<Comment> list(PostId postId, Pageable pageable);
}

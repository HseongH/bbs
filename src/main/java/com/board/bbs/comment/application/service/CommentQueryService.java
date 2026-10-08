package com.board.bbs.comment.application.service;

import com.board.bbs.comment.application.port.out.CommentRepository;
import com.board.bbs.comment.domain.Comment;
import com.board.bbs.post.domain.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 댓글 읽기 유스케이스 구현. */
@Service
@RequiredArgsConstructor
public class CommentQueryService {

  private final CommentRepository commentRepository;

  /**
   * 게시글의 댓글 목록을 조회한다.
   *
   * @param postId 게시글 식별자
   * @param pageable 페이지 정보
   * @return 댓글 페이지
   */
  @Transactional(readOnly = true)
  public Page<Comment> list(PostId postId, Pageable pageable) {
    return commentRepository.listByPost(postId, pageable);
  }
}

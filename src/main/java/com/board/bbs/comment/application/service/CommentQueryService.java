package com.board.bbs.comment.application.service;

import com.board.bbs.comment.application.port.in.ListCommentsUseCase;
import com.board.bbs.comment.application.port.out.ListCommentPort;
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
public class CommentQueryService implements ListCommentsUseCase {

  private final ListCommentPort listCommentPort;

  @Override
  @Transactional(readOnly = true)
  public Page<Comment> list(PostId postId, Pageable pageable) {
    return listCommentPort.listByPost(postId, pageable);
  }
}

package com.board.bbs.comment.application;

import com.board.bbs.comment.domain.Comment;
import java.util.List;

/**
 * 댓글 목록의 한 항목. 원댓글과 그 살아 있는 대댓글을 묶는다.
 *
 * @param root 원댓글. 살아 있는 대댓글이 있으면 삭제된 상태일 수 있다
 * @param replies 살아 있는 대댓글. 작성 순
 */
public record CommentThread(Comment root, List<Comment> replies) {

  /** 대댓글 목록을 바꿀 수 없게 복사한다. */
  public CommentThread {
    replies = List.copyOf(replies);
  }
}

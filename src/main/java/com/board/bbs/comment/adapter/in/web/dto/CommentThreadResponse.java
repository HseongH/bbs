package com.board.bbs.comment.adapter.in.web.dto;

import com.board.bbs.comment.application.CommentThread;
import java.util.List;

/**
 * 댓글 목록의 한 항목. 원댓글과 그 대댓글을 묶는다.
 *
 * @param root 원댓글
 * @param replies 대댓글. 작성 순
 */
public record CommentThreadResponse(CommentResponse root, List<CommentResponse> replies) {

  /**
   * 조회 모델을 응답으로 바꾼다.
   *
   * @param thread 원댓글 묶음
   * @return 응답
   */
  public static CommentThreadResponse from(CommentThread thread) {
    return new CommentThreadResponse(
        CommentResponse.from(thread.root()),
        thread.replies().stream().map(CommentResponse::from).toList());
  }
}

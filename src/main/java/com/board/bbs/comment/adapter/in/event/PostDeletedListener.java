package com.board.bbs.comment.adapter.in.event;

import com.board.bbs.comment.application.service.CommentCommandService;
import com.board.bbs.post.domain.PostDeleted;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 게시글이 삭제되면 그 게시글의 댓글을 삭제한다.
 *
 * <p>게시글 삭제와 같은 트랜잭션에서 처리해야 둘 중 하나만 반영되는 일이 없으므로, 커밋 이후가 아니라 발행 시점에 동기로 받는다.
 */
@Component
@RequiredArgsConstructor
public class PostDeletedListener {

  private final CommentCommandService commentCommandService;

  /**
   * 삭제된 게시글의 댓글을 삭제한다.
   *
   * @param event 게시글 삭제 이벤트
   */
  @EventListener
  public void on(PostDeleted event) {
    commentCommandService.deleteAllOfPost(event.postId(), event.deletedAt());
  }
}

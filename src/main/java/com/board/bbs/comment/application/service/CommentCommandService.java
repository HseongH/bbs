package com.board.bbs.comment.application.service;

import com.board.bbs.comment.application.port.in.DeleteCommentUseCase;
import com.board.bbs.comment.application.port.in.UpdateCommentUseCase;
import com.board.bbs.comment.application.port.in.WriteCommentUseCase;
import com.board.bbs.comment.application.port.out.LoadCommentPort;
import com.board.bbs.comment.application.port.out.SaveCommentPort;
import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentBody;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.port.out.LoadPostPort;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 댓글 쓰기 유스케이스 구현. 깊이 판단과 권한 검사는 도메인이 한다. */
@Service
@RequiredArgsConstructor
public class CommentCommandService
    implements WriteCommentUseCase, UpdateCommentUseCase, DeleteCommentUseCase {

  private final SaveCommentPort saveCommentPort;
  private final LoadCommentPort loadCommentPort;
  private final LoadPostPort loadPostPort;

  @Override
  @Transactional
  public CommentId write(
      PostId postId, MemberId author, String body, @Nullable Long parentCommentId) {

    loadPostPort.load(postId);

    CommentId parentId = parentCommentId == null ? null : new CommentId(parentCommentId);
    int parentDepth = parentId == null ? 0 : loadCommentPort.load(parentId).getDepth();

    Comment saved =
        saveCommentPort.save(
            Comment.write(postId, author, new CommentBody(body), parentId, parentDepth));
    return Objects.requireNonNull(saved.getId(), "저장된 댓글은 식별자를 가진다.");
  }

  @Override
  @Transactional
  public void update(CommentId id, MemberId requester, String body) {
    Comment comment = loadCommentPort.load(id);
    comment.updateBy(requester, new CommentBody(body));
    saveCommentPort.save(comment);
  }

  @Override
  @Transactional
  public void delete(CommentId id, MemberId requester, boolean admin) {
    Comment comment = loadCommentPort.load(id);
    comment.deleteBy(requester, admin, Instant.now());
    saveCommentPort.save(comment);
  }
}

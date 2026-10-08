package com.board.bbs.comment.application.service;

import com.board.bbs.comment.application.port.out.CommentRepository;
import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentBody;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.service.PostQueryService;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 댓글 쓰기 유스케이스 구현. 깊이 판단과 권한 검사는 도메인이 한다. */
@Service
public class CommentCommandService {

  private final CommentRepository commentRepository;
  private final PostQueryService postQueryService;

  CommentCommandService(CommentRepository commentRepository, PostQueryService postQueryService) {
    this.commentRepository = commentRepository;
    this.postQueryService = postQueryService;
  }

  /**
   * 댓글 또는 답글을 작성한다.
   *
   * @param postId 대상 게시글 식별자
   * @param author 작성자 식별자
   * @param body 본문
   * @param parentCommentId 부모 댓글 식별자. 원댓글이면 null
   * @return 작성된 댓글의 식별자
   */
  @Transactional
  public CommentId write(
      PostId postId, MemberId author, String body, @Nullable Long parentCommentId) {

    postQueryService.getById(postId);

    Comment parent =
        parentCommentId == null ? null : commentRepository.load(new CommentId(parentCommentId));

    Comment saved =
        commentRepository.save(Comment.write(postId, author, new CommentBody(body), parent));
    return Objects.requireNonNull(saved.getId(), "저장된 댓글은 식별자를 가진다.");
  }

  /**
   * 댓글을 수정한다.
   *
   * @param id 댓글 식별자
   * @param requester 요청한 회원 식별자
   * @param body 새 본문
   */
  @Transactional
  public void update(CommentId id, MemberId requester, String body) {
    Comment comment = commentRepository.load(id);
    comment.updateBy(requester, new CommentBody(body));
    commentRepository.save(comment);
  }

  /**
   * 댓글을 삭제한다.
   *
   * @param id 댓글 식별자
   * @param requester 요청한 회원 식별자
   * @param admin 관리자 여부
   */
  @Transactional
  public void delete(CommentId id, MemberId requester, boolean admin) {
    Comment comment = commentRepository.load(id);
    comment.deleteBy(requester, admin, Instant.now());
    commentRepository.save(comment);
  }

  /**
   * 게시글에 달린 댓글을 모두 삭제한다.
   *
   * @param postId 게시글 식별자
   * @param deletedAt 삭제 시각
   */
  @Transactional
  public void deleteAllOfPost(PostId postId, Instant deletedAt) {
    commentRepository.softDeleteAllByPost(postId, deletedAt);
  }
}

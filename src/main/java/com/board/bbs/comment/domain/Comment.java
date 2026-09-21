package com.board.bbs.comment.domain;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** 댓글 애그리게이트 루트. 게시글은 식별자로만 참조한다. */
public class Comment {

  private static final int MAX_DEPTH = 1;

  @Nullable private final CommentId id;
  private final PostId postId;
  private final MemberId authorId;
  private CommentBody body;
  @Nullable private final CommentId parentId;
  private final int depth;
  @Nullable private final Instant createdAt;
  @Nullable private Instant deletedAt;

  private Comment(
      @Nullable CommentId id,
      PostId postId,
      MemberId authorId,
      CommentBody body,
      @Nullable CommentId parentId,
      int depth,
      @Nullable Instant createdAt,
      @Nullable Instant deletedAt) {

    this.id = id;
    this.postId = Objects.requireNonNull(postId, "게시글 식별자는 필수입니다.");
    this.authorId = Objects.requireNonNull(authorId, "작성자는 필수입니다.");
    this.body = Objects.requireNonNull(body, "본문은 필수입니다.");
    this.parentId = parentId;
    this.depth = depth;
    this.createdAt = createdAt;
    this.deletedAt = deletedAt;
  }

  /**
   * 댓글 또는 답글을 작성한다. 깊이는 부모의 깊이에서 결정되며 한 단계까지만 허용한다.
   *
   * @param postId 대상 게시글 식별자
   * @param authorId 작성자 식별자
   * @param body 본문
   * @param parentId 부모 댓글 식별자. 원댓글이면 null
   * @param parentDepth 부모 댓글의 깊이. 원댓글이면 무시된다
   * @return 작성된 댓글
   * @throws BusinessException 허용 깊이를 넘는 경우
   */
  public static Comment write(
      PostId postId,
      MemberId authorId,
      CommentBody body,
      @Nullable CommentId parentId,
      int parentDepth) {

    int depth = parentId == null ? 0 : parentDepth + 1;
    if (depth > MAX_DEPTH) {
      throw new BusinessException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
    }
    return new Comment(null, postId, authorId, body, parentId, depth, null, null);
  }

  /**
   * 영속화된 댓글을 복원한다. 어댑터에서만 사용한다.
   *
   * @param id 댓글 식별자
   * @param postId 게시글 식별자
   * @param authorId 작성자 식별자
   * @param body 본문
   * @param parentId 부모 댓글 식별자
   * @param depth 깊이
   * @param createdAt 작성 시각
   * @param deletedAt 삭제 시각. 삭제되지 않았으면 null
   * @return 복원된 댓글
   */
  public static Comment restore(
      CommentId id,
      PostId postId,
      MemberId authorId,
      CommentBody body,
      @Nullable CommentId parentId,
      int depth,
      Instant createdAt,
      @Nullable Instant deletedAt) {

    return new Comment(
        Objects.requireNonNull(id), postId, authorId, body, parentId, depth, createdAt, deletedAt);
  }

  /**
   * 본문을 수정한다. 작성자만 수정할 수 있다.
   *
   * @param requester 요청한 회원 식별자
   * @param newBody 새 본문
   * @throws BusinessException 작성자가 아니거나 이미 삭제된 경우
   */
  public void updateBy(MemberId requester, CommentBody newBody) {
    requireNotDeleted();
    requireAuthor(requester);
    this.body = Objects.requireNonNull(newBody);
  }

  /**
   * 댓글을 삭제한다. 작성자 또는 관리자만 삭제할 수 있다.
   *
   * @param requester 요청한 회원 식별자
   * @param admin 관리자 여부
   * @param now 삭제 시각
   * @throws BusinessException 권한이 없거나 이미 삭제된 경우
   */
  public void deleteBy(MemberId requester, boolean admin, Instant now) {
    requireNotDeleted();
    if (!admin) {
      requireAuthor(requester);
    }
    this.deletedAt = Objects.requireNonNull(now);
  }

  private void requireAuthor(MemberId requester) {
    if (!authorId.equals(requester)) {
      throw new BusinessException(ErrorCode.ACCESS_DENIED);
    }
  }

  private void requireNotDeleted() {
    if (isDeleted()) {
      throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "삭제된 댓글입니다.");
    }
  }

  /**
   * 삭제 여부를 반환한다.
   *
   * @return 삭제되었으면 true
   */
  public boolean isDeleted() {
    return deletedAt != null;
  }

  @Nullable
  public CommentId getId() {
    return id;
  }

  public PostId getPostId() {
    return postId;
  }

  public MemberId getAuthorId() {
    return authorId;
  }

  public CommentBody getBody() {
    return body;
  }

  @Nullable
  public CommentId getParentId() {
    return parentId;
  }

  public int getDepth() {
    return depth;
  }

  @Nullable
  public Instant getCreatedAt() {
    return createdAt;
  }

  @Nullable
  public Instant getDeletedAt() {
    return deletedAt;
  }
}

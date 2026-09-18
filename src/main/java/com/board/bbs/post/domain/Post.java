package com.board.bbs.post.domain;

import com.board.bbs.member.domain.MemberId;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** 게시글 애그리게이트 루트. */
public class Post {

  @Nullable private final PostId id;
  private Title title;
  private Content content;
  private final MemberId authorId;
  @Nullable private final Instant createdAt;
  @Nullable private Instant deletedAt;
  private final long viewCount;
  private final long likeCount;

  private Post(
      @Nullable PostId id,
      Title title,
      Content content,
      MemberId authorId,
      @Nullable Instant createdAt,
      @Nullable Instant deletedAt,
      long viewCount,
      long likeCount) {

    this.id = id;
    this.title = Objects.requireNonNull(title, "제목은 필수입니다.");
    this.content = Objects.requireNonNull(content, "본문은 필수입니다.");
    this.authorId = Objects.requireNonNull(authorId, "작성자는 필수입니다.");
    this.createdAt = createdAt;
    this.deletedAt = deletedAt;
    this.viewCount = viewCount;
    this.likeCount = likeCount;
  }

  /**
   * 새 게시글을 작성한다. 아직 식별자가 없다.
   *
   * @param title 제목
   * @param content 본문
   * @param authorId 작성자 식별자
   * @return 작성된 게시글
   */
  public static Post write(Title title, Content content, MemberId authorId) {
    return new Post(null, title, content, authorId, null, null, 0L, 0L);
  }

  /**
   * 영속화된 게시글을 복원한다. 어댑터에서만 사용한다.
   *
   * @param id 게시글 식별자
   * @param title 제목
   * @param content 본문
   * @param authorId 작성자 식별자
   * @param createdAt 작성 시각
   * @param deletedAt 삭제 시각. 삭제되지 않았으면 null
   * @param viewCount 조회수
   * @param likeCount 좋아요 수
   * @return 복원된 게시글
   */
  public static Post restore(
      PostId id,
      Title title,
      Content content,
      MemberId authorId,
      Instant createdAt,
      @Nullable Instant deletedAt,
      long viewCount,
      long likeCount) {

    return new Post(
        Objects.requireNonNull(id),
        title,
        content,
        authorId,
        createdAt,
        deletedAt,
        viewCount,
        likeCount);
  }

  /**
   * 삭제 여부를 반환한다.
   *
   * @return 삭제되었으면 true
   */
  public boolean isDeleted() {
    return deletedAt != null;
  }

  @Nullable public PostId getId() {
    return id;
  }

  public Title getTitle() {
    return title;
  }

  public Content getContent() {
    return content;
  }

  public MemberId getAuthorId() {
    return authorId;
  }

  @Nullable public Instant getCreatedAt() {
    return createdAt;
  }

  @Nullable public Instant getDeletedAt() {
    return deletedAt;
  }

  public long getViewCount() {
    return viewCount;
  }

  public long getLikeCount() {
    return likeCount;
  }
}

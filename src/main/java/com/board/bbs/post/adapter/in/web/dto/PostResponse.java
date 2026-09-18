package com.board.bbs.post.adapter.in.web.dto;

import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import java.util.Objects;

/**
 * 게시글 상세 응답.
 *
 * @param id 게시글 식별자
 * @param title 제목
 * @param content 본문
 * @param authorId 작성자 식별자
 * @param viewCount 조회수
 * @param likeCount 좋아요 수
 * @param createdAt 작성 시각
 */
public record PostResponse(
    Long id,
    String title,
    String content,
    Long authorId,
    long viewCount,
    long likeCount,
    Instant createdAt) {

  /**
   * 도메인 게시글을 응답으로 변환한다.
   *
   * @param post 게시글
   * @return 게시글 응답
   */
  public static PostResponse from(Post post) {
    PostId id = Objects.requireNonNull(post.getId(), "저장된 게시글은 식별자를 가진다.");
    return new PostResponse(
        id.value(),
        post.getTitle().value(),
        post.getContent().value(),
        post.getAuthorId().value(),
        post.getViewCount(),
        post.getLikeCount(),
        Objects.requireNonNull(post.getCreatedAt(), "저장된 게시글은 작성 시각을 가진다."));
  }
}

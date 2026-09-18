package com.board.bbs.post.adapter.out.persistence;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.domain.Content;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.post.domain.Title;
import java.util.Objects;

/** 도메인과 영속성 모델 사이의 유일한 변환 지점. */
final class PostMapper {

  private PostMapper() {}

  static Post toDomain(PostJpaEntity entity) {
    return Post.restore(
        new PostId(Objects.requireNonNull(entity.getId(), "저장된 게시글은 식별자를 가진다.")),
        new Title(entity.getTitle()),
        new Content(entity.getContent()),
        new MemberId(entity.getAuthorId()),
        Objects.requireNonNull(entity.getCreatedAt(), "저장된 게시글은 작성 시각을 가진다."),
        entity.getDeletedAt(),
        entity.getViewCount(),
        entity.getLikeCount());
  }

  static PostJpaEntity toEntity(Post post) {
    PostId id = post.getId();
    return new PostJpaEntity(
        id == null ? null : id.value(),
        post.getTitle().value(),
        post.getContent().value(),
        post.getAuthorId().value(),
        post.getViewCount(),
        post.getLikeCount(),
        post.getCreatedAt(),
        post.getDeletedAt());
  }
}

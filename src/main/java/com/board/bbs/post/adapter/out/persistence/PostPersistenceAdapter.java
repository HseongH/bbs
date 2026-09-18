package com.board.bbs.post.adapter.out.persistence;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.post.application.port.out.LoadPostPort;
import com.board.bbs.post.application.port.out.SavePostPort;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 게시글 영속성 어댑터. */
@Component
@RequiredArgsConstructor
public class PostPersistenceAdapter implements SavePostPort, LoadPostPort {

  private final PostJpaRepository repository;

  @Override
  public Post save(Post post) {
    return PostMapper.toDomain(repository.save(PostMapper.toEntity(post)));
  }

  @Override
  public Post load(PostId id) {
    return repository
        .findByIdAndDeletedAtIsNull(id.value())
        .map(PostMapper::toDomain)
        .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
  }
}

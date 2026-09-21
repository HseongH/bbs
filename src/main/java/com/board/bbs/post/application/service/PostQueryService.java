package com.board.bbs.post.application.service;

import com.board.bbs.post.application.PostSearchCondition;
import com.board.bbs.post.application.PostSummary;
import com.board.bbs.post.application.port.in.GetPostUseCase;
import com.board.bbs.post.application.port.in.SearchPostsUseCase;
import com.board.bbs.post.application.port.out.LoadPostPort;
import com.board.bbs.post.application.port.out.SearchPostPort;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 읽기 유스케이스 구현. */
@Service
@RequiredArgsConstructor
public class PostQueryService implements GetPostUseCase, SearchPostsUseCase {

  private final LoadPostPort loadPostPort;
  private final SearchPostPort searchPostPort;

  @Override
  @Transactional(readOnly = true)
  public Post getById(PostId id) {
    return loadPostPort.load(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PostSummary> search(PostSearchCondition condition, Pageable pageable) {
    return searchPostPort.search(condition, pageable);
  }
}

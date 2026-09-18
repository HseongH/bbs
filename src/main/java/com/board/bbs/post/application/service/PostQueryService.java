package com.board.bbs.post.application.service;

import com.board.bbs.post.application.port.in.GetPostUseCase;
import com.board.bbs.post.application.port.out.LoadPostPort;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 읽기 유스케이스 구현. */
@Service
@RequiredArgsConstructor
public class PostQueryService implements GetPostUseCase {

  private final LoadPostPort loadPostPort;

  @Override
  @Transactional(readOnly = true)
  public Post getById(PostId id) {
    return loadPostPort.load(id);
  }
}

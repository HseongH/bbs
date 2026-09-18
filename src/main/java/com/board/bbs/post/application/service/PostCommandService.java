package com.board.bbs.post.application.service;

import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.port.in.CreatePostUseCase;
import com.board.bbs.post.application.port.out.SavePostPort;
import com.board.bbs.post.domain.Content;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.post.domain.Title;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 쓰기 유스케이스 구현. */
@Service
@RequiredArgsConstructor
public class PostCommandService implements CreatePostUseCase {

  private final SavePostPort savePostPort;

  @Override
  @Transactional
  public PostId create(MemberId author, String title, String content) {
    Post saved = savePostPort.save(Post.write(new Title(title), new Content(content), author));
    return Objects.requireNonNull(saved.getId(), "저장된 게시글은 식별자를 가진다.");
  }
}

package com.board.bbs.post.application.service;

import com.board.bbs.comment.application.port.out.DeleteCommentsByPostPort;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.port.in.CreatePostUseCase;
import com.board.bbs.post.application.port.in.DeletePostUseCase;
import com.board.bbs.post.application.port.in.UpdatePostUseCase;
import com.board.bbs.post.application.port.out.LoadPostPort;
import com.board.bbs.post.application.port.out.SavePostPort;
import com.board.bbs.post.domain.Content;
import com.board.bbs.post.domain.Post;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.post.domain.Title;
import java.time.Instant;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 게시글 쓰기 유스케이스 구현. 권한 판단은 도메인이 하고 여기서는 조율만 한다. */
@Service
@RequiredArgsConstructor
public class PostCommandService implements CreatePostUseCase, UpdatePostUseCase, DeletePostUseCase {

  private final SavePostPort savePostPort;
  private final LoadPostPort loadPostPort;
  private final DeleteCommentsByPostPort deleteCommentsByPostPort;

  @Override
  @Transactional
  public PostId create(MemberId author, String title, String content) {
    Post saved = savePostPort.save(Post.write(new Title(title), new Content(content), author));
    return Objects.requireNonNull(saved.getId(), "저장된 게시글은 식별자를 가진다.");
  }

  @Override
  @Transactional
  public void update(PostId id, MemberId requester, String title, String content) {
    Post post = loadPostPort.load(id);
    post.updateBy(requester, new Title(title), new Content(content));
    savePostPort.save(post);
  }

  @Override
  @Transactional
  public void delete(PostId id, MemberId requester, boolean admin) {
    Post post = loadPostPort.load(id);
    Instant now = Instant.now();
    post.deleteBy(requester, admin, now);
    savePostPort.save(post);
    deleteCommentsByPostPort.softDeleteAllByPost(id, now);
  }
}

package com.board.bbs.comment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.board.bbs.comment.application.CommentThread;
import com.board.bbs.comment.application.port.out.CommentRepository;
import com.board.bbs.comment.domain.Comment;
import com.board.bbs.comment.domain.CommentBody;
import com.board.bbs.comment.domain.CommentId;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.service.PostQueryService;
import com.board.bbs.post.domain.PostId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class CommentQueryServiceTest {

  private static final PostId POST = new PostId(1L);
  private static final MemberId AUTHOR = new MemberId(1L);

  /**
   * 원댓글 조회와 대댓글 조회는 서로 다른 쿼리라, 그 사이에 마지막 대댓글이 삭제될 수 있다. 그러면 삭제된 원댓글만 남은 묶음이 생기는데, 이런 묶음은 목록에 나오면 안
   * 된다 (CMT-FR-009).
   */
  @Test
  void 대댓글이_사라진_삭제된_원댓글은_목록에서_뺀다() {
    Comment deletedRoot = 원댓글(1L, Instant.EPOCH);
    Comment liveRoot = 원댓글(2L, null);
    CommentQueryService service =
        new CommentQueryService(
            new FixedCommentRepository(List.of(deletedRoot, liveRoot), List.of()),
            mock(PostQueryService.class));

    Page<CommentThread> page = service.list(POST, PageRequest.of(0, 20));

    assertThat(page.getContent())
        .extracting(thread -> thread.root().getId())
        .containsExactly(new CommentId(2L));
  }

  private static Comment 원댓글(long id, Instant deletedAt) {
    return Comment.restore(
        new CommentId(id), POST, AUTHOR, new CommentBody("원댓글"), null, 0, Instant.EPOCH, deletedAt);
  }

  /** 원댓글 조회 결과와 대댓글 조회 결과를 정해 둔 저장소. */
  private record FixedCommentRepository(List<Comment> roots, List<Comment> replies)
      implements CommentRepository {

    @Override
    public Page<Comment> listRoots(PostId postId, Pageable pageable) {
      return new PageImpl<>(roots, pageable, roots.size());
    }

    @Override
    public List<Comment> listRepliesOf(List<CommentId> rootIds) {
      return replies;
    }

    @Override
    public Comment save(Comment comment) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Comment load(CommentId id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void softDeleteAllByPost(PostId postId, Instant now) {
      throw new UnsupportedOperationException();
    }
  }
}

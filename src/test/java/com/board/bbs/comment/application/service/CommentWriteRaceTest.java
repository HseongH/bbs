package com.board.bbs.comment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.board.bbs.common.error.BusinessException;
import com.board.bbs.common.error.ErrorCode;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.application.service.PostCommandService;
import com.board.bbs.post.domain.PostId;
import com.board.bbs.support.IntegrationTestBase;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** 게시글 삭제가 커밋되기 전에 시작된 댓글 작성이 삭제된 게시글에 살아 있는 댓글을 남기지 않는지 검증한다. */
class CommentWriteRaceTest extends IntegrationTestBase {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  @Autowired private PostCommandService postCommandService;
  @Autowired private CommentCommandService commentCommandService;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  private MemberId author;

  @BeforeEach
  void 데이터를_준비한다() {
    jdbcTemplate.update("DELETE FROM post_like");
    jdbcTemplate.update("DELETE FROM comment");
    jdbcTemplate.update("DELETE FROM post");
    jdbcTemplate.update("DELETE FROM member");
    jdbcTemplate.update(
        "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
            + " VALUES ('sub-race', '경쟁테스터', 'race@example.com', now(), now())");
    author =
        new MemberId(
            Objects.requireNonNull(
                jdbcTemplate.queryForObject(
                    "SELECT id FROM member WHERE subject = 'sub-race'", Long.class)));
  }

  @Test
  void 삭제가_커밋되기_전에_시작한_댓글_작성은_실패하고_살아_있는_댓글이_남지_않는다() throws Exception {
    PostId postId = postCommandService.create(author, "삭제될 글", "본문입니다.");
    CountDownLatch deletedButNotCommitted = new CountDownLatch(1);
    CountDownLatch writeSettled = new CountDownLatch(1);
    AtomicInteger deletionPid = new AtomicInteger();

    Throwable failure;
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final CompletableFuture<Void> deletion =
          CompletableFuture.runAsync(
              () ->
                  transactionTemplate.executeWithoutResult(
                      status -> {
                        postCommandService.delete(postId, author, false);
                        deletionPid.set(backendPid());
                        deletedButNotCommitted.countDown();
                        await(writeSettled);
                      }),
              executor);

      await(deletedButNotCommitted);
      CompletableFuture<Void> writing =
          CompletableFuture.runAsync(
              () -> commentCommandService.write(postId, author, "늦은 댓글", null), executor);
      // 수정 전에는 작성이 바로 끝나고, 수정 후에는 삭제가 커밋될 때까지 잠금을 기다린다.
      waitUntilDoneOrBlockedBy(writing, deletionPid.get());
      writeSettled.countDown();

      deletion.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
      failure = catchThrowable(() -> writing.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS));
    }

    assertThat(failure).hasCauseInstanceOf(BusinessException.class);
    assertThat(((BusinessException) failure.getCause()).getErrorCode())
        .isEqualTo(ErrorCode.POST_NOT_FOUND);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM comment WHERE post_id = ? AND deleted_at IS NULL",
                Long.class,
                postId.value()))
        .isZero();
  }

  private void waitUntilDoneOrBlockedBy(CompletableFuture<?> future, int blockerPid)
      throws InterruptedException {
    Instant deadline = Instant.now().plus(TIMEOUT);
    while (!future.isDone() && !someoneIsBlockedBy(blockerPid)) {
      if (Instant.now().isAfter(deadline)) {
        throw new AssertionError("댓글 작성이 끝나지도, 잠금을 기다리지도 않았습니다.");
      }
      Thread.sleep(20);
    }
  }

  /** 삭제 트랜잭션이 잡은 잠금을 기다리는 세션만 센다. 관계없는 잠금 대기로 판정이 앞당겨지지 않게 한다. */
  private boolean someoneIsBlockedBy(int blockerPid) {
    Long waiting =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_stat_activity WHERE ? = ANY(pg_blocking_pids(pid))",
            Long.class,
            blockerPid);
    return waiting != null && waiting > 0;
  }

  /** 트랜잭션 안에서 부르면 그 트랜잭션이 쓰는 연결의 서버 프로세스 번호를 돌려준다. */
  private int backendPid() {
    return Objects.requireNonNull(
        jdbcTemplate.queryForObject("SELECT pg_backend_pid()", Integer.class));
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
        throw new AssertionError("상대 트랜잭션을 기다리다 시간이 초과되었습니다.");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    }
  }
}

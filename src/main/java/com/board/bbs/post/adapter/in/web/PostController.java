package com.board.bbs.post.adapter.in.web;

import com.board.bbs.common.security.CurrentMember;
import com.board.bbs.member.domain.MemberId;
import com.board.bbs.post.adapter.in.web.dto.CreatePostRequest;
import com.board.bbs.post.adapter.in.web.dto.PostResponse;
import com.board.bbs.post.application.port.in.CreatePostUseCase;
import com.board.bbs.post.application.port.in.GetPostUseCase;
import com.board.bbs.post.domain.PostId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 게시글 API. */
@Tag(name = "게시글")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

  private final CreatePostUseCase createPostUseCase;
  private final GetPostUseCase getPostUseCase;

  /**
   * 게시글을 작성한다.
   *
   * @param author 현재 로그인 회원 식별자
   * @param request 작성 요청
   * @return 생성된 게시글의 위치를 담은 201 응답
   */
  @Operation(summary = "게시글 작성")
  @PostMapping
  public ResponseEntity<Void> create(
      @CurrentMember MemberId author, @Valid @RequestBody CreatePostRequest request) {

    PostId postId = createPostUseCase.create(author, request.title(), request.content());
    return ResponseEntity.created(URI.create("/api/posts/" + postId.value())).build();
  }

  /**
   * 게시글을 조회한다.
   *
   * @param id 게시글 식별자
   * @return 게시글 상세
   */
  @Operation(summary = "게시글 단건 조회")
  @GetMapping("/{id}")
  public PostResponse get(@PathVariable Long id) {
    return PostResponse.from(getPostUseCase.getById(new PostId(id)));
  }
}

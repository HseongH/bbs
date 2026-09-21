package com.board.bbs.comment.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/**
 * 댓글 작성 요청.
 *
 * @param body 본문
 * @param parentCommentId 부모 댓글 식별자. 원댓글이면 생략한다
 */
public record WriteCommentRequest(
    @NotBlank(message = "댓글 본문은 필수입니다.") @Size(max = 1000, message = "댓글 본문은 1000자를 넘을 수 없습니다.") String body,
    @Nullable Long parentCommentId) {}

package com.board.bbs.comment.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 수정 요청.
 *
 * @param body 새 본문
 */
public record UpdateCommentRequest(
    @NotBlank(message = "댓글 본문은 필수입니다.") @Size(max = 1000, message = "댓글 본문은 1000자를 넘을 수 없습니다.") String body) {}

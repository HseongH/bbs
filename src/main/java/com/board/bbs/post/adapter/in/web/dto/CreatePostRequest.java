package com.board.bbs.post.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 게시글 작성 요청.
 *
 * @param title 제목
 * @param content 본문
 */
public record CreatePostRequest(
    @NotBlank(message = "제목은 필수입니다.") @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.") String title,
    @NotBlank(message = "본문은 필수입니다.") @Size(max = 10000, message = "본문은 10000자를 넘을 수 없습니다.") String content) {}

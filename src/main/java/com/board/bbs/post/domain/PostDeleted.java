package com.board.bbs.post.domain;

import java.time.Instant;

/**
 * 게시글이 삭제되었음을 알린다. 게시글에 딸린 데이터를 가진 기능은 이 이벤트를 받아 정리한다.
 *
 * @param postId 삭제된 게시글 식별자
 * @param deletedAt 삭제 시각
 */
public record PostDeleted(PostId postId, Instant deletedAt) {}

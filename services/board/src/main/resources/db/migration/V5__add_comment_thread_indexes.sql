-- 댓글 목록은 원댓글 한 페이지와 그 대댓글을 따로 조회한다.
-- 원댓글 조회는 삭제된 원댓글도 포함해야 하므로 deleted_at 조건이 없는 부분 인덱스를 쓴다.
CREATE INDEX idx_comment_roots_by_post
    ON comment (post_id, created_at, id)
    WHERE depth = 0;

-- 대댓글 조회와, 원댓글에 살아 있는 대댓글이 있는지 확인하는 EXISTS 조건이 함께 쓴다.
CREATE INDEX idx_comment_live_replies
    ON comment (parent_comment_id, created_at, id)
    WHERE deleted_at IS NULL;

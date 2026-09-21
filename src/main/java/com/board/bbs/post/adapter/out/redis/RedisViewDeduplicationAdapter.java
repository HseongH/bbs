package com.board.bbs.post.adapter.out.redis;

import com.board.bbs.post.application.port.out.ViewDeduplicationPort;
import com.board.bbs.post.domain.PostId;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 조회 이력을 Redis에 남겨 같은 조회자의 중복 집계를 막는다. */
@Component
@RequiredArgsConstructor
public class RedisViewDeduplicationAdapter implements ViewDeduplicationPort {

  private static final Duration TTL = Duration.ofHours(24);

  private final StringRedisTemplate redisTemplate;

  /** SETNX는 조회 여부 확인과 기록을 한 번에 처리하므로 경합에서도 한 번만 true를 돌려준다. */
  @Override
  public boolean markViewed(PostId postId, String viewerKey) {
    String key = "post:view:%d:%s".formatted(postId.value(), viewerKey);
    return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", TTL));
  }
}

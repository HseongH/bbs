package com.board.bbs.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** QueryDSL 질의 팩터리를 등록한다. */
@Configuration
public class QuerydslConfig {

  /**
   * QueryDSL 질의 팩터리를 만든다.
   *
   * @param entityManager 영속성 컨텍스트
   * @return 질의 팩터리
   */
  @Bean
  JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
    return new JPAQueryFactory(entityManager);
  }
}

package com.board.bbs.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** 엔티티 생성·수정 시각 자동 기록을 활성화한다. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}

package com.board.bbs.member.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

  Optional<MemberJpaEntity> findBySubject(String subject);
}

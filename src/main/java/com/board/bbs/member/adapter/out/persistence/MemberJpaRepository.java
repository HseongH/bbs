package com.board.bbs.member.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MemberJpaRepository extends JpaRepository<MemberJpaEntity, Long> {

  Optional<MemberJpaEntity> findBySubject(String subject);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      value =
          "INSERT INTO member (subject, nickname, email, created_at, updated_at)"
              + " VALUES (:subject, :nickname, :email, now(), now())"
              + " ON CONFLICT (subject) DO NOTHING",
      nativeQuery = true)
  int insertIfAbsent(
      @Param("subject") String subject,
      @Param("nickname") String nickname,
      @Param("email") String email);
}

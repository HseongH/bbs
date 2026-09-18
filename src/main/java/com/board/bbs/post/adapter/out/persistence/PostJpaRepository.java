package com.board.bbs.post.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long> {

  Optional<PostJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}

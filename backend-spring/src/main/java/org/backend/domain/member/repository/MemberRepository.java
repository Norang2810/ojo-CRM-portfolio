package org.backend.domain.member.repository;

import org.backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    // 고객별 기본 정보 조회
    @EntityGraph(attributePaths = "consent")
    Optional<Member> findWithConsentById(Long id);

    // 배치를 위해
    @Query("SELECT m.id FROM Member m")
    List<Long> findAllIds();

    // 채널 멤버 추가 시 존재하는 회원 ID 한 번에 확인 (IN 쿼리)
    @Query("SELECT m.id FROM Member m WHERE m.id IN :ids")
    Set<Long> findExistingIds(@Param("ids") List<Long> ids);
}
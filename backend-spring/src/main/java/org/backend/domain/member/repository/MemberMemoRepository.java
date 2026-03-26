package org.backend.domain.member.repository;

import org.backend.domain.member.entity.MemberMemo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberMemoRepository extends JpaRepository<MemberMemo, Long> {

    // 상담사 + 고객 조합으로 메모 조회
    Optional<MemberMemo> findByAdminIdAndMemberId(Long adminId, Long memberId);
}
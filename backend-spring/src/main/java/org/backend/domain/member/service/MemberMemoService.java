package org.backend.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.backend.domain.admin.repository.AdminRepository;
import org.backend.domain.member.dto.MemberMemoResponse;
import org.backend.domain.member.entity.MemberMemo;
import org.backend.domain.member.repository.MemberMemoRepository;
import org.backend.domain.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberMemoService {

    private final MemberMemoRepository memberMemoRepository;
    private final MemberRepository memberRepository;
    private final AdminRepository adminRepository;

    @Transactional
    public Long saveOrUpdateMemo(Long adminId, Long memberId, String content, String summary) {
        if (!adminRepository.existsById(adminId)) {
            throw new IllegalArgumentException("존재하지 않는 상담사입니다. ID: " + adminId);
        }
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("존재하지 않는 회원입니다. ID: " + memberId);
        }

        return memberMemoRepository.findByAdminIdAndMemberId(adminId, memberId)
                .map(memo -> {
                    memo.updateContent(content, summary);
                    return memo.getId();
                })
                .orElseGet(() -> {
                    MemberMemo newMemo = MemberMemo.builder()
                            .adminId(adminId)
                            .memberId(memberId)
                            .content(content)
                            .summary(summary)
                            .build();
                    return memberMemoRepository.save(newMemo).getId();
                });
    }

    public MemberMemoResponse getMemo(Long adminId, Long memberId) {
        MemberMemo memo = memberMemoRepository.findByAdminIdAndMemberId(adminId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("등록된 메모가 없습니다."));
        return MemberMemoResponse.from(memo);
    }

    @Transactional
    public void deleteMemo(Long adminId, Long memberId) {
        MemberMemo memo = memberMemoRepository.findByAdminIdAndMemberId(adminId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 메모가 존재하지 않습니다."));
        memberMemoRepository.delete(memo);
    }
}
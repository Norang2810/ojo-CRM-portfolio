package org.backend.domain.member.controller;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.domain.member.dto.MemberMemoResponse;
import org.backend.domain.member.dto.MemoRequest;
import org.backend.domain.member.service.MemberMemoService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member-memos")
public class MemberMemoController {

    private final MemberMemoService memberMemoService;

    // JWT 토큰에서 adminId 추출
    private Long getCurrentAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        return Long.parseLong((String) authentication.getPrincipal());
    }

    // 상담사별 고객 메모 저장/수정
    @PostMapping("/{memberId}")
    public CommonResponse<Long> saveMemo(
            @PathVariable Long memberId,
            @RequestBody MemoRequest request) {
        try {
            Long adminId = getCurrentAdminId();
            Long memoId = memberMemoService.saveOrUpdateMemo(adminId, memberId, request.getContent(), request.getSummary());
            return CommonResponse.success(memoId, "메모가 저장되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return CommonResponse.fail(e.getMessage());
        }
    }

    // 상담사별 고객 메모 조회
    @GetMapping("/{memberId}")
    public CommonResponse<MemberMemoResponse> getMemo(@PathVariable Long memberId) {
        try {
            Long adminId = getCurrentAdminId();
            MemberMemoResponse memo = memberMemoService.getMemo(adminId, memberId);
            return CommonResponse.success(memo, "메모 조회 성공");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return CommonResponse.fail(e.getMessage());
        }
    }

    // 상담사별 고객 메모 삭제
    @DeleteMapping("/{memberId}")
    public CommonResponse<Void> deleteMemo(@PathVariable Long memberId) {
        try {
            Long adminId = getCurrentAdminId();
            memberMemoService.deleteMemo(adminId, memberId);
            return CommonResponse.success(null, "메모가 삭제되었습니다.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return CommonResponse.fail(e.getMessage());
        }
    }
}
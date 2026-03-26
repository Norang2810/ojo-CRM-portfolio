package org.backend.domain.member.dto;

import lombok.Builder;
import lombok.Getter;
import org.backend.domain.member.entity.MemberMemo;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberMemoResponse {

    private Long id;
    private Long memberId;
    private Long adminId;
    private String content;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberMemoResponse from(MemberMemo memo) {
        return MemberMemoResponse.builder()
                .id(memo.getId())
                .memberId(memo.getMemberId())
                .adminId(memo.getAdminId())
                .content(memo.getContent())
                .summary(memo.getSummary())
                .createdAt(memo.getCreatedAt())
                .updatedAt(memo.getUpdatedAt())
                .build();
    }
}
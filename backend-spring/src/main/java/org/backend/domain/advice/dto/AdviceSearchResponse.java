package org.backend.domain.advice.dto;

import lombok.Builder;
import lombok.Getter;
import org.backend.domain.advice.document.AdviceDocument;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdviceSearchResponse {

    private Long id;
    private Long memberId;
    private String adminName;
    private String category;
    private String adviceContent;
    private LocalDateTime createdAt;


    public static AdviceSearchResponse from(AdviceDocument document){
        return AdviceSearchResponse.builder()
                .id(document.getId())
                .memberId(document.getMemberId())
                .adminName(document.getAdminName())
                .category(document.getCategory())
                .adviceContent(document.getAdviceContent())
                .createdAt(document.getCreatedAt())
                .build();
    }





}

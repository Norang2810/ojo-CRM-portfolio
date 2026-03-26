package org.backend.domain.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SegmentMemberDto {
    private Long memberId;
    private String name;
    private Integer rfmScore;
    private Double churnScore;
    private String lifecycleStage;
}

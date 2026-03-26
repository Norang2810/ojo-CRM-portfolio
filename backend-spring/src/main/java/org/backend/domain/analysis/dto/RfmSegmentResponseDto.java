package org.backend.domain.analysis.dto;

import java.util.List;

public record RfmSegmentResponseDto(
    Long totalCount,
    List<SegmentDetail> segmentDetailList
) {
    public record SegmentDetail(
        String type,
        Long count,
        Double ratio,
        Double avgR,
        Double avgF,
        Double avgM
    ){}
}

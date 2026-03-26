package org.backend.domain.advice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class AdviceTimelineResponse {

    private Long memberId;
    private List<TimelineItem> timeline;

    @Getter
    @Builder
    public static class TimelineItem {
        private LocalDate date;
        private String category;
        private String direction;
        private String content;
        private String promotionName;
        private Long satisfactionScore;
    }
}
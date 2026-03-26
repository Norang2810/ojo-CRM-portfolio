package org.backend.domain.advice.controller;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.domain.advice.dto.AdviceTimelineResponse;
import org.backend.domain.advice.service.AdviceTimelineService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/advice")
public class AdviceTimelineController {

    private final AdviceTimelineService adviceTimelineService;

    @GetMapping("/{memberId}")
    public CommonResponse<AdviceTimelineResponse> getAdviceTimeline(@PathVariable Long memberId) {
        try {
            AdviceTimelineResponse response = adviceTimelineService.getAdviceTimeline(memberId);
            return CommonResponse.success(response, null);
        } catch (Exception e) {
            return CommonResponse.error(e.getMessage());
        }
    }
}
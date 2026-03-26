package org.backend.domain.auth.controller;

import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.domain.auth.dto.response.AccessLogSummaryDto;
import org.backend.domain.auth.service.AccessLogService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/access-logs")
public class AccessLogController {

    private final AccessLogService accessLogService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<CommonResponse<Page<AccessLogSummaryDto>>> getAccessLogs(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Page<AccessLogSummaryDto> result = accessLogService.getAccessLogs(page, size);
        return ResponseEntity.ok(CommonResponse.success(result, "접근로그 목록 조회 성공"));
    }
}
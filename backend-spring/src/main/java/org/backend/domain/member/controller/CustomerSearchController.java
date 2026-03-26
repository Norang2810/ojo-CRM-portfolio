package org.backend.domain.member.controller;

import org.backend.common.CommonResponse;
import org.backend.common.page.PageResponse;
import org.backend.domain.member.dto.search.CustomerSearchSummaryResponse;
import org.backend.domain.member.service.CustomerSearchService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
public class CustomerSearchController {

    private final CustomerSearchService service;

    public CustomerSearchController(CustomerSearchService service) {
        this.service = service;
    }

    // 고객 검색
    @GetMapping("/search")
    public CommonResponse<PageResponse<CustomerSearchSummaryResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        Page<CustomerSearchSummaryResponse> result =
                service.search(keyword, page, size);

        return CommonResponse.success(
                PageResponse.from(result),
                "고객 검색 성공"
        );
    }
}

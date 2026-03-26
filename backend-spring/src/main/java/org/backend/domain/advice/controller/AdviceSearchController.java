package org.backend.domain.advice.controller;


import lombok.RequiredArgsConstructor;
import org.backend.common.CommonResponse;
import org.backend.domain.advice.dto.AdviceSearchResponse;
import org.backend.domain.advice.service.AdviceSearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/advice")
public class AdviceSearchController {

    private final AdviceSearchService adviceSearchService;

    @GetMapping("/search")
    public CommonResponse<Page<AdviceSearchResponse>> searchAdvice(
            @RequestParam(name = "keyword", required = false, defaultValue = "") String keyword,
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Direction.DESC) Pageable pageable) {

        try{

            Page<AdviceSearchResponse> searchResponsePage = adviceSearchService.searchByKeyword(keyword, pageable);

            return CommonResponse.success(searchResponsePage,null);

        }catch (Exception e){
            return CommonResponse.error(e.getMessage());
        }
    }
}

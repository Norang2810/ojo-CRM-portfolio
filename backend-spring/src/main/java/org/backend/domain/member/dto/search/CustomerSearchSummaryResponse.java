package org.backend.domain.member.dto.search;

public record CustomerSearchSummaryResponse(
        Long memberId,
        String name,
        String email,
        String phone,
        String service,
        String servicePeriod,
        String consultCategory,
        String consultFrequency,
        String vip
) {}
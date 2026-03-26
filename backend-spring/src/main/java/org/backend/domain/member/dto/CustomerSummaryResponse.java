package org.backend.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerSummaryResponse {

    private Long memberId;
    private String name;
    private String email;
    private String phone;
    private String service;
    private String servicePeriod;
    private String consultCategory;
    private String consultFrequency;
    private String vip;
}
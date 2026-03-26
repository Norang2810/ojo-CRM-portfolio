package org.backend.domain.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerFilterRequest {
    private String segment;
    private String frequency;
    private String service;
    private Long categoryId;
}
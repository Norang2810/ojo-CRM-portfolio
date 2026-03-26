package org.backend.domain.advice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HourlyConsultationDto {
    private Integer hour;
    private Long inbound;
    private Long outbound;
    private Long total;
}

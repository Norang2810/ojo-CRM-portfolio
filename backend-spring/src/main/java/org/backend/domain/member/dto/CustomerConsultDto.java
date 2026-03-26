package org.backend.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CustomerConsultDto {

    private Long adviceId;
    private String categoryName;
    private String direction;
    private String channel;
    private Long satisfactionScore;
    private Boolean isConverted;
    private LocalDateTime createdAt;

}

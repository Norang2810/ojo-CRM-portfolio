package org.backend.domain.member.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@NoArgsConstructor
public class CustomerFilterResponse {
    private Long memberId;
    private String name;
    private String email;
    private String phone;
    private String service;
    private String servicePeriod;
    private String consultCategory;
    private String consultFrequency;
    private String vip;

    public CustomerFilterResponse(Long memberId, String name, String email, String phone,
            String service, String consultCategory, LocalDateTime createdAt, Long frequency, String type) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.service = service;
        this.servicePeriod = createdAt != null ? createdAt.toLocalDate().toString() + " ~ 현재" : null;
        this.consultCategory = consultCategory;

        if (frequency == null) {
            this.consultFrequency = "LOW";
        } else if (frequency <= 2) {
            this.consultFrequency = "LOW";
        } else if (frequency <= 5) {
            this.consultFrequency = "MEDIUM";
        } else {
            this.consultFrequency = "HIGH";
        }

        if (type == null) {
            this.vip = "일반";
        } else {
            this.vip = switch (type.toUpperCase()) {
                case "VIP" -> "VIP";
                case "LOYAL" -> "잠재 VIP";
                case "RISK" -> "이탈 우려";
                case "LOST" -> "이탈";
                case "COMMON", "일반" -> "일반";
                default -> type;
            };
        }
    }
}

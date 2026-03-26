package org.backend.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.backend.domain.member.entity.MemberConsent;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ConsentResponse {

    private String personalAccepted;
    private String marketingAccepted;
    private String isConverted;
    private LocalDateTime acceptedAt;
    private LocalDateTime expiresAt;

    public static ConsentResponse from(MemberConsent consent) {
        return new ConsentResponse(
                consent.getPersonalAccepted(),
                consent.getMarketingAccepted(),
                consent.getIsConverted(),
                consent.getAcceptedAt(),
                consent.getExpiresAt()
        );
    }
}
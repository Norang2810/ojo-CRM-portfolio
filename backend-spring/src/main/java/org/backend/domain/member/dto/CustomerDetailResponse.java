package org.backend.domain.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.backend.domain.member.entity.Member;
import org.backend.domain.member.entity.MemberConsent;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class CustomerDetailResponse {

    private Long memberId;
    private String name;
    private String phone;
    private String email;
    private String gender;
    private LocalDate birthDate;
    private String region;
    private String status;
    private ConsentResponse consent;

    public static CustomerDetailResponse from(Member member) {

        MemberConsent consent = member.getConsent();

        return new CustomerDetailResponse(
                member.getId(),
                member.getName(),
                member.getPhone(),
                member.getEmail(),
                member.getGender(),
                member.getBirthDate(),
                member.getRegion(),
                member.getStatus(),
                consent != null ? ConsentResponse.from(consent) : null
        );
    }
}
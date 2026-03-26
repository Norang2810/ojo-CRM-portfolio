package org.backend.domain.batch.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.backend.domain.batch.entity.ConsultationBasics;
import org.backend.domain.batch.entity.FeatureUsage;
import org.backend.domain.batch.entity.Lifecycle;
import org.backend.domain.batch.entity.Monetary;

@Getter
@Builder
public class MemberFeatureResponse {
    private Long memberId;
    private Lifecycle lifecycle;
    private Monetary monetary;
    private FeatureUsage usage;
    private ConsultationBasics consultation;
}
package org.backend.domain.batch.service;

import lombok.RequiredArgsConstructor;
import org.backend.domain.batch.dto.response.MemberFeatureResponse;
import org.backend.domain.batch.repository.ConsultationBasicsRepository;
import org.backend.domain.batch.repository.FeatureUsageRepository;
import org.backend.domain.batch.repository.LifecycleRepository;
import org.backend.domain.batch.repository.MonetaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberFeatureService {

    private final LifecycleRepository lifecycleRepository;
    private final MonetaryRepository monetaryRepository;
    private final FeatureUsageRepository featureUsageRepository;
    private final ConsultationBasicsRepository consultationRepository;

    public MemberFeatureResponse getMemberFeatures(Long memberId) {
        
        return MemberFeatureResponse.builder()
                .memberId(memberId)
                .lifecycle(lifecycleRepository.findFirstByMemberIdOrderByFeatureBaseDateDesc(memberId).orElse(null))
                .monetary(monetaryRepository.findFirstByMemberIdOrderByFeatureBaseDateDesc(memberId).orElse(null))
                .usage(featureUsageRepository.findFirstByMemberIdOrderByFeatureBaseDateDesc(memberId).orElse(null))
                .consultation(consultationRepository.findFirstByMemberIdOrderByFeatureBaseDateDesc(memberId).orElse(null))
                .build();
    }
}
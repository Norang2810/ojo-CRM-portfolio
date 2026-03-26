package org.backend.domain.subscription.service;

import lombok.RequiredArgsConstructor;
import org.backend.common.exception.CustomException;
import org.backend.common.exception.ErrorCode;
import org.backend.domain.subscription.dto.response.ProductResponse;
import org.backend.domain.subscription.dto.response.SubscriptionListResponse;
import org.backend.domain.subscription.dto.response.SubscriptionResponse;
import org.backend.domain.subscription.entity.SubscriptionPeriod;
import org.backend.domain.subscription.repository.SubscriptionPeriodRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionQueryServiceImpl implements SubscriptionQueryService {

    private final SubscriptionPeriodRepository subscriptionPeriodRepository;

    // 현재 이용 중인 서비스 조회
    @Override
    public SubscriptionListResponse getActiveSubscriptions(Long memberId) {

        List<SubscriptionPeriod> activeSubscriptions =
                subscriptionPeriodRepository.findAllByMember_IdAndStatus(memberId, "ACTIVE");

        if (activeSubscriptions.isEmpty()) {
            throw new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
        }

        List<SubscriptionResponse> responses = activeSubscriptions.stream()
                .map(subscription -> new SubscriptionResponse(
                        subscription.getId(),
                        new ProductResponse(
                                subscription.getProduct().getId(),
                                subscription.getProduct().getProductName(),
                                subscription.getProduct().getProductType(),
                                subscription.getProduct().getPrice()
                        ),
                        subscription.getQuantity(),
                        subscription.getTotalPrice(),
                        subscription.getStartedAt(),
                        subscription.getStatus()
                ))
                .toList();

        return new SubscriptionListResponse(responses);
    }
}

package org.backend.domain.advice.view;

import java.time.LocalDateTime;

public interface CustomerConsultView {

    Long getAdviceId();

    String getCategoryName();

    String getDirection();

    String getChannel();

    Long getSatisfactionScore();

    Boolean getIsConverted();

    LocalDateTime getCreatedAt();
}

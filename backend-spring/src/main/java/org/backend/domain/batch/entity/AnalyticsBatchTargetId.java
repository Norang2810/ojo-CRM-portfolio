package org.backend.domain.batch.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AnalyticsBatchTargetId implements Serializable {
    private String batchId;
    private Long memberId;
}

package org.backend.domain.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "analytics_batch_target")
@IdClass(AnalyticsBatchTargetId.class)
@Getter
public class AnalyticsBatchTargetEntity {

    @Id
    @Column(name = "batch_id", length = 36)
    private String batchId;

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "inference_required", nullable = false)
    private Boolean inferenceRequired;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "status", nullable = false)
    private String status;
}

package org.backend.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.backend.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis")
@Getter
public class Analysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "rfm_score")
    private Integer rfmScore;

    @Column(name = "type", length = 255)
    private String type;

    @Column(name = "ltv")
    private Long ltv;

    @Column(name = "lifecycle_stage", length = 255)
    private String lifecycleStage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "r_score")
    private Integer rScore;

    @Column(name = "f_score")
    private Integer fScore;

    @Column(name = "m_score")
    private Integer mScore;
}

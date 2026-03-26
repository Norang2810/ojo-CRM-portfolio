package org.backend.domain.analysis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.backend.domain.member.entity.Member;

import java.time.LocalDateTime;

@Entity
@Table(name = "rfm")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// member_id + updated_at로 복합키 사용할 수도 있음
public class Rfm {
    @Id
    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, insertable = false, updatable = false)
    private Member member;

    private LocalDateTime recency;
    private Integer frequency;
    private Long monetary;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

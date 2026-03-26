package org.backend.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "memo", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_memo_admin_member", columnNames = {"admin_id", "member_id"})
})
public class MemberMemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "memo_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "summary", length = 20)
    private String summary;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MemberMemo(Long adminId, Long memberId, String content, String summary) {
        this.adminId = adminId;
        this.memberId = memberId;
        this.content = content;
        this.summary = summary;
    }

    public void updateContent(String content, String summary) {
        this.content = content;
        this.summary = summary;
    }
}
package org.backend.domain.subscription.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotion")
@Getter
@NoArgsConstructor
public class Promotion {

    @Id
    @Column(name = "promotion_id", length = 255)
    private String id;

    @Column(name = "promotion_name", nullable = false, length = 255)
    private String promotionName;

    @Column(name = "promotion_detail", columnDefinition = "TEXT")
    private String promotionDetail;

}
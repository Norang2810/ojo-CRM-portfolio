package org.backend.domain.subscription.entity;


import jakarta.persistence.*;
import lombok.Getter;


@Entity
@Getter
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(name = "product_name", nullable = false, length = 50)
    private String productName;

    @Column(name = "product_type", nullable = false, length = 20)
    private String productType;

    @Column(name = "product_category", nullable = false, length = 20)
    private String productCategory;

    @Column(nullable = false)
    private Long price;

}
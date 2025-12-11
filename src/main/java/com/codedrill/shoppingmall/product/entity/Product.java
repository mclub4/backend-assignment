package com.codedrill.shoppingmall.product.entity;

import com.codedrill.shoppingmall.common.entity.BaseEntity;
import com.codedrill.shoppingmall.common.enums.EnumProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumProductStatus status;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private Long price;

    @Column(nullable = false)
    private Integer stock;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long userId; // 상품을 등록한 사용자 ID

    @Builder
    public Product(EnumProductStatus status, String name, Long price, Integer stock, String description, Long userId) {
        this.status = status;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.userId = userId;
    }

    public void update(String name, Long price, Integer stock, String description) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
    }

    public void approve() {
        this.status = EnumProductStatus.APPROVED;
    }

    public void decreaseStock(Integer quantity) {
        this.stock -= quantity;
    }

    public void increaseStock(Integer quantity) {
        this.stock += quantity;
    }
}


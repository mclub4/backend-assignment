package com.codedrill.shoppingmall.order.entity;

import com.codedrill.shoppingmall.common.entity.BaseEntity;
import com.codedrill.shoppingmall.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Long totalPrice;

    @Builder
    public Order(User user, OrderStatus status, Long totalPrice) {
        this.user = user;
        this.status = status;
        this.totalPrice = totalPrice;
    }

    public void pay() {
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }

    public boolean canChangeStatus() {
        return this.status != OrderStatus.CANCELLED && this.status != OrderStatus.COMPLETED;
    }
}


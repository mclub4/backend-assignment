package com.codedrill.shoppingmall.order.repository;

import com.codedrill.shoppingmall.order.entity.Order;
import com.codedrill.shoppingmall.order.entity.OrderStatus;
import com.codedrill.shoppingmall.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.user = :user " +
           "AND (:status IS NULL OR o.status = :status) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findByUserAndStatus(
            @Param("user") User user,
            @Param("status") OrderStatus status,
            Pageable pageable
    );
}


package com.codedrill.shoppingmall.order.repository;

import com.codedrill.shoppingmall.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}


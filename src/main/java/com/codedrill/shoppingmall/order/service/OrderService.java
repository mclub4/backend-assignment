package com.codedrill.shoppingmall.order.service;

import com.codedrill.shoppingmall.common.entity.PrincipalDetails;
import com.codedrill.shoppingmall.common.exception.BusinessException;
import com.codedrill.shoppingmall.common.exception.ErrorCode;
import com.codedrill.shoppingmall.common.util.SecurityUtil;
import com.codedrill.shoppingmall.order.dto.*;
import com.codedrill.shoppingmall.order.entity.Order;
import com.codedrill.shoppingmall.order.entity.OrderItem;
import com.codedrill.shoppingmall.order.entity.OrderStatus;
import com.codedrill.shoppingmall.order.repository.OrderItemRepository;
import com.codedrill.shoppingmall.order.repository.OrderRepository;
import com.codedrill.shoppingmall.product.entity.Product;
import com.codedrill.shoppingmall.common.enums.EnumProductStatus;
import com.codedrill.shoppingmall.product.repository.ProductRepository;
import com.codedrill.shoppingmall.user.entity.User;
import com.codedrill.shoppingmall.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Deprecated
public class OrderService {

    // OrderItem 생성 전 임시 데이터 클래스
    private static class OrderItemData {
        private final Product product;
        private final Long price;
        private final Integer quantity;

        public OrderItemData(Product product, Long price, Integer quantity) {
            this.product = product;
            this.price = price;
            this.quantity = quantity;
        }

        public Product getProduct() {
            return product;
        }

        public Long getPrice() {
            return price;
        }

        public Integer getQuantity() {
            return quantity;
        }
    }

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, PrincipalDetails user) {
        // 사용자 조회
        User orderUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        // 주문 상품 검증 및 재고 차감
        List<OrderItemData> itemDataList = request.getItems().stream()
                .map(itemRequest -> {
                    Product product = productRepository.findByIdAndNotDeleted(itemRequest.getProductId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

                    // 승인된 상품만 주문 가능
                    if (product.getStatus() != EnumProductStatus.APPROVED) {
                        throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
                    }

                    // 재고 검증
                    if (product.getStock() < itemRequest.getQuantity()) {
                        throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
                    }

                    // 재고 차감
                    product.decreaseStock(itemRequest.getQuantity());

                    return new OrderItemData(product, product.getPrice(), itemRequest.getQuantity());
                })
                .collect(Collectors.toList());

        // 총 가격 계산
        Long totalPrice = itemDataList.stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();

        // 주문 생성
        Order order = Order.builder()
                .user(orderUser)
                .status(OrderStatus.CREATED)
                .totalPrice(totalPrice)
                .build();

        Order savedOrder = orderRepository.save(order);

        // OrderItem 생성 및 저장
        List<OrderItem> orderItems = itemDataList.stream()
                .map(itemData -> {
                    OrderItem orderItem = OrderItem.builder()
                            .order(savedOrder)
                            .product(itemData.getProduct())
                            .price(itemData.getPrice())
                            .quantity(itemData.getQuantity())
                            .build();
                    return orderItemRepository.save(orderItem);
                })
                .collect(Collectors.toList());

        return toOrderResponse(savedOrder);
    }

    public Page<OrderResponse> getMyOrders(Integer page, Integer size, String status, PrincipalDetails user) {
        User orderUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        OrderStatus orderStatus = parseStatus(status);

        Page<Order> orderPage = orderRepository.findByUserAndStatus(orderUser, orderStatus, pageable);

        return orderPage.map(this::toOrderResponse);
    }

    public OrderDetailResponse getOrder(Long id, PrincipalDetails user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 권한 체크: 본인 주문이거나 ADMIN만 조회 가능
        if (!SecurityUtil.isAdmin(user) && !order.getUser().getId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // OrderItem 조회
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(id);

        return toOrderDetailResponse(order, orderItems);
    }

    @Transactional
    public OrderResponse payOrder(Long id, PrincipalDetails user) {
        Order order = findByIdAndCheckAccess(id, user);

        // 상태 전이 검증: CREATED -> PAID
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.pay();
        Order savedOrder = orderRepository.save(order);

        return toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long id, PrincipalDetails user) {
        Order order = findByIdAndCheckAccess(id, user);

        // 상태 전이 검증: CREATED -> CANCELLED
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 재고 복구
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(id);
        orderItems.forEach(item -> {
            Product product = item.getProduct();
            product.increaseStock(item.getQuantity());
            productRepository.save(product);
        });

        order.cancel();
        Order savedOrder = orderRepository.save(order);

        return toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse completeOrder(Long id, PrincipalDetails user) {
        Order order = findByIdAndCheckAccess(id, user);

        // 상태 전이 검증: PAID -> COMPLETED
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        order.complete();
        Order savedOrder = orderRepository.save(order);

        return toOrderResponse(savedOrder);
    }

    private Order findByIdAndCheckAccess(Long id, PrincipalDetails user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 권한 체크: 본인 주문이거나 ADMIN만 접근 가능
        if (!SecurityUtil.isAdmin(user) && !order.getUser().getId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return order;
    }

    private OrderStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }

        try {
            return OrderStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .status(order.getStatus().name())
                .totalPrice(order.getTotalPrice())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderDetailResponse toOrderDetailResponse(Order order, List<OrderItem> orderItems) {
        List<OrderItemResponse> itemResponses = orderItems.stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());

        return OrderDetailResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .status(order.getStatus().name())
                .totalPrice(order.getTotalPrice())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemResponse toOrderItemResponse(OrderItem orderItem) {
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct().getId())
                .productName(orderItem.getProduct().getName())
                .price(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .build();
    }
}

package com.codedrill.shoppingmall.order.controller;

import com.codedrill.shoppingmall.common.consts.RestUriConst;
import com.codedrill.shoppingmall.common.entity.PrincipalDetails;
import com.codedrill.shoppingmall.common.response.Response;
import com.codedrill.shoppingmall.order.dto.OrderCreateRequest;
import com.codedrill.shoppingmall.order.dto.OrderDetailResponse;
import com.codedrill.shoppingmall.order.dto.OrderResponse;
import com.codedrill.shoppingmall.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(RestUriConst.REST_URI_ORDER)
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "주문 생성")
    public Response<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        OrderResponse order = orderService.createOrder(request, user);
        return Response.success(order);
    }

    @GetMapping("/my")
    @Operation(summary = "내 주문 목록 조회")
    public Response<Page<OrderResponse>> getMyOrders(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        Page<OrderResponse> orders = orderService.getMyOrders(page, size, status, user);
        return Response.success(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "주문 상세 조회")
    public Response<OrderDetailResponse> getOrder(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        OrderDetailResponse order = orderService.getOrder(id, user);
        return Response.success(order);
    }

    @PatchMapping("/{id}/pay")
    @Operation(summary = "주문 결제")
    public Response<OrderResponse> payOrder(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        OrderResponse order = orderService.payOrder(id, user);
        return Response.success(order);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "주문 취소")
    public Response<OrderResponse> cancelOrder(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        OrderResponse order = orderService.cancelOrder(id, user);
        return Response.success(order);
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "주문 완료")
    public Response<OrderResponse> completeOrder(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        OrderResponse order = orderService.completeOrder(id, user);
        return Response.success(order);
    }
}

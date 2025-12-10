package com.codedrill.shoppingmall.order.controller;

import com.codedrill.shoppingmall.common.consts.RestUriConst;
import com.codedrill.shoppingmall.common.response.Response;
import com.codedrill.shoppingmall.order.dto.OrderCreateRequest;
import com.codedrill.shoppingmall.order.dto.OrderDetailResponse;
import com.codedrill.shoppingmall.order.dto.OrderResponse;
import com.codedrill.shoppingmall.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(RestUriConst.REST_URI_ORDER)
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "주문 생성")
    public Response<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        //TODO: 주문 생성 구현
        return Response.success();
    }

    @GetMapping("/my")
    @Operation(summary = "내 주문 목록 조회")
    public Response<Object> getMyOrders(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status
    ) {
        //TODO: 내 주문 목록 조회 구현
        return Response.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "주문 상세 조회")
    public Response<OrderDetailResponse> getOrder(@PathVariable Long id) {
        //TODO: 주문 상세 조회 구현
        return Response.success();
    }

    @PatchMapping("/{id}/pay")
    @Operation(summary = "주문 결제")
    public Response<OrderResponse> payOrder(@PathVariable Long id) {
        //TODO: 주문 결제 구현
        return Response.success();
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "주문 취소")
    public Response<OrderResponse> cancelOrder(@PathVariable Long id) {
        //TODO: 주문 취소 구현
        return Response.success();
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "주문 완료")
    public Response<OrderResponse> completeOrder(@PathVariable Long id) {
        //TODO: 주문 완료 구현
        return Response.success();
    }
}

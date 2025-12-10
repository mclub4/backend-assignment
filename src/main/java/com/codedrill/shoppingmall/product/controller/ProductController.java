package com.codedrill.shoppingmall.product.controller;

import com.codedrill.shoppingmall.common.consts.RestUriConst;
import com.codedrill.shoppingmall.common.response.Response;
import com.codedrill.shoppingmall.product.dto.*;
import com.codedrill.shoppingmall.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(RestUriConst.REST_URI_PRODUCT)
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "상품 등록")
    public Response<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        //TODO: 상품 등록 구현
        return Response.success();
    }

    @GetMapping
    @Operation(summary = "상품 목록 조회")
    public Response<ProductPageResponse> getProductList(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(required = false) String name
    ) {
        //TODO: 상품 목록 조회 구현
        return Response.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "상품 단건 조회")
    public Response<ProductDetailResponse> getProduct(@PathVariable Long id) {
        //TODO: 상품 단건 조회 구현
        return Response.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "상품 수정")
    public Response<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        //TODO: 상품 수정 구현
        return Response.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "상품 삭제 (Soft Delete)")
    public Response<Void> deleteProduct(@PathVariable Long id) {
        //TODO: 상품 삭제 구현
        return Response.success();
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "상품 승인")
    public Response<ProductResponse> approveProduct(@PathVariable Long id) {
        //TODO: 상품 승인 구현
        return Response.success();
    }
}


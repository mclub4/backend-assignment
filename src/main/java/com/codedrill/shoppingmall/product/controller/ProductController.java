package com.codedrill.shoppingmall.product.controller;

import com.codedrill.shoppingmall.common.consts.RestUriConst;
import com.codedrill.shoppingmall.common.entity.PrincipalDetails;
import com.codedrill.shoppingmall.common.response.Response;
import com.codedrill.shoppingmall.product.dto.*;
import com.codedrill.shoppingmall.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
        ProductResponse product = productService.createProduct(request);
        return Response.success(product);
    }

    @GetMapping
    @Operation(summary = "상품 목록 조회")
    public Response<ProductPageResponse> getProductList(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "minPrice", required = false) Long minPrice,
            @RequestParam(value = "maxPrice", required = false) Long maxPrice,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal PrincipalDetails user
            ) {
        ProductPageResponse productPage = productService.getProductList(page, size, minPrice, maxPrice, name, status, user);
        return Response.success(productPage);
    }

    @GetMapping("/{id}")
    @Operation(summary = "상품 단건 조회")
    public Response<ProductDetailResponse> getProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        ProductDetailResponse product = productService.getProduct(id, user);
        return Response.success(product);
    }

    @PutMapping("/{id}")
    @Operation(summary = "상품 수정")
    public Response<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        ProductResponse product = productService.updateProduct(id, request);
        return Response.success(product);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "상품 삭제 (Soft Delete)")
    public Response<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Response.success();
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "상품 승인")
    public Response<ProductResponse> approveProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        ProductResponse product = productService.approveProduct(id, user);
        return Response.success(product);
    }
}


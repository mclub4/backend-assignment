package com.codedrill.shoppingmall.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPageResponse {
    private List<ProductSummary> content;
    private Long totalElements;
    private Integer totalPages;
    private Integer page;
    private Integer size;
}


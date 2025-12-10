package com.codedrill.shoppingmall.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductUpdateRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    @Size(min = 2, max = 50, message = "상품명은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
    private Long price;

    @Min(value = 0, message = "재고는 0 이상이어야 합니다.")
    private Integer stock;

    private String description;
}


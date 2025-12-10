package com.codedrill.shoppingmall.product.service;

import com.codedrill.shoppingmall.common.exception.BusinessException;
import com.codedrill.shoppingmall.common.exception.ErrorCode;
import com.codedrill.shoppingmall.common.util.SecurityUtil;
import com.codedrill.shoppingmall.product.dto.*;
import com.codedrill.shoppingmall.product.entity.Product;
import com.codedrill.shoppingmall.product.entity.ProductStatus;
import com.codedrill.shoppingmall.product.repository.ProductRepository;
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
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = Product.builder()
                .status(ProductStatus.PENDING)
                .name(request.getName())
                .price(request.getPrice())
                .stock(request.getStock())
                .description(request.getDescription())
                .build();

        Product savedProduct = productRepository.save(product);
        return toProductResponse(savedProduct);
    }

    public ProductPageResponse getProductList(
            Integer page, Integer size,
            Long minPrice, Long maxPrice, String name
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        // 일반 사용자는 APPROVED만, ADMIN은 모두 조회 가능
        ProductStatus status = SecurityUtil.isAdmin() ? null : ProductStatus.APPROVED;
        
        Page<Product> productPage = productRepository.findProducts(
                status, minPrice, maxPrice, name, pageable
        );

        List<ProductSummary> content = productPage.getContent().stream()
                .map(this::toProductSummary)
                .collect(Collectors.toList());

        return ProductPageResponse.builder()
                .content(content)
                .totalElements(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .build();
    }

    public ProductDetailResponse getProduct(Long id) {
        Product product = findByIdAndNotDeleted(id);
        
        // 일반 사용자는 APPROVED만 조회 가능
        if (!SecurityUtil.isAdmin() && product.getStatus() != ProductStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
        }
        
        return toProductDetailResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = findByIdAndNotDeleted(id);
        
        product.update(
                request.getName(),
                request.getPrice(),
                request.getStock(),
                request.getDescription()
        );

        Product updatedProduct = productRepository.save(product);
        return toProductResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findByIdAndNotDeleted(id);
        product.softDelete();
        productRepository.save(product);
    }

    @Transactional
    public ProductResponse approveProduct(Long id) {
        // ADMIN만 승인 가능
        if (!SecurityUtil.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Product product = findByIdAndNotDeleted(id);
        
        if (product.getStatus() != ProductStatus.PENDING) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_PENDING);
        }

        product.approve();
        Product approvedProduct = productRepository.save(product);
        return toProductResponse(approvedProduct);
    }

    private Product findByIdAndNotDeleted(Long id) {
        return productRepository.findByIdAndNotDeleted(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .status(product.getStatus().name())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .description(product.getDescription())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductDetailResponse toProductDetailResponse(Product product) {
        return ProductDetailResponse.builder()
                .id(product.getId())
                .status(product.getStatus().name())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .description(product.getDescription())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductSummary toProductSummary(Product product) {
        return ProductSummary.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .status(product.getStatus().name())
                .build();
    }
}


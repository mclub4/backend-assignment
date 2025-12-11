package com.codedrill.shoppingmall.product.service;

import com.codedrill.shoppingmall.common.entity.PrincipalDetails;
import com.codedrill.shoppingmall.common.exception.BusinessException;
import com.codedrill.shoppingmall.common.exception.ErrorCode;
import com.codedrill.shoppingmall.common.util.SecurityUtil;
import com.codedrill.shoppingmall.product.dto.*;
import com.codedrill.shoppingmall.product.entity.Product;
import com.codedrill.shoppingmall.common.enums.EnumProductStatus;
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
    public ProductResponse createProduct(ProductCreateRequest request, PrincipalDetails user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // PENDING 상태인 상품이 이미 있으면 새로운 상품 등록 불가
        long pendingCount = productRepository.countPendingProductsByUserId(user.getUserId());
        if (pendingCount > 0) {
            throw new BusinessException(ErrorCode.PRODUCT_PENDING_EXISTS);
        }

        Product product = Product.builder()
                .status(EnumProductStatus.PENDING)
                .name(request.getName())
                .price(request.getPrice())
                .stock(request.getStock())
                .description(request.getDescription())
                .userId(user.getUserId())
                .build();

        Product savedProduct = productRepository.save(product);
        return toProductResponse(savedProduct);
    }

    public ProductPageResponse getProductList(
            Integer page, Integer size,
            Long minPrice, Long maxPrice, String name, String status,
            PrincipalDetails user
    ) {
        Pageable pageable = PageRequest.of(page, size);
        
        // 상태 처리: ADMIN은 Query Param의 status 사용, 일반 사용자는 APPROVED만
        String statusString;
        if (SecurityUtil.isAdmin(user)) {
            // ADMIN인 경우 Query Param의 status를 사용 (null이면 모든 상태 조회)
            EnumProductStatus productStatus = parseStatus(status);
            statusString = productStatus != null ? productStatus.name() : null;
        } else {
            // 일반 사용자는 항상 APPROVED만 조회
            statusString = EnumProductStatus.APPROVED.name();
        }
        
        // 상품명 전처리: null 체크, trim, 빈 문자열 처리
        String processedName = preprocessName(name);
        
        Page<Product> productPage = productRepository.findProducts(
                statusString, minPrice, maxPrice, processedName, pageable
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

    public ProductDetailResponse getProduct(Long id, PrincipalDetails user) {
        Product product = findByIdAndNotDeleted(id);
        
        // 일반 사용자는 APPROVED만 조회 가능
        if (!SecurityUtil.isAdmin(user) && product.getStatus() != EnumProductStatus.APPROVED) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_APPROVED);
        }
        
        return toProductDetailResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request, PrincipalDetails user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Product product = findByIdAndNotDeleted(id);
        
        // 등록한 사용자만 수정 가능 (ADMIN은 모든 상품 수정 가능)
        if (!SecurityUtil.isAdmin(user) && !product.getUserId().equals(user.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        
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
    public ProductResponse approveProduct(Long id, PrincipalDetails user) {
        Product product = findByIdAndNotDeleted(id);
        
        if (product.getStatus() != EnumProductStatus.PENDING) {
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

    /**
     * 상품명 검색 파라미터 전처리
     * - null 체크
     * - 앞뒤 공백 제거
     * - 빈 문자열인 경우 null 반환 (검색 조건에서 제외)
     */
    private String preprocessName(String name) {
        if (name == null) {
            return null;
        }
        
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        
        return trimmed;
    }

    /**
     * 상태 문자열을 ProductStatus enum으로 변환
     * - null이거나 빈 문자열이면 null 반환 (모든 상태 조회)
     * - 유효하지 않은 값이면 예외 발생
     */
    private EnumProductStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        
        try {
            return EnumProductStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_STATUS);
        }
    }

}


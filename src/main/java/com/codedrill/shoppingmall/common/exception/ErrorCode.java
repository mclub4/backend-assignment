package com.codedrill.shoppingmall.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    VALIDATION_ERROR("VALIDATION_ERROR", "입력값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST_BODY("INVALID_REQUEST_BODY", "요청 본문이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    NOT_FOUND("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    
    // 인증/인가
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다."),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    DUPLICATE_EMAIL("DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    
    // 상품
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),
    PRODUCT_ALREADY_DELETED("PRODUCT_ALREADY_DELETED", "이미 삭제된 상품입니다."),
    PRODUCT_NOT_APPROVED("PRODUCT_NOT_APPROVED", "승인되지 않은 상품입니다."),
    PRODUCT_ALREADY_APPROVED("PRODUCT_ALREADY_APPROVED", "이미 승인된 상품입니다."),
    PRODUCT_NOT_PENDING("PRODUCT_NOT_PENDING", "승인 대기 중인 상품만 승인할 수 있습니다."),
    PRODUCT_PENDING_EXISTS("PRODUCT_PENDING_EXISTS", "승인 대기 중인 상품이 이미 존재합니다. 새로운 상품을 등록하려면 기존 상품이 승인되거나 취소되어야 합니다."),
    INVALID_PRODUCT_STATUS("INVALID_PRODUCT_STATUS", "유효하지 않은 상품 상태입니다."),
    
    // 주문
    ORDER_NOT_FOUND("ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."),
    INSUFFICIENT_STOCK("INSUFFICIENT_STOCK", "재고가 부족합니다."),
    INVALID_ORDER_STATUS("INVALID_ORDER_STATUS", "잘못된 주문 상태입니다."),
    ORDER_ACCESS_DENIED("ORDER_ACCESS_DENIED", "주문에 접근할 권한이 없습니다.");

    private final String code;
    private final String message;
}


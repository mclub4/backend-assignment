package com.codedrill.shoppingmall.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static com.codedrill.shoppingmall.common.config.JwtAuthenticationFilter.ATTRIBUTE_TOKEN_ERROR;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public CustomAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        Exception exception = (Exception) request.getAttribute(ATTRIBUTE_TOKEN_ERROR);
        if (exception == null) {
            return;
        }

        if (exception instanceof JwtTokenInvalidException jwtTokenInvalidException) {
            resolver.resolveException(request, response, null, jwtTokenInvalidException);
        } else {
            // 처리되지 않은 exception이 발생한 경우입니다.
            log.error("{}: {}", exception.getClass(), exception.getMessage());
            resolver.resolveException(request, response, null, exception);
        }
    }

}

package com.codedrill.shoppingmall.user.controller;

import com.codedrill.shoppingmall.common.consts.RestUriConst;
import com.codedrill.shoppingmall.common.response.Response;
import com.codedrill.shoppingmall.user.dto.LoginRequest;
import com.codedrill.shoppingmall.user.dto.LoginResponse;
import com.codedrill.shoppingmall.user.dto.SignupRequest;
import com.codedrill.shoppingmall.user.dto.UserResponse;
import com.codedrill.shoppingmall.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(RestUriConst.REST_URI_AUTH)
public class AuthController {

    private final UserService userService;

    @PostMapping("/signup")
    @Operation(summary = "회원 가입")
    public Response<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
        //TODO: 회원 가입 구현
        return Response.success();
    }

    @PostMapping("/login")
    @Operation(summary = "로그인")
    public Response<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        //TODO: 로그인 구현
        return Response.success();
    }
}


package com.codedrill.shoppingmall.common.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@AllArgsConstructor
public class PrincipalDetails implements UserDetails {
    private Long userId;
    private String email;
    private String username;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
}

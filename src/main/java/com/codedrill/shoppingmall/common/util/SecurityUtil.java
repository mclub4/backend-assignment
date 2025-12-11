package com.codedrill.shoppingmall.common.util;

import com.codedrill.shoppingmall.common.entity.PrincipalDetails;
import com.codedrill.shoppingmall.common.enums.EnumRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    /**
     * PrincipalDetails의 authorities에서 특정 EnumRole을 가지고 있는지 확인
     * - EnumRole.name()으로 문자열을 얻어 authorities 리스트에 있는지 확인
     * - "ADMIN" 또는 "ROLE_ADMIN" 형태 모두 확인
     */
    public static boolean hasRole(PrincipalDetails user, EnumRole role) {
        if (user == null || user.getAuthorities() == null || user.getAuthorities().isEmpty()) {
            return false;
        }
        
        String roleName = role.name(); // "ADMIN" 또는 "USER"
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(roleName) || authority.equals("ROLE_" + roleName));
    }

    /**
     * PrincipalDetails의 authorities에서 ADMIN 권한을 가지고 있는지 확인
     */
    public static boolean isAdmin(PrincipalDetails user) {
        return hasRole(user, EnumRole.ADMIN);
    }
}


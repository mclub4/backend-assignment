package com.codedrill.shoppingmall.user.entity;

import com.codedrill.shoppingmall.common.enums.EnumRole;
import com.codedrill.shoppingmall.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 12)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumRole role;
}


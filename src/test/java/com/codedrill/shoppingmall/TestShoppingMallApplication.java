package com.codedrill.shoppingmall;

import org.springframework.boot.SpringApplication;

public class TestShoppingMallApplication {

    public static void main(String[] args) {
        SpringApplication.from(ShoppingMallApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

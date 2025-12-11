package com.codedrill.shoppingmall.product.repository;

import com.codedrill.shoppingmall.product.entity.Product;
import com.codedrill.shoppingmall.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = "SELECT p.* FROM products p WHERE p.deleted_at IS NULL " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:name IS NULL OR p.name ILIKE '%' || :name || '%')",
           countQuery = "SELECT COUNT(p.id) FROM products p WHERE p.deleted_at IS NULL " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:name IS NULL OR p.name ILIKE '%' || :name || '%')",
           nativeQuery = true)
    Page<Product> findProducts(
            @Param("status") String status,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("name") String name,
            Pageable pageable
    );

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdAndNotDeleted(@Param("id") Long id);
}


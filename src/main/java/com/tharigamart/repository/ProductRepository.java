package com.tharigamart.repository;

import com.tharigamart.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Dynamic catalogue search: free-text across title/brand/description,
     * category and price range. Only active products are shown publicly.
     * Null parameters are simply ignored.
     */
    @Query("""
            select p from Product p
            where p.active = true
              and (:q is null
                   or lower(p.title) like lower(concat('%', :q, '%'))
                   or lower(p.brand) like lower(concat('%', :q, '%'))
                   or lower(p.description) like lower(concat('%', :q, '%')))
              and (:categoryId is null or p.category.id = :categoryId)
              and (:minPrice is null or p.price >= :minPrice)
              and (:maxPrice is null or p.price <= :maxPrice)
            """)
    Page<Product> search(@Param("q") String q,
                         @Param("categoryId") Long categoryId,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);

    Page<Product> findByActiveTrueAndIsFeaturedTrue(Pageable pageable);

    /** All active products (used by the AI shopping assistant for catalogue context). */
    List<Product> findByActiveTrue();

    List<Product> findByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    /** Products listed by one seller. */
    List<Product> findBySellerId(Long sellerId);

    long countBySellerId(Long sellerId);

    /** Full catalogue including inactive products (admin). */
    @Query("select p from Product p order by p.createdAt desc")
    List<Product> findAllForAdmin();
}

package com.playdata.orderservice.product.repository;

import com.playdata.orderservice.product.dto.ProductSearchDto;
import com.playdata.orderservice.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

//    @Query("SELECT p FROM Product p WHERE p.category LIKE '%?%'")
//    void findByCategoryPaging(ProductSearchDto dto, Pageable pageable);
//    @Query("SELECT p FROM Product p WHERE p.name LIKE '%?%'")
//    void findByProdNamePaging(ProductSearchDto dto, Pageable pageable);

}

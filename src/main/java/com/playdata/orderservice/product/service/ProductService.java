package com.playdata.orderservice.product.service;

import com.playdata.orderservice.common.configs.AwsS3Config;
import com.playdata.orderservice.product.dto.ProductResDto;
import com.playdata.orderservice.product.dto.ProductSaveReqDto;
import com.playdata.orderservice.product.dto.ProductSearchDto;
import com.playdata.orderservice.product.entity.Product;
import com.playdata.orderservice.product.entity.QProduct;
import com.playdata.orderservice.product.repository.ProductRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.UUID;

import static com.playdata.orderservice.product.entity.QProduct.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final JPAQueryFactory factory;
    private final AwsS3Config s3Config;

    public Product productCreate(ProductSaveReqDto dto) throws IOException {

        MultipartFile productImage = dto.getProductImage();

        String uniqueFileName
                = UUID.randomUUID() + "_" + productImage.getOriginalFilename();

        /*
        File file
                = new File("/Users/stephen/Desktop/develop/upload/" + uniqueFileName);
        try {
            productImage.transferTo(file);
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패!");
        }
        */

        // 더 이상 로컬 경로에 이미지를 저장하지 않고, s3 버킷에 저장

        String imageUrl
                = s3Config.uploadToS3Bucket(productImage.getBytes(), uniqueFileName);


        Product product = dto.toEntity();
        product.updateImagePath(imageUrl); // 파일명이 아닌 s3 오브젝트의 url이 저장될 것이다.

        return productRepository.save(product);

    }

    public Page<ProductResDto> productList(ProductSearchDto searchDto, Pageable pageable) {

        /*
        Page<Product> products = productRepository.findAll(pageable);

        // 클라이언트단에 페이징에 필요한 데이터를 제공하기 위해 Page 객체 자체를 넘기려고 한다.
        // Page 안에 Entity가 들어있으니, 이것을 dto로 변환을 해서 넘기고 싶다. (Page 객체는 유지)
        // map을 통해 Product를 dto로 일괄 변환해서 리턴.
        Page<ProductResDto> productResDtos = products.map(p -> p.fromEntity());
         */

        BooleanBuilder builder = new BooleanBuilder();

        if (searchDto.getSearchName() != null) {
            // 상품 이름 검색 조건
            if (searchDto.getCategory().equals("name")) {
                builder.and(product.name.like("%" + searchDto.getSearchName() + "%"));
                // 상품 카테고리 검색 조건
            } else if (searchDto.getCategory().equals("category")) {
                builder.and(product.category.like("%" + searchDto.getSearchName() + "%"));
            }
        }

        // QueryDSL을 이용한 검색 및 페이징 처리
        List<Product> products = factory
                .selectFrom(product)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 총 검색 결과 수를 구하는 쿼리
        long total = factory
                .selectFrom(product)
                .where(builder)
                .fetchCount();

        // queryDSL로 조회한 내용을 모두 포함하는 Page 객체 생성.
        Page<Product> productPage
                = new PageImpl<>(products, pageable, total);

        // 엔터티를 일괄적으로 dto로 변환하기
        Page<ProductResDto> productResDtos
                = productPage.map(p -> p.fromEntity());


        return productResDtos;
    }

    public void productDelete(Long id) throws Exception {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Product with id " + id + " not found")
        );

        String imageUrl = product.getImagePath(); // S3 버킷 내의 오브젝트 url
        s3Config.deleteFromS3Bucket(imageUrl);

        productRepository.deleteById(id);
    }
}



















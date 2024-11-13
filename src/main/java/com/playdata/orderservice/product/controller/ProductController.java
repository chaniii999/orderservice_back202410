package com.playdata.orderservice.product.controller;

import com.playdata.orderservice.common.dto.CommonResDto;
import com.playdata.orderservice.product.dto.ProductResDto;
import com.playdata.orderservice.product.dto.ProductSaveReqDto;
import com.playdata.orderservice.product.dto.ProductSearchDto;
import com.playdata.orderservice.product.entity.Product;
import com.playdata.orderservice.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    // 요청과 함께 이미지가 전달이 될 것이다. 해당 이미지를 처리하는 방식이 두 가지로 나뉜다.
    // 1. JS의 FormData 객체를 통해 모든 데이터를 전달. (multipart/form-data 형식으로 전달, form 태그 x)
    // 2. JSON 형태로 전달 (이미지를 Base64 인코딩을 통해 문자열로 변환해서 전달)
    public ResponseEntity<?> createProduct(ProductSaveReqDto dto) throws IOException {

        log.info("/product/create: POST");
        Product product = productService.productCreate(dto);

        CommonResDto resDto
                = new CommonResDto(HttpStatus.CREATED, "product 등록 성공", product.getId());

        return new ResponseEntity<>(resDto, HttpStatus.CREATED);
    }


    @GetMapping("/list")
    // 페이징이 필요합니다. 리턴은 ProductResDto 형태로 리턴됩니다.
    // ProductResDto(id, name, category, price, stockQuantity, imagePath)
    // 컨트롤러 파라미터로 Pageable 선언하면, 페이징 파라미터 처리를 쉽게 진행할 수 있음.
    // /list?page=1&size=10&sort=name,desc 요런 식으로.
    // 요청 시 쿼리스트링이 전달되지 않으면 기본값 0, 20, unsorted
    public ResponseEntity<?> listProducts(ProductSearchDto searchDto, Pageable pageable) {
        log.info("/product/list: GET, dto: {}", searchDto);
        log.info("/product/list: GET, pageable={}", pageable);
        Page<ProductResDto> dtoList = productService.productList(searchDto, pageable);

        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK, "상품리스트 정상조회 완료", dtoList);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<?> productDelete(@RequestParam Long id) throws Exception {
        log.info("/product/delete: DELETE, id: {}", id);
        productService.productDelete(id);

        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK, "삭제 완료", null);

        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }




}





















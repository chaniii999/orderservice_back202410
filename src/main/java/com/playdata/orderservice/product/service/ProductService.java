package com.playdata.orderservice.product.service;

import com.playdata.orderservice.product.dto.ProductResDto;
import com.playdata.orderservice.product.dto.ProductSaveReqDto;
import com.playdata.orderservice.product.entity.Product;
import com.playdata.orderservice.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public Product productCreate(ProductSaveReqDto dto) {

        MultipartFile productImage = dto.getProductImage();

        String uniqueFileName
                = UUID.randomUUID() + "_" + productImage.getOriginalFilename();

        File file
                = new File("/Users/stephen/Desktop/develop/upload/" + uniqueFileName);
        try {
            productImage.transferTo(file);
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패!");
        }

        Product product = dto.toEntity();
        product.updateImagePath(uniqueFileName);

        return productRepository.save(product);

    }

    public Page<ProductResDto> productList(Pageable pageable) {
        Page<Product> products = productRepository.findAll(pageable);

        // 클라이언트단에 페이징에 필요한 데이터를 제공하기 위해 Page 객체 자체를 넘기려고 한다.
        // Page 안에 Entity가 들어있으니, 이것을 dto로 변환을 해서 넘기고 싶다. (Page 객체는 유지)
        // map을 통해 Product를 dto로 일괄 변환해서 리턴.
        Page<ProductResDto> productResDtos = products.map(p -> p.fromEntity());

        return productResDtos;
    }
}



















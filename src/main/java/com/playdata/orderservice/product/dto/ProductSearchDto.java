package com.playdata.orderservice.product.dto;

import lombok.*;

@Setter @Getter @ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchDto {

    private String category;
    private String searchName;

}

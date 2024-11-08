package com.playdata.orderservice.ordering.service;

import com.playdata.orderservice.common.auth.TokenUserInfo;
import com.playdata.orderservice.ordering.controller.SseController;
import com.playdata.orderservice.ordering.dto.OrderingListResDto;
import com.playdata.orderservice.ordering.dto.OrderingSaveReqDto;
import com.playdata.orderservice.ordering.entity.OrderDetail;
import com.playdata.orderservice.ordering.entity.OrderStatus;
import com.playdata.orderservice.ordering.entity.Ordering;
import com.playdata.orderservice.ordering.repository.OrderingRepository;
import com.playdata.orderservice.product.entity.Product;
import com.playdata.orderservice.product.repository.ProductRepository;
import com.playdata.orderservice.user.entity.User;
import com.playdata.orderservice.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final SseController sseController;


    public Ordering createOrdering(List<OrderingSaveReqDto> dtoList,
                                   TokenUserInfo userInfo) {
        // Ordering 객체를 생성하기 위해 회원 정보를 얻어오기.
        User user = userRepository.findByEmail(userInfo.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("User Not Found")
        );

        // Ordering(주문) 객체 생성
        Ordering ordering = Ordering.builder()
                .user(user)
                .orderDetails(new ArrayList<>()) // 아직 주문 상세 들어가기 전.
                .build();

        // 주문 상세 내역에 대한 처리를 반복문으로 지정.
        for (OrderingSaveReqDto dto : dtoList) {

            // dto에는 상품 고유 id가 있으니까 그걸 활용해서
            // product 객체를 조회하자.
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(
                    () -> new EntityNotFoundException("Product Not Found")
            );

            // 재고 넉넉하게 있는지 확인.
            int quantity = dto.getProductCount();
            if (product.getStockQuantity() < quantity) {
                throw new IllegalArgumentException("재고 부족!");
            }

            // 재고가 부족하지 않다면 재고 수량을 주문 수량만큼 빼 주자. (setter 직접 작성)
            product.updateStockQuantity(quantity);

            // 주문 상세 내역 엔터티를 생성
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .ordering(ordering)
                    .quantity(quantity)
                    .build();

            // 주문 내역 리스트에 상세 내역을 add 하기.
            // (cascadeType.PERSIST로 세팅했기 때문에 함께 add가 진행될 것.)
            ordering.getOrderDetails().add(orderDetail);
        } // end forEach

        // Ordering 객체를 save하면 내부에 있는 detail 리스트도 함께 INSERT가 진행이 된다.
        Ordering save = orderingRepository.save(ordering);

        // 관리자에게 주문이 생성되었다는 알림을 전송
        sseController.sendOrderMessage(save);

        return save;

    }

    public List<OrderingListResDto> myOrders(TokenUserInfo userInfo) {
        /*
         OrderingListResDto -> OrderDetailDto(static 내부 클래스)
         {
            id: 주문번호,
            userEmail: 주문한 사람 이메일,
            orderStatus: 주문 상태
            orderDetails: [
                {
                    id: 주문상세번호,
                    productName: 상품명,
                    count: 수량
                },
                {
                    id: 주문상세번호,
                    productName: 상품명,
                    count: 수량
                },
                {
                    id: 주문상세번호,
                    productName: 상품명,
                    count: 수량
                }
                ...
            ]
         }
         */
        String userEmail = userInfo.getEmail();
        User user = userRepository.findByEmail(userEmail).orElseThrow(
                () -> new EntityNotFoundException("User Not Found")
        );

        List<Ordering> orderingList = orderingRepository.findByUser(user);

        // Ordering 엔터티를 DTO로 변환하자. 주문 상세에 대한 변환도 필요하다!
        List<OrderingListResDto> dtos = orderingList.stream()
                .map(order -> order.fromEntity())
                .collect(Collectors.toList());

        return dtos;
    }

    public List<OrderingListResDto> orderList() {
        List<Ordering> orderList = orderingRepository.findAll();

        List<OrderingListResDto> dtos = orderList.stream()
                .map(order -> order.fromEntity())
                .collect(Collectors.toList());

        return dtos;
    }

    public Ordering orderCancel(long id) {
        // 상태를 CANCEL로 변경해 주세요.
        // 클라이언트에게는 변경 상태와 주문 id만 넘겨 주세요.
        Ordering ordering = orderingRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("주문 없는데요!")
        );

        ordering.updateStatus(OrderStatus.CANCELED); // 더티 체킹 (save를 하지 않아도 변경을 감지한다.)
        return ordering;
    }
}





















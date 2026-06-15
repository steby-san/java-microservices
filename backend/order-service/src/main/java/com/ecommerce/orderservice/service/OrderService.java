package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.ProductClient;
import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.entity.*;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.exception.*;
import com.ecommerce.orderservice.producer.KafkaProducer;
import com.ecommerce.orderservice.repository.OrderRepository;



import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final ProductClient productClient;

    private final KafkaProducer kafkaProducer;

    @Transactional

    public OrderResponse createOrder(
            Long userId,
            CreateOrderRequest request
    ) {

        BigDecimal totalAmount =
                BigDecimal.ZERO;

        List<OrderItem> orderItems =
                new ArrayList<>();

        for (OrderItemRequest item :
                request.getItems()) {

            ProductResponse product =
                    productClient.getProduct(
                            item.getProductId()
                    );

            if(product == null) {
                throw new ResourceNotFoundException(
                        "Product not found"
                );
            }

            if(product.getStockQuantity()
                    < item.getQuantity()) {

                throw new BadRequestException(
                        "Insufficient stock"
                );
            }

            productClient.decreaseStock(
                    item.getProductId(),
                    item.getQuantity()
            );

            BigDecimal lineTotal =
                    product.getPrice().multiply(
                            BigDecimal.valueOf(
                                    item.getQuantity()
                            )
                    );

            totalAmount =
                    totalAmount.add(lineTotal);

            OrderItem orderItem =
                    OrderItem.builder()
                            .productId(
                                    product.getId()
                            )
                            .quantity(
                                    item.getQuantity()
                            )
                            .price(
                                    product.getPrice()
                            )
                            .build();

            orderItems.add(orderItem);
        }

        Order order =
                Order.builder()
                        .userId(userId)
                        .totalAmount(totalAmount)
                        .status(
                                OrderStatus.CREATED
                        )
                        .items(orderItems)
                        .build();

        orderItems.forEach(
                item -> item.setOrder(order)
        );

        Order savedOrder =
                orderRepository.save(order);

        kafkaProducer.sendOrderCreatedEvent(
                new OrderCreatedEvent(
                        savedOrder.getId(),
                        userId,
                        totalAmount
                )
        );

        return convertToResponse(
                savedOrder
        );
    }

    public OrderResponse fallbackCreateOrder(
            Long userId,
            CreateOrderRequest request,
            Exception ex
    ) {

        throw new RuntimeException(
                "Dịch vụ sản phẩm đang bảo trì, vui lòng thử lại sau."
        );
    }

    public List<OrderResponse>
    getOrdersByUser(Long userId) {

        return orderRepository
                .findByUserId(userId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(
            Long id
    ) {

        Order order =
                orderRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found"
                                )
                        );

        return convertToResponse(order);
    }

    private OrderResponse
    convertToResponse(Order order) {

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(
                        order.getTotalAmount()
                )
                .status(
                        order.getStatus().name()
                )
                .createdAt(
                        order.getCreatedAt()
                )
                .items(
                        order.getItems()
                                .stream()
                                .map(item ->
                                        OrderItemResponse
                                                .builder()
                                                .productId(
                                                        item.getProductId()
                                                )
                                                .quantity(
                                                        item.getQuantity()
                                                )
                                                .price(
                                                        item.getPrice()
                                                )
                                                .build()
                                )
                                .toList()
                )
                .build();
    }
}
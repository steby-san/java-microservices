package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.*;
import com.ecommerce.orderservice.service.OrderService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponse createOrder(

            @RequestHeader("X-User-Id")
            Long userId,

            @RequestBody
            CreateOrderRequest request
    ) {

        return orderService.createOrder(
                userId,
                request
        );
    }

    @GetMapping("/my-orders")
    public List<OrderResponse>
    myOrders(

            @RequestHeader("X-User-Id")
            Long userId
    ) {

        return orderService
                .getOrdersByUser(userId);
    }

    @GetMapping("/{id}")
    public OrderResponse getById(
            @PathVariable Long id
    ) {

        return orderService
                .getOrderById(id);
    }
}
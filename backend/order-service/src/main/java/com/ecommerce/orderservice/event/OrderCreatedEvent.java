package com.ecommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreatedEvent {

    private Long orderId;

    private Long userId;

    private BigDecimal totalAmount;
}
package com.ecommerce.orderservice.producer;

import com.ecommerce.orderservice.event.OrderCreatedEvent;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

    public void sendOrderCreatedEvent(
            OrderCreatedEvent event
    ) {
        System.out.println(
                "Order Created Event: " +
                        event.getOrderId()
        );
    }
}
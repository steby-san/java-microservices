package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.entity.Product;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ProductConsumer {

    @KafkaListener(
            topics = "product-topic",
            groupId = "product-group")
    public void receive(Product product) {

        System.out.println("========== PRODUCT RECEIVED ==========");
        System.out.println(product);
        System.out.println("======================================");

    }
}
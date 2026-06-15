package com.ecommerce.productservice.kafka;

import com.ecommerce.productservice.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductProducer {

    private static final String TOPIC = "product-topic";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProduct(Product product) {
        kafkaTemplate.send(TOPIC, product);
        System.out.println("Kafka Producer: " + product.getName());
    }
}
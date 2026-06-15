package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.dto.ProductResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public ProductResponse getProduct(String productId) {

        return restTemplate.getForObject(
                "http://localhost:8082/api/products/" + productId,
                ProductResponse.class
        );
    }

    public void decreaseStock(
            String productId,
            Integer quantity
    ) {

        restTemplate.put(
                "http://localhost:8082/api/products/"
                        + productId
                        + "/decrease-stock?quantity="
                        + quantity,
                null
        );
    }
}
package com.ecommerce.productservice.config;

import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner initDatabase(ProductRepository repository) {
        return args -> {

            System.out.println("========== DATA SEEDER START ==========");

            long count = repository.count();

            System.out.println("Current count = " + count);

            if (count == 0) {

                Product product = Product.builder()
                        .name("Test Product")
                        .description("Test")
                        .price(BigDecimal.valueOf(1000))
                        .stockQuantity(10)
                        .category("Test")
                        .build();

                repository.save(product);

                System.out.println("Saved Product");

                System.out.println("New Count = " + repository.count());
            }

            System.out.println("========== DATA SEEDER END ==========");
        };
    }
}
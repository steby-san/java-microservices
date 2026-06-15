package com.ecommerce.productservice.service;

import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.kafka.ProductProducer;
import com.ecommerce.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private ProductProducer producer;

    @Cacheable(value = "products")
    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    @Cacheable(value = "product", key = "#id")
    public Product getProductById(String id) {
        return repository.findById(id).orElse(null);
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(Product product) {

        Product saved = repository.save(product);

        producer.sendProduct(saved);

        return saved;
    }

    @CachePut(value = "product", key = "#id")
    @CacheEvict(value = "products", allEntries = true)
    public Product updateProduct(String id, Product product) {

        Product oldProduct = repository.findById(id).orElse(null);

        if (oldProduct == null) {
            return null;
        }

        oldProduct.setName(product.getName());
        oldProduct.setDescription(product.getDescription());
        oldProduct.setPrice(product.getPrice());
        oldProduct.setStockQuantity(product.getStockQuantity());
        oldProduct.setCategory(product.getCategory());

        Product updated = repository.save(oldProduct);

        producer.sendProduct(updated);

        return updated;
    }

    @CacheEvict(value = {"products", "product"}, allEntries = true)
    public void deleteProduct(String id) {

        Product product = repository.findById(id).orElse(null);

        repository.deleteById(id);

        if (product != null) {
            producer.sendProduct(product);
        }
    }

    @CachePut(value = "product", key = "#id")
    @CacheEvict(value = "products", allEntries = true)
    public Product reduceStock(String id, int quantity) {

        Product product = repository.findById(id).orElse(null);

        if (product == null) {
            return null;
        }

        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException("Not enough stock");
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);

        Product updated = repository.save(product);

        producer.sendProduct(updated);

        return updated;
    }
}
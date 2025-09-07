package com.ikea.repository;

import com.ikea.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByContainArticles_ArtId(String artId);

    boolean existsByName(String name);
}

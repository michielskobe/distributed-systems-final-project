package com.frontend.dsgt.service;

import com.frontend.dsgt.model.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.javatuples.*;

import java.util.*;

@Service
public class ProductAggregationService {

    private final RestTemplate restTemplate;

    private final List<Pair<String,String>> endpoints = List.of(
            Pair.with("bicycles","https://crank-wankers.francecentral.cloudapp.azure.com/Crank-Wankers/list_products"),
            Pair.with("ledstrips","https://rgbeast.francecentral.cloudapp.azure.com/RGBeast/list_products"),
            Pair.with("batteries","https://battery-bastards.francecentral.cloudapp.azure.com/Battery-Bastards/list_products")
    );

    @Value("${external.api.key}")
    private String apiKey;

    public ProductAggregationService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public List<Product> fetchAllProducts() {
        List<Product> result = new ArrayList<>();
        for (Pair<String,String> endpoint : endpoints) {
            String category = endpoint.getValue0();
            String url = endpoint.getValue1();
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("x-api-key", apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
                List<Map<String, String>> list = (List<Map<String, String>>) response.getBody().get("response");

                for (Map<String, String> item : list) {
                    Product p = new Product();
                    p.setId(item.get("id"));
                    p.setName(item.get("name"));
                    p.setQuantity(item.get("quantity"));
                    p.setDescription(item.get("description"));
                    p.setCategory(category);
                    result.add(p);
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch from " + url + ": " + e.getMessage());
            }
        }
        return result;
    }

    public List<Product> fetchProductCategory(String category) {
        List<Product> result = new ArrayList<>();
        for (Pair<String,String> endpoint : endpoints) {
            if (endpoint.getValue0().equals(category)){
                String url = endpoint.getValue1();
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("x-api-key", apiKey);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Void> entity = new HttpEntity<>(headers);

                    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
                    List<Map<String, String>> list = (List<Map<String, String>>) response.getBody().get("response");

                    for (Map<String, String> item : list) {
                        Product p = new Product();
                        p.setId(item.get("id"));
                        p.setName(item.get("name"));
                        p.setQuantity(item.get("quantity"));
                        p.setDescription(item.get("description"));
                        p.setImageUrl(item.get("image_url"));
                        p.setCategory(category);
                        result.add(p);

                    }
                } catch (Exception e) {
                    System.err.println("Failed to fetch from " + url + ": " + e.getMessage());
                }
            }
        }
        return result;
    }
}

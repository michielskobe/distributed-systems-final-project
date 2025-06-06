package com.frontend.dsgt;

import com.frontend.dsgt.service.ProductAggregationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProductController {

    private final ProductAggregationService productService;

    public ProductController(ProductAggregationService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public String showProducts(Model model) {
        model.addAttribute("bicycles", productService.fetchProductCategory("bicycles"));
        model.addAttribute("ledstrips", productService.fetchProductCategory("ledstrips"));
        model.addAttribute("batteries", productService.fetchProductCategory("batteries"));

        model.addAttribute("pageTitle", "Products");

        return "products";
    }
}

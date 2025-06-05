package com.frontend.dsgt;

import com.frontend.dsgt.service.ProductAggregationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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
    public String showProducts(Model model, @AuthenticationPrincipal OidcUser user) {
        if (user != null) {
            model.addAttribute("profile", user.getClaims());
        }

        model.addAttribute("bicycles", productService.fetchProductCategory("bicycles"));
        model.addAttribute("ledstrips", productService.fetchProductCategory("ledstrips"));
        model.addAttribute("batteries", productService.fetchProductCategory("batteries"));

        model.addAttribute("pageTitle", "Products");
        model.addAttribute("shopName", "The Biker Boys");

        return "products";
    }
}

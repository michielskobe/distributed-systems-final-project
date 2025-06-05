package com.frontend.dsgt;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;

@Controller
public class ErrorController {

    @GetMapping("/error/access-denied")
    public String accessDenied(Model model, @AuthenticationPrincipal OidcUser user) {

        model.addAttribute("profile", user.getClaims());

        model.addAttribute("pageTitle", "Access Denied");
        model.addAttribute("shopName", "The Biker Boys");

        return "error/access-denied"; // Points to access-denied.html
    }
}


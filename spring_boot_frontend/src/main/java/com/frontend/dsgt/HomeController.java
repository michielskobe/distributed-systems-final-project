package com.frontend.dsgt;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Model model, @AuthenticationPrincipal OidcUser user) {
	//var authentication = SecurityContextHolder.getContext().getAuthentication();
	//var authorities = authentication.getAuthorities().stream()
	//        .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

		if (user != null) {
			model.addAttribute("profile", user.getClaims());
		}

		model.addAttribute("pageTitle", "Home");
		model.addAttribute("shopName", "The Biker Boys");
	return "index";
	}

}

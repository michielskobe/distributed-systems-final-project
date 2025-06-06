package com.frontend.dsgt;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalModelAttributes {

    /**
     * This method runs before every controller (and also before error pages are rendered).
     * It always adds "shopName" and, if the user is logged in, a "profile" attribute.
     */
    @ModelAttribute
    public void populateCommonAttributes(Model model,
                                         @AuthenticationPrincipal OidcUser oidcUser) {
        // 1) shopName is always set
        model.addAttribute("shopName", "The Biker Boys");

        // 2) If an OidcUser is present (i.e. authenticated), expose their claims as "profile"
        if (oidcUser != null) {
            model.addAttribute("profile", oidcUser.getClaims());
        }

        // 3) A fallback pageTitle—templates can override via th:with
        //    We set it to blank so that layout/default doesn't show "null".
        model.addAttribute("pageTitle", "Website");
    }
}

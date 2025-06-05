package com.frontend.dsgt;

import com.frontend.dsgt.model.Order;
import com.frontend.dsgt.model.Product;
import com.frontend.dsgt.service.ProductAggregationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@SessionAttributes({"order", "bicycles", "ledstrips", "batteries", "fetchError"})
public class OrderController {

    private final ProductAggregationService productService;

    public OrderController(ProductAggregationService productService) {
        this.productService = productService;
    }

    /**
     * Create a new Order once per session (if not already present).
     */
    @ModelAttribute("order")
    public Order order() {
        return new Order();
    }

    /**
     * Before any /order/** handler, attempt to fetch products ONCE.
     * If fetch fails, set fetchError=true so the template shows an error.
     * Otherwise store fetched lists in session attributes.
     */
    @ModelAttribute
    public void fetchProductsIfNeeded(Model model) {
        Boolean fetchError = (Boolean) model.getAttribute("fetchError");
        List<Product> bikes  = (List<Product>) model.getAttribute("bicycles");
        List<Product> leds   = (List<Product>) model.getAttribute("ledstrips");
        List<Product> bats   = (List<Product>) model.getAttribute("batteries");

        // If no lists in session yet OR last time there was an error, try fetching:
        if (fetchError == null || Boolean.TRUE.equals(fetchError) || bikes == null) {
            try {
                List<Product> fetchedBikes  = productService.fetchProductCategory("bicycles");
                List<Product> fetchedLeds   = productService.fetchProductCategory("ledstrips");
                List<Product> fetchedBats   = productService.fetchProductCategory("batteries");

                model.addAttribute("bicycles", fetchedBikes);
                model.addAttribute("ledstrips", fetchedLeds);
                model.addAttribute("batteries", fetchedBats);
                model.addAttribute("fetchError", false);
            } catch (Exception e) {
                model.addAttribute("fetchError", true);
            }
        }
    }

    // ——————————————————————————————————————————————
    // Step 1: Choose Bicycle
    @GetMapping("/order/bicycle")
    public String chooseBike(
            @ModelAttribute("order") Order order,
            Model model,
            @AuthenticationPrincipal OidcUser user
    ) {
        model.addAttribute("profile", user.getClaims());
        model.addAttribute("pageTitle", "Order - Bicycle");
        model.addAttribute("shopName", "The Biker Boys");

        // For the template:
        model.addAttribute("step", 1);
        model.addAttribute("stepLabel", "Select Your Bicycle");
        model.addAttribute("stepDescription", "Choose the bike(s) and their quantities.");

        return "order/bicycle";
    }

    @PostMapping("/order/bicycle")
    public String saveBike(
            @ModelAttribute("order") Order order,
            @RequestParam Map<String, String> params,
            @RequestParam("action") String action
    ) {
        // Clear previous bicycle quantities
        order.getBicycleQuantities().clear();


        // Read all params named "bikeQty_<productId>"
        params.forEach((key, value) -> {
            if (key.startsWith("bikeQty_")) {
                String productId = key.substring("bikeQty_".length());
                try {
                    int qty = Integer.parseInt(value);
                    if (qty > 0) {
                        order.getBicycleQuantities().put(productId, qty);
                    }
                } catch (NumberFormatException ignored) { }
            }
        });

        // If “Review” clicked
        if ("review".equals(action)) {
            return "redirect:/order/summary";
        }
        // Otherwise go to LED step
        return "redirect:/order/led";
    }

    // ——————————————————————————————————————————————
    // Step 2: Choose LED
    @GetMapping("/order/led")
    public String chooseLed(
            @ModelAttribute("order") Order order,
            Model model,
            @AuthenticationPrincipal OidcUser user
    ) {
        model.addAttribute("profile", user.getClaims());
        model.addAttribute("pageTitle", "Order - LED Strip");
        model.addAttribute("shopName", "The Biker Boys");

        model.addAttribute("step", 2);
        model.addAttribute("stepLabel", "Select Your LED Strip");
        model.addAttribute("stepDescription", "Choose LED strip(s) and their quantities.");

        return "order/led";
    }

    @PostMapping("/order/led")
    public String saveLed(
            @ModelAttribute("order") Order order,
            @RequestParam Map<String, String> params,
            @RequestParam("action") String action
    ) {
        // Clear previous LED quantities
        order.getLedQuantities().clear();

        // Read "ledQty_<productId>"
        params.forEach((key, value) -> {
            if (key.startsWith("ledQty_")) {
                String productId = key.substring("ledQty_".length());
                try {
                    int qty = Integer.parseInt(value);
                    if (qty > 0) {
                        order.getLedQuantities().put(productId, qty);
                    }
                } catch (NumberFormatException ignored) { }
            }
        });

        if ("review".equals(action)) {
            return "redirect:/order/summary";
        }
        return "redirect:/order/battery";
    }

    // ——————————————————————————————————————————————
    // Step 3: Choose Battery
    @GetMapping("/order/battery")
    public String chooseBattery(
            @ModelAttribute("order") Order order,
            Model model,
            @AuthenticationPrincipal OidcUser user
    ) {
        model.addAttribute("profile", user.getClaims());
        model.addAttribute("pageTitle", "Order - Battery");
        model.addAttribute("shopName", "The Biker Boys");

        model.addAttribute("step", 3);
        model.addAttribute("stepLabel", "Select Your Battery");
        model.addAttribute("stepDescription", "Choose battery(ies) and their quantities.");

        return "order/battery";
    }

    @PostMapping("/order/battery")
    public String saveBattery(
            @ModelAttribute("order") Order order,
            @RequestParam Map<String, String> params,
            @RequestParam("action") String action
    ) {
        // Clear previous battery quantities
        order.getBatteryQuantities().clear();

        // Read "batQty_<productId>"
        params.forEach((key, value) -> {
            if (key.startsWith("batQty_")) {
                String productId = key.substring("batQty_".length());
                try {
                    int qty = Integer.parseInt(value);
                    if (qty > 0) {
                        order.getBatteryQuantities().put(productId, qty);
                    }
                } catch (NumberFormatException ignored) { }
            }
        });

        return "redirect:/order/summary";
    }

    // ——————————————————————————————————————————————
    // Step 4: Summary
    @GetMapping("/order/summary")
    public String summary(
            @ModelAttribute("order") Order order,
            Model model,
            @AuthenticationPrincipal OidcUser user
    ) {
        model.addAttribute("profile", user.getClaims());
        model.addAttribute("pageTitle", "Order - Summary");
        model.addAttribute("shopName", "The Biker Boys");

        // If somehow they reach summary with no selections, bounce them to first step
        //if (!order.hasAnySelection()) {
        //    return "redirect:/order/bicycle";
        //}
        return "order/summary";
    }

    @PostMapping("/order/complete")
    public String complete(
            @ModelAttribute("order") Order order,
            SessionStatus status,
            RedirectAttributes redirectAttrs
    ) {
        // Add a one‐time flash message
        redirectAttrs.addFlashAttribute("successMessage", "Your order has been placed successfully!");

        // TODO: Persist order to your database

        // Clear session attributes (order + product lists + fetchError)
        status.setComplete();
        return "redirect:/";
    }

    // ——————————————————————————————————————————————
    // Cancel Button: Clears everything, then sends back to step 1
    @PostMapping("/order/cancel")
    public String cancelOrder(SessionStatus status) {
        status.setComplete();
        return "redirect:/";
    }
}

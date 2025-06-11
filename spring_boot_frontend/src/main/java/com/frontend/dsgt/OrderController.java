package com.frontend.dsgt;

import com.frontend.dsgt.model.Order;
import com.frontend.dsgt.model.Product;
import com.frontend.dsgt.service.ProductAggregationService;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;

import com.frontend.dsgt.model.OrderEntity;
import com.frontend.dsgt.model.OrderItemEntity;
import com.frontend.dsgt.repository.OrderRepository;
import com.azure.storage.queue.QueueClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

@Controller
@SessionAttributes({"order", "bicycles", "ledstrips", "batteries", "fetchBikeError", "fetchLedError", "fetchBatError"})
public class OrderController {

    private final ProductAggregationService productService;
    private final OrderRepository orderRepo;
    private final QueueClient queueClient;

    public OrderController(ProductAggregationService productService, OrderRepository orderRepo, QueueClient queueClient) {
        this.productService = productService;
        this.orderRepo = orderRepo;
        this.queueClient = queueClient;
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
        Boolean fetchBikeError = (Boolean) model.getAttribute("fetchBikeError");
        Boolean fetchLedError = (Boolean) model.getAttribute("fetchLedError");
        Boolean fetchBatError = (Boolean) model.getAttribute("fetchBatError");
        List<Product> bikes  = (List<Product>) model.getAttribute("bicycles");
        List<Product> leds   = (List<Product>) model.getAttribute("ledstrips");
        List<Product> bats   = (List<Product>) model.getAttribute("batteries");

        // If no lists in session yet OR last time there was an error, try fetching:
        if (fetchBikeError == null || Boolean.TRUE.equals(fetchBikeError) || bikes == null || bikes.size() == 0) {
            try {
                List<Product> fetchedBikes  = productService.fetchProductCategory("bicycles");
                model.addAttribute("bicycles", fetchedBikes);
                if (fetchedBikes.size() > 0) {
                    model.addAttribute("fetchBikeError", false);
                }
                else {
                    model.addAttribute("fetchBikeError", true);
                }
            } catch (Exception e) {
                model.addAttribute("fetchBikeError", true);
            }
        }

        if (fetchLedError == null || Boolean.TRUE.equals(fetchLedError) || leds == null || leds.size() == 0) {
            try {
                List<Product> fetchedLeds   = productService.fetchProductCategory("ledstrips");
                model.addAttribute("ledstrips", fetchedLeds);
                if (fetchedLeds.size() > 0) {
                    model.addAttribute("fetchLedError", false);
                }
                else {
                    model.addAttribute("fetchLedError", true);
                }
            } catch (Exception e) {
                model.addAttribute("fetchLedError", true);
            }
        }

        if (fetchBatError == null || Boolean.TRUE.equals(fetchBatError) || bats == null || bats.size() == 0) {
            try {
                List<Product> fetchedBats   = productService.fetchProductCategory("batteries");
                model.addAttribute("batteries", fetchedBats);
                if (fetchedBats.size() > 0) {
                    model.addAttribute("fetchBatError", false);
                }
                else {
                    model.addAttribute("fetchBatError", true);
                }
            } catch (Exception e) {
                model.addAttribute("fetchBatError", true);
            }
        }
    }

    // ——————————————————————————————————————————————
    // Step 1: Choose Bicycle
    @GetMapping("/order/bicycle")
    public String chooseBike(
            @ModelAttribute("order") Order order,
            Model model
    ) {
        model.addAttribute("pageTitle", "Order - Bicycle");

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
            Model model
    ) {
        model.addAttribute("pageTitle", "Order - LED Strip");

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
            Model model
    ) {
        model.addAttribute("pageTitle", "Order - Battery");

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
            Model model
    ) {
        model.addAttribute("pageTitle", "Order - Summary");
        return "order/summary";
    }

    @PostMapping("/order/complete")
    public String complete(
            @ModelAttribute("order") Order order,
            SessionStatus status,
            RedirectAttributes redirectAttrs,
            @AuthenticationPrincipal OidcUser user) {

        // 1) Save to DB
        OrderEntity oe = new OrderEntity();
        oe.setUserEmail(user.getClaims().get("email").toString());

        // 1) Bicycle (supplier_id = 2)
        for (Map.Entry<String,Integer> entry : order.getBicycleQuantities().entrySet()) {
            String prodId = entry.getKey();
            int qty = entry.getValue();
            OrderItemEntity item = new OrderItemEntity();
            item.setSupplierId(2);
            item.setProductId(prodId);
            item.setAmount(qty);
            oe.addItem(item);
        }

        // 2) LED (supplier_id = 1)
        for (Map.Entry<String,Integer> entry : order.getLedQuantities().entrySet()) {
            String prodId = entry.getKey();
            int qty = entry.getValue();
            OrderItemEntity item = new OrderItemEntity();
            item.setSupplierId(1);
            item.setProductId(prodId);
            item.setAmount(qty);
            oe.addItem(item);
        }

        // 3) Battery (supplier_id = 3)
        for (Map.Entry<String,Integer> entry : order.getBatteryQuantities().entrySet()) {
            String prodId = entry.getKey();
            int qty = entry.getValue();
            OrderItemEntity item = new OrderItemEntity();
            item.setSupplierId(3);
            item.setProductId(prodId);
            item.setAmount(qty);
            oe.addItem(item);
        }

        oe.setStatus("NEW");
        oe = orderRepo.save(oe);  // persist

        // 2) Push to Azure Queue
        String msg = String.format("%d", oe.getId());
        queueClient.createIfNotExists();
        queueClient.sendMessage(msg);

        // 3) Flash and clear
        redirectAttrs.addFlashAttribute("successMessage", "Your order has been placed successfully!");
        status.setComplete();
        return "redirect:/";
    }

    // ——————————————————————————————————————————————
    // Cancel Button: Clears everything, then sends back to home page
    @PostMapping("/order/cancel")
    public String cancelOrder(SessionStatus status) {
        status.setComplete();
        return "redirect:/";
    }


    /**
     * “My Orders” – any authenticated user can see their own orders.
     */
    @GetMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public String myOrders(Model model, @AuthenticationPrincipal OidcUser user,
                           @RequestParam(defaultValue = "0") int page) {
        String email = user.getClaims().get("email").toString();
        Page<OrderEntity> pg = orderRepo.findByUserEmail(email, PageRequest.of(page, 18, Sort.by("createdAt").descending()));
        model.addAttribute("ordersPage", pg);
        return "orders";
    }

    /**
     * “All Orders” – only managers can see every order.
     */
    @GetMapping("/admin/orders")
    @PreAuthorize("hasAuthority('Manager')")
    public String allOrders(Model model,
                            @RequestParam(defaultValue = "0") int page) {
        Page<OrderEntity> pg = orderRepo.findAll(PageRequest.of(page, 18, Sort.by("createdAt").descending()));
        model.addAttribute("ordersPage", pg);
        return "admin-orders";
    }
}

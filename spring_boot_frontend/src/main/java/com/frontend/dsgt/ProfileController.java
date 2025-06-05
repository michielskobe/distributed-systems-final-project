package com.frontend.dsgt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Controller
public class ProfileController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final static ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        Map<String, Object> claims = oidcUser.getClaims();
        model.addAttribute("profile", claims);
        model.addAttribute("profileJson", claimsToJson(claims));

        // Compute a nicely formatted "Last Updated" string
        String formattedUpdated = computeFormattedUpdatedAt(claims);
        model.addAttribute("formattedUpdatedAt", formattedUpdated);

        model.addAttribute("pageTitle", "Profile");
        model.addAttribute("shopName", "The Biker Boys");
        return "profile";
    }

    private String claimsToJson(Map<String, Object> claims) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(claims);
        } catch (JsonProcessingException jpe) {
            log.error("Error parsing claims to JSON", jpe);
        }
        return "Error parsing claims to JSON.";
    }

    /**
     * Attempts to parse the 'updated_at' claim in one of two forms:
     * 1) an ISO-8601 string (e.g. "2025-06-05T16:30:34.781Z"), or
     * 2) a numeric seconds-since-epoch (possibly fractional).
     * Converts to LocalDateTime in Europe/Brussels and formats "yyyy-MM-dd HH:mm".
     * Returns "Unknown" if parsing fails.
     */
    private String computeFormattedUpdatedAt(Map<String, Object> claims) {
        Object raw = claims.get("updated_at");
        if (raw == null) {
            return "Unknown";
        }

        Instant instant;
        try {
            if (raw instanceof String) {
                // If it's an ISO-8601 timestamp:
                String str = (String) raw;
                // Attempt ISO-8601 parse
                instant = Instant.parse(str);
            } else if (raw instanceof Number) {
                // If it's a numeric seconds-since-epoch (Double, BigDecimal, etc.)
                double seconds = ((Number) raw).doubleValue();
                long millis = (long) (seconds * 1000.0);
                instant = Instant.ofEpochMilli(millis);
            } else {
                // Try parsing the object's toString() as either ISO or double
                String text = raw.toString();
                if (text.matches("\\d+\\.?\\d*")) {
                    // purely numeric
                    double seconds = Double.parseDouble(text);
                    long millis = (long) (seconds * 1000.0);
                    instant = Instant.ofEpochMilli(millis);
                } else {
                    // fallback: treat as ISO
                    instant = Instant.parse(text);
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse updated_at claim: {}", raw, e);
            return "Unknown";
        }

        // Convert to Brussels local date/time
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.of("Europe/Brussels"));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return ldt.format(fmt);
    }
}

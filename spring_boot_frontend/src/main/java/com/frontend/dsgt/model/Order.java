package com.frontend.dsgt.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Order implements Serializable {
    // Maps productId → quantity (>0 only)
    private Map<String, Integer> bicycleQuantities = new HashMap<>();
    private Map<String, Integer> ledQuantities     = new HashMap<>();
    private Map<String, Integer> batteryQuantities = new HashMap<>();

    public Map<String, Integer> getBicycleQuantities() {
        return bicycleQuantities;
    }
    public void setBicycleQuantities(Map<String, Integer> bicycleQuantities) {
        this.bicycleQuantities = bicycleQuantities;
    }

    public Map<String, Integer> getLedQuantities() {
        return ledQuantities;
    }
    public void setLedQuantities(Map<String, Integer> ledQuantities) {
        this.ledQuantities = ledQuantities;
    }

    public Map<String, Integer> getBatteryQuantities() {
        return batteryQuantities;
    }
    public void setBatteryQuantities(Map<String, Integer> batteryQuantities) {
        this.batteryQuantities = batteryQuantities;
    }

    /** Returns true if at least one product in any category has quantity>0. */
    public boolean hasAnySelection() {
        return bicycleQuantities.values().stream().anyMatch(q -> q != null && q > 0)
                || ledQuantities.values().stream().anyMatch(q -> q != null && q > 0)
                || batteryQuantities.values().stream().anyMatch(q -> q != null && q > 0);
    }
}

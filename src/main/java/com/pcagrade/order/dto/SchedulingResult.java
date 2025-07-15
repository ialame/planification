package com.pcagrade.order.dto;

import java.util.ArrayList;
import java.util.List;

public class SchedulingResult {
    private boolean success;
    private String message;
    private int scheduledOrdersCount;
    private List<String> warnings;

    public SchedulingResult(boolean success, String message, int scheduledOrdersCount) {
        this(success, message, scheduledOrdersCount, new ArrayList<>());
    }

    public SchedulingResult(boolean success, String message, int scheduledOrdersCount, List<String> warnings) {
        this.success = success;
        this.message = message;
        this.scheduledOrdersCount = scheduledOrdersCount;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }

    // Getters et setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getScheduledOrdersCount() { return scheduledOrdersCount; }
    public void setScheduledOrdersCount(int scheduledOrdersCount) { this.scheduledOrdersCount = scheduledOrdersCount; }

    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}

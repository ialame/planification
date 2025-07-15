package com.pcagrade.order.dto;

import java.time.LocalDate;
import java.util.Map;

public class DashboardStats {
    private Long totalOrders;
    private Long pendingOrders;
    private Long scheduledOrders;
    private Long completedOrders;
    private Long overdueOrders;
    private Long activeEmployees;
    private Double averageProcessingTimeHours;
    private Map<LocalDate, Long> dailyOrdersChart;
    private Map<String, Long> ordersByPriority;
    private Map<String, Integer> employeeWorkload;

    // Constructeur par défaut
    public DashboardStats() {}

    // Getters et Setters
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }

    public Long getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(Long pendingOrders) { this.pendingOrders = pendingOrders; }

    public Long getScheduledOrders() { return scheduledOrders; }
    public void setScheduledOrders(Long scheduledOrders) { this.scheduledOrders = scheduledOrders; }

    public Long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Long completedOrders) { this.completedOrders = completedOrders; }

    public Long getOverdueOrders() { return overdueOrders; }
    public void setOverdueOrders(Long overdueOrders) { this.overdueOrders = overdueOrders; }

    public Long getActiveEmployees() { return activeEmployees; }
    public void setActiveEmployees(Long activeEmployees) { this.activeEmployees = activeEmployees; }

    public Double getAverageProcessingTimeHours() { return averageProcessingTimeHours; }
    public void setAverageProcessingTimeHours(Double averageProcessingTimeHours) {
        this.averageProcessingTimeHours = averageProcessingTimeHours;
    }

    public Map<LocalDate, Long> getDailyOrdersChart() { return dailyOrdersChart; }
    public void setDailyOrdersChart(Map<LocalDate, Long> dailyOrdersChart) {
        this.dailyOrdersChart = dailyOrdersChart;
    }

    public Map<String, Long> getOrdersByPriority() { return ordersByPriority; }
    public void setOrdersByPriority(Map<String, Long> ordersByPriority) {
        this.ordersByPriority = ordersByPriority;
    }

    public Map<String, Integer> getEmployeeWorkload() { return employeeWorkload; }
    public void setEmployeeWorkload(Map<String, Integer> employeeWorkload) {
        this.employeeWorkload = employeeWorkload;
    }
}
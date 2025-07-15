package com.pcagrade.order.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

@RestController
public class DashboardController {

    @GetMapping("/api/dashboard/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Dashboard API is working");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/dashboard/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCommandes", 25);
        stats.put("commandesEnAttente", 8);
        stats.put("commandesEnCours", 5);
        stats.put("commandesTerminees", 12);
        stats.put("employesActifs", 3);
        stats.put("status", "success");
        return ResponseEntity.ok(stats);
    }


}

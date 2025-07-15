package com.pcagrade.order.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ConfigController {

    @Value("${spring.profiles.active:default}")
    private String activeProfiles;

    @Value("${ulid.debug:false}")
    private boolean ulidDebug;

    @GetMapping("/config/info")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("activeProfiles", activeProfiles);
        config.put("ulidDebug", ulidDebug);
        config.put("environment", System.getProperty("java.version"));
        return config;
    }
}


package com.pcagrade.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// ❌ SUPPRIMÉ : import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
// ❌ SUPPRIMÉ : @CrossOrigin(origins = "*")
public class OrderManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderManagementApplication.class, args);

        System.out.println("🚀 Application démarrée avec CORS configuré");
        System.out.println("🌐 API disponible sur: http://localhost:8080");
        System.out.println("🔧 CORS: allowCredentials=false, originPatterns=*");
    }
}
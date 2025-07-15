package com.pcagrade.order.entity;

public enum PrioriteCommande {
    HAUTE,    // 1 semaine  - prix >= 1000€
    MOYENNE,  // 2 semaines - prix >= 500€
    BASSE     // 4 semaines - prix < 500€
}
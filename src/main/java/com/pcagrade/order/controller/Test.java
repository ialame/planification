package com.pcagrade.order.controller;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.pcagrade.order.entity.Commande;


import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        // Création
        Ulid id = UlidCreator.getMonotonicUlid();
        Commande order = new Commande();  // Constructeur par défaut
        order.setNumCommande("CMD-001");
        order.setNbDescellements(5);
// Accès
        System.out.println("ID: " + order.getId());
        System.out.println("Créé le: " + order.getDateDebutTraitement());

// Comparaison
        List<Commande> orders = new ArrayList<>(); // au lieu de List<Commande>
        orders.sort((c1, c2) -> c1.getId().compareTo(c2.getId())); // Tri chronologique
    }
}

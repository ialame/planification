package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;  // ✅ UUID

@Repository
public interface CommandeRepository extends JpaRepository<Commande, UUID> {  // ✅ UUID

    Optional<Commande> findByNumeroCommande(String numeroCommande);

    @Query("SELECT c FROM Commande c WHERE c.status = :status")
    List<Commande> findCommandesNonAssignees(@Param("status") int status);

    long countByStatus(int status);

    @Query("SELECT c FROM Commande c WHERE c.status IN (1, 2) ORDER BY c.date ASC")
    List<Commande> findCommandesATraiter();
}
package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Planification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;  // ✅ UUID au lieu de Ulid

@Repository
public interface PlanificationRepository extends JpaRepository<Planification, UUID> {  // ✅ UUID

    // Recherche par employé et période
    @Query("SELECT p FROM Planification p WHERE p.employeId = :employeId " +
            "AND p.datePlanification BETWEEN :debut AND :fin " +
            "ORDER BY p.datePlanification, p.heureDebut")
    List<Planification> findByEmployeIdAndDatePlanificationBetween(
            @Param("employeId") UUID employeId,  // ✅ UUID
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    // Recherche par période
    @Query("SELECT p FROM Planification p WHERE p.datePlanification BETWEEN :debut AND :fin " +
            "ORDER BY p.datePlanification, p.heureDebut")
    List<Planification> findByDatePlanificationBetween(
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    // Recherche par commande
    @Query("SELECT p FROM Planification p WHERE p.orderId = :orderId")
    List<Planification> findByOrderId(@Param("orderId") UUID orderId);  // ✅ UUID

    // Planifications non terminées
    @Query("SELECT p FROM Planification p WHERE p.terminee = false " +
            "ORDER BY p.datePlanification, p.heureDebut")
    List<Planification> findByTermineeFalse();

    // Planifications par employé
    @Query("SELECT p FROM Planification p WHERE p.employeId = :employeId " +
            "ORDER BY p.datePlanification DESC, p.heureDebut DESC")
    List<Planification> findByEmployeId(@Param("employeId") UUID employeId);  // ✅ UUID
}

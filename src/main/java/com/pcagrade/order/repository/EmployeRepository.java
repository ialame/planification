package com.pcagrade.order.repository;

import com.pcagrade.order.entity.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;  // ✅ UUID au lieu de Ulid

@Repository
public interface EmployeRepository extends JpaRepository<Employe, UUID> {  // ✅ UUID

    // ✅ Méthodes corrigées selon les erreurs
    List<Employe> findByActifTrue();

    Optional<Employe> findByEmail(String email);

    // ✅ AJOUT : méthode manquante findByNom
    List<Employe> findByNom(String nom);

    // ✅ AJOUT : méthode manquante findEmployesActifs
    @Query("SELECT e FROM Employe e WHERE e.actif = true ORDER BY e.nom, e.prenom")
    List<Employe> findEmployesActifs();

    // ✅ AJOUT : méthode manquante countByActif
    long countByActif(boolean actif);

    @Query("SELECT e FROM Employe e WHERE e.actif = true ORDER BY e.nom, e.prenom")
    List<Employe> findActiveEmployeesOrderedByName();

    @Query("SELECT COUNT(e) FROM Employe e WHERE e.actif = true")
    long countActiveEmployees();
}
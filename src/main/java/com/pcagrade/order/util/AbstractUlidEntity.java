// ============= CORRECTIF COMPLET - ABSTRACTULIDENTITY + SERVICES =============

// ✅ 1. REMPLACEZ votre AbstractUlidEntity.java ENTIÈREMENT :

package com.pcagrade.order.util;

import com.pcagrade.order.entity.Employe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * ✅ Entité abstraite ULID stocké comme UUID en BINARY(16)
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractUlidEntity {

    @Id
    @GeneratedValue(generator = "ulid-generator")
    @GenericGenerator(
            name = "ulid-generator",
            strategy = "com.pcagrade.order.util.UlidGenerator"
    )
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;  // ✅ UUID au lieu de Ulid

    /**
     * Hook JPA pour s'assurer qu'un ID est généré avant la persistance
     */
    @PrePersist
    protected void ensureId() {
        if (this.id == null) {
            // Générer un ULID et le convertir en UUID
            Ulid ulid = UlidCreator.getUlid();
            this.id = ulid.toUuid();
            System.out.println("🆔 Generated ULID: " + ulid + " → UUID: " + this.id);
        }
    }

    /**
     * Obtient l'ID sous forme de String ULID (reconversion depuis UUID)
     */
    @Transient
    public String getUlidString() {
        if (id == null) return null;
        try {
            // Reconvertir UUID → ULID pour affichage
            Ulid ulid = Ulid.from(id);
            return ulid.toString();
        } catch (Exception e) {
            // Si ce n'est pas un ULID valide, retourner l'UUID
            return id.toString();
        }
    }

    /**
     * Obtient le ULID depuis l'UUID stocké
     */
    @Transient
    public Ulid getUlid() {
        if (id == null) return null;
        try {
            return Ulid.from(id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Obtient l'ID sous forme hexadécimale (pour les requêtes natives)
     */
    @Transient
    public String getIdAsHex() {
        if (id == null) return null;
        return id.toString().replace("-", "").toUpperCase();
    }

    /**
     * Définit l'ID depuis un ULID string
     */
    public void setUlidFromString(String ulidString) {
        if (ulidString == null || ulidString.trim().isEmpty()) {
            this.id = null;
            return;
        }

        try {
            Ulid ulid = Ulid.from(ulidString.trim());
            this.id = ulid.toUuid();
        } catch (Exception e) {
            System.err.println("❌ Erreur conversion ULID string: " + e.getMessage());
            throw new IllegalArgumentException("ULID invalide: " + ulidString, e);
        }
    }

    // Méthodes standard
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AbstractUlidEntity that = (AbstractUlidEntity) obj;
        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + getUlidString() + "}";
    }
}

// ============================================================================

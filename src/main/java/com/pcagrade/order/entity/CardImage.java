package com.pcagrade.order.entity;


import com.pcagrade.order.util.AbstractUlidEntity;
import com.pcagrade.order.util.LocalizationColumnDefinitions;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;


@Data
@EqualsAndHashCode(callSuper=false)  // ← Ajoutez cette ligne
@Entity
@Table(name = "card_image")
public class CardImage extends AbstractUlidEntity {

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(name = "langue",nullable = false, columnDefinition = LocalizationColumnDefinitions.DEFINITION)
    private Localization localization;

    @ManyToOne
    @JoinColumn(name = "image_id")
    private Image image;

//    @OneToMany(mappedBy = "image", cascade = CascadeType.ALL)
//    private List<CardImageHistory> history;


    @Column(name = "fichier", nullable = false)
    private String fichier = "toto";

    @Column(name = "traits", nullable = false, columnDefinition = "longtext")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> traits=Collections.emptyMap();

    @Column(name = "statut", nullable = false)
    private Integer statut=0;

    @Column(name = "infos", nullable = false, columnDefinition = "longtext")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> infos= Collections.emptyMap();

    @Column(name = "downloaded_at", nullable = false)
    private Instant downloadedAt = Instant.now();

    @Column(name = "taille_img", length = 50)
    private String tailleImg;

    @Column(name = "cards")
    private String cards;

    @Column(name = "src")
    private String src;

    /// ///////////////////////////////////////////////////////////

}

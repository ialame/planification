package com.pcagrade.order.entity;


import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "card")
public class Card extends AbstractUlidEntity {

    @Size(max = 255)
    @NotNull
    @Column(name = "discriminator", nullable = false)
    private String discriminator;

    @Size(max = 255)
    @NotNull
    @Column(name = "num", nullable = false)
    private String num;

    @NotNull
    @Lob
    @Column(name = "attributes", nullable = false)
    private String attributes;

    @NotNull
    @ColumnDefault("'[]'")
    @Lob
    @Column(name = "allowed_notes", nullable = false)
    private String allowedNotes;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "ap", nullable = false)
    private Boolean ap = false;

    @Column(name = "image_id")
    private Integer imageId;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "card", cascade = CascadeType.ALL)
    private List<CardTranslation> translations = new ArrayList<>();

}
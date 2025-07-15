package com.pcagrade.order.entity;


import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;


import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "card_translation")
public class CardTranslation extends AbstractUlidEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "translatable_id")
    private Card card;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Size(max = 255)
    @NotNull
    @Column(name = "label_name", nullable = false)
    private String labelName;

    @Column(name = "release_date")
    private Instant releaseDate;

    @NotNull
    @Column(name = "available", nullable = false)
    private Boolean available = false;

    @Size(max = 5)
    @NotNull
    @Column(name = "locale", nullable = false, length = 5)
    private String locale;

    @Size(max = 255)
    @NotNull
    @Column(name = "discriminator", nullable = false)
    private String discriminator;

}
package com.pcagrade.order.entity;


import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "card_certification")
public class CardCertification extends AbstractUlidEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @NotNull
    @Column(name = "date", nullable = false)
    private Instant date;

    @Size(max = 13)
    @NotNull
    @Column(name = "code_barre", nullable = false, length = 13)
    private String codeBarre;

    @ColumnDefault("0")
    @Column(name = "reverse")
    private Boolean reverse;

    @ColumnDefault("2")
    @Column(name = "edition")
    private Integer edition;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "shadowless", nullable = false)
    private Boolean shadowless = false;

    @Size(max = 255)
    @NotNull
    @ColumnDefault("'FR'")
    @Column(name = "langue", nullable = false)
    private String langue;

    @Column(name = "annotation")
    private Boolean annotation;

    @Column(name = "bug")
    private Boolean bug;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "manuelle", nullable = false)
    private Boolean manuelle = false;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "descellee", nullable = false)
    private Boolean descellee = false;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "csn", nullable = false)
    private Boolean csn = false;

    @Size(max = 10)
    @Column(name = "type", length = 10)
    private String type;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "photo", nullable = false)
    private Boolean photo = false;

    @Size(max = 2)
    @NotNull
    @ColumnDefault("'FR'")
    @Column(name = "langue_mention", nullable = false, length = 2)
    private String langueMention;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "vd_cc", nullable = false)
    private Integer vdCc;

    @ColumnDefault("0")
    @Column(name = "foil")
    private Boolean foil;

    @NotNull
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @NotNull
    @Column(name = "status", nullable = false)
    private Integer status;

    @Size(max = 255)
    @NotNull
    @Column(name = "custom_qr_code_url", nullable = false)
    private String customQrCodeUrl;

    @Size(max = 255)
    @NotNull
    @Column(name = "custom_label", nullable = false)
    private String customLabel;

    @Column(name = "multi_grade")
    private Boolean multiGrade;

}
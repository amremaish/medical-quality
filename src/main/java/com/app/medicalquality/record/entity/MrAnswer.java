package com.app.medicalquality.record.entity;

import com.app.medicalquality.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "mr_answers")
@Getter
@Setter
@NoArgsConstructor
public class MrAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id", nullable = false)
    private MrRecord record;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private MrItem item;

    @Column(nullable = false)
    private boolean legible;

    @Column(nullable = false)
    private boolean accurate;

    @Column(nullable = false)
    private boolean complete;

    @Column(nullable = false)
    private boolean na;

    @Column(columnDefinition = "text")
    private String comment;
}

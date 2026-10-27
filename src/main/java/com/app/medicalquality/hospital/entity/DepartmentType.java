package com.app.medicalquality.hospital.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "department_types")
@Getter
@Setter
@NoArgsConstructor
public class DepartmentType {

    @Id
    @Column(length = 40)
    private String code;

    @Column(name = "name_en", nullable = false, length = 120)
    private String nameEn;

    @Column(name = "examples_ar", nullable = false, length = 300)
    private String examplesAr;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}

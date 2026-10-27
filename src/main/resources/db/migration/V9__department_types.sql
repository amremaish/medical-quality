create table department_types (
    code varchar(40) not null primary key,
    name_en varchar(120) not null,
    examples_ar varchar(300) not null,
    sort_order int not null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

insert into department_types (code, name_en, examples_ar, sort_order) values
    ('CLINICAL', 'Clinical Services', 'الباطنة، القلب، أمراض الكلى', 1),
    ('INPATIENT', 'Inpatient Services', 'الأجنحة، أقسام التنويم', 2),
    ('OUTPATIENT', 'Outpatient Services', 'العيادات الخارجية', 3),
    ('EMERGENCY', 'Emergency Services', 'الطوارئ', 4),
    ('CRITICAL_CARE', 'Critical Care', 'ICU، NICU', 5),
    ('SURGICAL', 'Surgical Services', 'غرف العمليات، جراحة القلب', 6),
    ('DIAGNOSTIC', 'Diagnostic Services', 'الإيكو، رسم المخ، وظائف التنفس', 7),
    ('LABORATORY', 'Laboratory Services', 'المعامل', 8),
    ('RADIOLOGY', 'Radiology Services', 'الأشعة', 9),
    ('PHARMACY', 'Pharmacy Services', 'الصيدليات، مخازن الأدوية', 10),
    ('REHABILITATION', 'Rehabilitation Services', 'العلاج الطبيعي، التخاطب', 11),
    ('TRANSPLANT', 'Transplant Services', 'زرع الكلى، زرع الكبد', 12),
    ('BLOOD_BANK', 'Blood Bank Services', 'بنك الدم', 13),
    ('CSSD', 'Sterilization Services', 'التعقيم المركزي', 14),
    ('ADMIN_SUPPORT', 'Administrative & Support Services', 'الإدارة والخدمات المساندة', 15);

update departments
set dept_type = case dept_type
    when 'Pharmacy' then 'PHARMACY'
    when 'Diagnostic' then 'DIAGNOSTIC'
    when 'Inpatient' then 'INPATIENT'
    when 'Inpatient Wards' then 'INPATIENT'
    when 'Day Care' then 'OUTPATIENT'
    when 'Outpatient' then 'OUTPATIENT'
    when 'Laboratory' then 'LABORATORY'
    when 'Radiology' then 'RADIOLOGY'
    when 'Operating Rooms' then 'SURGICAL'
    when 'Cath Lab' then 'SURGICAL'
    when 'Plastic Surgery' then 'SURGICAL'
    when 'ER' then 'EMERGENCY'
    when 'ICU' then 'CRITICAL_CARE'
    when 'NICU' then 'CRITICAL_CARE'
    when 'Rehabilitation' then 'REHABILITATION'
    when 'Speech Therapy' then 'REHABILITATION'
    when 'Cardiology' then 'CLINICAL'
    when 'Neurosciences' then 'CLINICAL'
    when 'Tropical Medicine' then 'CLINICAL'
    when 'Blood Bank' then 'BLOOD_BANK'
    when 'Transplant' then 'TRANSPLANT'
    when 'CSSD' then 'CSSD'
    else dept_type
end;

alter table departments
    modify dept_type varchar(40) not null;

alter table departments
    add constraint departments_dept_type_fk
    foreign key (dept_type) references department_types (code);

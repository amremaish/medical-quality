package com.app.medicalquality.role;

import java.util.Set;

public final class RoleCodes {

    public static final String SYSTEM_ADMINISTRATOR = "SYSTEM_ADMINISTRATOR";
    public static final String HOSPITAL_OWNER = "HOSPITAL_OWNER";
    public static final String QUALITY_DIRECTOR = "QUALITY_DIRECTOR";
    public static final String QUALITY_SUPERVISOR = "QUALITY_SUPERVISOR";
    public static final String ASSESSOR = "ASSESSOR";
    public static final String DEPARTMENT_COORDINATOR = "DEPARTMENT_COORDINATOR";
    public static final String VIEW_ONLY = "VIEW_ONLY";

    public static final String SCOPE_HOSPITAL = "HOSPITAL";
    public static final String SCOPE_ASSESSOR = "ASSESSOR";
    public static final String SCOPE_DEPARTMENT = "DEPARTMENT";
    public static final String SCOPE_DEPARTMENT_LIST = "DEPARTMENT_LIST";

    public static final Set<String> RESERVED = Set.of(
            SYSTEM_ADMINISTRATOR,
            HOSPITAL_OWNER,
            QUALITY_DIRECTOR,
            QUALITY_SUPERVISOR,
            ASSESSOR,
            DEPARTMENT_COORDINATOR,
            VIEW_ONLY);

    public static final Set<String> RESERVED_NAMES = Set.of(
            "System Administrator",
            "Hospital Owner",
            "Quality Director",
            "Quality Supervisor",
            "Assessor",
            "Department Coordinator",
            "View Only");

    public static final Set<String> SCOPES = Set.of(
            SCOPE_HOSPITAL, SCOPE_ASSESSOR, SCOPE_DEPARTMENT, SCOPE_DEPARTMENT_LIST);

    private RoleCodes() {}
}

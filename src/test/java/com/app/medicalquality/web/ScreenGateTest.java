package com.app.medicalquality.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.app.medicalquality.role.RoleCodes;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ScreenGateTest {

    private static final Set<String> CONDUCT = Set.of("CONDUCT_AUDIT");
    private static final Set<String> ASSESSOR_BOARD = Set.of("VIEW_ASSESSOR_DASHBOARD");
    private static final Set<String> SUPERVISOR = Set.of("MANAGE_USERS", "MANAGE_DEPARTMENTS", "CONDUCT_AUDIT");

    @Test
    void ownerSeesDirectoryAndDepartmentCreate() {
        assertTrue(ScreenGate.allowed("/users", false, false, RoleCodes.HOSPITAL_OWNER, Set.of()));
        assertTrue(ScreenGate.allowed("/users/4", false, false, RoleCodes.HOSPITAL_OWNER, Set.of()));
        assertTrue(ScreenGate.allowed("/roles/new", false, false, RoleCodes.HOSPITAL_OWNER, Set.of()));
        assertTrue(ScreenGate.allowed("/departments/new", false, false, RoleCodes.HOSPITAL_OWNER, Set.of()));
        assertTrue(ScreenGate.allowed("/departments/4", false, false, RoleCodes.HOSPITAL_OWNER, Set.of()));
    }

    @Test
    void manageUsersDoesNotOpenTheDirectory() {
        assertFalse(ScreenGate.allowed("/users", false, false, RoleCodes.QUALITY_SUPERVISOR, SUPERVISOR));
        assertFalse(ScreenGate.allowed("/roles", false, false, "CUSTOM_ROLE", SUPERVISOR));
    }

    @Test
    void exceptionalPermissionOpensUserScreensOnly() {
        Set<String> overrides = Set.of("MANAGE_USER_PERMISSION_OVERRIDES");
        assertTrue(ScreenGate.allowed("/users", false, false, "CUSTOM_ROLE", overrides));
        assertTrue(ScreenGate.allowed("/users/4", false, false, "CUSTOM_ROLE", overrides));
        assertFalse(ScreenGate.allowed("/users/new", false, false, "CUSTOM_ROLE", overrides));
        assertFalse(ScreenGate.allowed("/roles", false, false, "CUSTOM_ROLE", overrides));
        assertFalse(ScreenGate.allowed("/roles/new", false, false, "CUSTOM_ROLE", overrides));
    }

    @Test
    void effectivePermissionStatesDriveScreensAndActions() {
        assertTrue(ScreenGate.allowed("/audits/new", false, false, "CUSTOM_ROLE", Set.of("CONDUCT_AUDIT")));
        assertFalse(ScreenGate.allowed("/dashboard", false, false, "CUSTOM_ROLE", Set.of("CONDUCT_AUDIT")));
        assertTrue(ScreenGate.allowed("/dashboard", false, false, "CUSTOM_ROLE", Set.of("VIEW_COMMAND_CENTER")));
        assertFalse(ScreenGate.allowed(
                "/dashboard", false, false, RoleCodes.QUALITY_DIRECTOR, Set.of("CONDUCT_AUDIT")));
        assertFalse(ScreenGate.action(false, Set.of("CONDUCT_AUDIT"), "MANAGE_CAPA"));
        assertTrue(ScreenGate.action(false, Set.of("MANAGE_CAPA"), "MANAGE_CAPA"));
        assertFalse(ScreenGate.action(false, Set.of(), "CONDUCT_AUDIT"));
    }

    @Test
    void assessorEditFollowsManageAssessors() {
        assertFalse(ScreenGate.allowed("/assessors", false, false, RoleCodes.ASSESSOR, Set.of()));
        assertFalse(ScreenGate.allowed("/assessors/3", false, false, RoleCodes.ASSESSOR, Set.of()));
        assertTrue(ScreenGate.allowed("/assessors", false, false, "CUSTOM_ROLE", Set.of("MANAGE_ASSESSORS")));
        assertTrue(ScreenGate.allowed("/assessors/3", false, false, "CUSTOM_ROLE", Set.of("MANAGE_ASSESSORS")));
        assertTrue(ScreenGate.allowed("/assessors", false, false, RoleCodes.HOSPITAL_OWNER, Set.of()));
        assertTrue(ScreenGate.allowed("/assessors/3", false, false, RoleCodes.HOSPITAL_OWNER, Set.of()));
    }

    @Test
    void manageDepartmentsDoesNotOpenDepartmentCreate() {
        assertFalse(ScreenGate.allowed(
                "/departments/new", false, false, RoleCodes.QUALITY_SUPERVISOR, SUPERVISOR));
        assertFalse(ScreenGate.allowed(
                "/departments/4", false, false, RoleCodes.QUALITY_SUPERVISOR, SUPERVISOR));
        assertTrue(ScreenGate.allowed("/departments", false, false, RoleCodes.QUALITY_SUPERVISOR, SUPERVISOR));
    }

    @Test
    void customRoleSeesOnlyScreensForItsPermissions() {
        assertTrue(ScreenGate.allowed("/audits/new", false, false, "CUSTOM_ROLE", CONDUCT));
        assertTrue(ScreenGate.allowed("/record-reviews/new", false, false, "CUSTOM_ROLE", CONDUCT));
        assertTrue(ScreenGate.allowed("/audits", false, false, "CUSTOM_ROLE", CONDUCT));
        assertTrue(ScreenGate.allowed("/audits/4", false, false, "CUSTOM_ROLE", CONDUCT));
        assertTrue(ScreenGate.allowed("/audits?departmentId=2", false, false, "CUSTOM_ROLE", CONDUCT));
        assertFalse(ScreenGate.allowed("/capas", false, false, "CUSTOM_ROLE", CONDUCT));
        assertFalse(ScreenGate.allowed("/capas/new", false, false, "CUSTOM_ROLE", CONDUCT));
        assertTrue(ScreenGate.allowed("/capas", false, false, "CUSTOM_ROLE", Set.of("MANAGE_CAPA")));
        assertFalse(ScreenGate.allowed("/findings", false, false, "CUSTOM_ROLE", CONDUCT));
        assertTrue(ScreenGate.allowed("/findings", false, false, "CUSTOM_ROLE", Set.of("MANAGE_FINDINGS")));
        assertFalse(ScreenGate.allowed("/evidence", false, false, "CUSTOM_ROLE", CONDUCT));
        assertTrue(ScreenGate.allowed("/evidence", false, false, "CUSTOM_ROLE", Set.of("MANAGE_EVIDENCE")));
        assertFalse(ScreenGate.allowed("/departments", false, false, "CUSTOM_ROLE", CONDUCT));
        assertTrue(ScreenGate.allowed("/departments", false, false, "CUSTOM_ROLE", Set.of("MANAGE_DEPARTMENTS")));
        assertFalse(ScreenGate.allowed("/dashboard", false, false, "CUSTOM_ROLE", CONDUCT));
        assertFalse(ScreenGate.allowed("/imports", false, false, "CUSTOM_ROLE", CONDUCT));
        assertFalse(ScreenGate.allowed("/imports/new", false, false, "CUSTOM_ROLE", CONDUCT));
        assertFalse(ScreenGate.allowed("/evidence/new", false, false, "CUSTOM_ROLE", CONDUCT));
    }

    @Test
    void dashboardScreensFollowTheirOwnPermission() {
        assertTrue(ScreenGate.allowed("/dashboard/assessor", false, false, "CUSTOM_ROLE", ASSESSOR_BOARD));
        assertFalse(ScreenGate.allowed("/dashboard", false, false, "CUSTOM_ROLE", ASSESSOR_BOARD));
        assertFalse(ScreenGate.allowed("/dashboard/executive", false, false, "CUSTOM_ROLE", ASSESSOR_BOARD));
        assertFalse(ScreenGate.allowed("/dashboard/quality-director", false, false, "CUSTOM_ROLE", ASSESSOR_BOARD));
        assertFalse(ScreenGate.allowed("/dashboard/unknown", false, false, "CUSTOM_ROLE", ASSESSOR_BOARD));
    }

    @Test
    void directUrlToAGatedScreenIsDeniedWhenThePermissionIsMissing() {
        assertFalse(ScreenGate.allowed("/audits/new", false, false, RoleCodes.VIEW_ONLY, Set.of()));
        assertFalse(ScreenGate.allowed("/audits/new", false, false, RoleCodes.VIEW_ONLY, null));
        assertFalse(ScreenGate.allowed("/audits", false, false, RoleCodes.VIEW_ONLY, null));
        assertFalse(ScreenGate.allowed("/findings", false, false, RoleCodes.VIEW_ONLY, Set.of("VIEW_ASSESSOR_DASHBOARD")));
        assertTrue(ScreenGate.allowed("/dashboard/assessor", false, false, RoleCodes.VIEW_ONLY, Set.of("VIEW_ASSESSOR_DASHBOARD")));
        assertTrue(ScreenGate.allowed("/", false, false, RoleCodes.VIEW_ONLY, null));
    }

    @Test
    void superAdminWithoutAnOpenedHospitalStaysOnPlatformScreens() {
        assertFalse(ScreenGate.allowed("/audits", true, false, null, Set.of()));
        assertFalse(ScreenGate.allowed("/users", true, false, null, Set.of()));
        assertTrue(ScreenGate.allowed("/organizations", true, false, null, Set.of()));
        assertTrue(ScreenGate.allowed("/owners", true, false, null, Set.of()));
        assertFalse(ScreenGate.allowed("/organizations", false, false, RoleCodes.HOSPITAL_OWNER, Set.of()));
        assertFalse(ScreenGate.allowed("/hospitals", false, false, RoleCodes.HOSPITAL_OWNER, Set.of()));
    }

    @Test
    void superAdminInsideAHospitalHoldsEveryScreen() {
        assertTrue(ScreenGate.allowed("/users", true, true, null, Set.of()));
        assertTrue(ScreenGate.allowed("/dashboard", true, true, null, Set.of()));
        assertTrue(ScreenGate.allowed("/audits/new", true, true, null, null));
        assertTrue(ScreenGate.allowed("/departments/new", true, true, null, Set.of()));
    }

    @Test
    void actionsAreAuthorizedIndependently() {
        Set<String> deleteOnly = Set.of("DELETE_AUDIT_ONLY");
        assertTrue(ScreenGate.action(false, deleteOnly, "DELETE_AUDIT_ONLY"));
        assertFalse(ScreenGate.action(false, deleteOnly, "DELETE_AUDIT_PDF"));
        assertFalse(ScreenGate.action(false, deleteOnly, "DELETE_AUDIT_EVERYTHING"));
        assertFalse(ScreenGate.action(false, null, "CONDUCT_AUDIT"));
        assertFalse(ScreenGate.everyAction(false, Set.of("MANAGE_IMPORT"), "MANAGE_IMPORT", "CONDUCT_AUDIT"));
        assertTrue(ScreenGate.everyAction(
                false, Set.of("MANAGE_IMPORT", "CONDUCT_AUDIT"), "MANAGE_IMPORT", "CONDUCT_AUDIT"));
        assertTrue(ScreenGate.action(true, null, "MANAGE_CAPA"));
        assertFalse(ScreenGate.action(true, null, ""));
    }

    @Test
    void eachRoleOpensOnlyTheScreensItsPermissionsAllow() {
        Set<String> assessor = Set.of(
                "CONDUCT_AUDIT",
                "MANAGE_FINDINGS",
                "MANAGE_CAPA",
                "MANAGE_EVIDENCE",
                "VIEW_ASSESSOR_DASHBOARD");
        assertTrue(ScreenGate.allowed("/audits", false, false, RoleCodes.ASSESSOR, assessor));
        assertTrue(ScreenGate.allowed("/record-reviews", false, false, RoleCodes.ASSESSOR, assessor));
        assertTrue(ScreenGate.allowed("/findings", false, false, RoleCodes.ASSESSOR, assessor));
        assertTrue(ScreenGate.allowed("/capas", false, false, RoleCodes.ASSESSOR, assessor));
        assertTrue(ScreenGate.allowed("/evidence", false, false, RoleCodes.ASSESSOR, assessor));
        assertTrue(ScreenGate.allowed("/dashboard/assessor", false, false, RoleCodes.ASSESSOR, assessor));
        assertFalse(ScreenGate.allowed("/departments", false, false, RoleCodes.ASSESSOR, assessor));
        assertFalse(ScreenGate.allowed("/assessors", false, false, RoleCodes.ASSESSOR, assessor));
        assertFalse(ScreenGate.allowed("/dashboard", false, false, RoleCodes.ASSESSOR, assessor));
        assertFalse(ScreenGate.allowed("/imports", false, false, RoleCodes.ASSESSOR, assessor));
        assertFalse(ScreenGate.allowed("/users", false, false, RoleCodes.ASSESSOR, assessor));
        assertFalse(ScreenGate.allowed("/quality-policy", false, false, RoleCodes.ASSESSOR, assessor));

        Set<String> coordinator = Set.of("MANAGE_CAPA", "MANAGE_EVIDENCE", "VIEW_ASSESSOR_DASHBOARD");
        assertTrue(ScreenGate.allowed("/capas", false, false, RoleCodes.DEPARTMENT_COORDINATOR, coordinator));
        assertTrue(ScreenGate.allowed("/evidence", false, false, RoleCodes.DEPARTMENT_COORDINATOR, coordinator));
        assertFalse(ScreenGate.allowed("/audits", false, false, RoleCodes.DEPARTMENT_COORDINATOR, coordinator));
        assertFalse(ScreenGate.allowed("/findings", false, false, RoleCodes.DEPARTMENT_COORDINATOR, coordinator));
        assertFalse(ScreenGate.allowed("/departments", false, false, RoleCodes.DEPARTMENT_COORDINATOR, coordinator));
        assertFalse(ScreenGate.allowed("/assessors", false, false, RoleCodes.DEPARTMENT_COORDINATOR, coordinator));
    }

    @Test
    void qualityPolicyRequiresItsOwnPrivilege() {
        assertFalse(ScreenGate.allowed("/quality-policy", false, false, RoleCodes.ASSESSOR, Set.of("CONDUCT_AUDIT")));
        assertTrue(ScreenGate.allowed(
                "/quality-policy", false, false, RoleCodes.QUALITY_DIRECTOR, Set.of("QUALITY_POLICY_VIEW")));
        assertTrue(ScreenGate.allowed(
                "/quality-policy", false, false, RoleCodes.HOSPITAL_OWNER, Set.of("QUALITY_POLICY_EDIT")));
        assertFalse(ScreenGate.allowed("/quality-policy", true, false, null, Set.of("QUALITY_POLICY_VIEW")));
        assertTrue(ScreenGate.allowed("/quality-policy", true, true, null, Set.of()));
    }

    @Test
    void passwordPageStaysAvailable() {
        assertTrue(ScreenGate.allowed("/account/password", false, false, RoleCodes.VIEW_ONLY, null));
    }
}

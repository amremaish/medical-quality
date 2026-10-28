package com.app.medicalquality.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EffectivePermissionsTest {

    @Test
    void inheritUsesTheRole() {
        assertTrue(EffectivePermissions.granted(true, EffectivePermissions.INHERIT, true));
        assertFalse(EffectivePermissions.granted(false, null, true));
    }

    @Test
    void allowGrantsAPermissionTheRoleDoesNot() {
        assertTrue(EffectivePermissions.granted(false, EffectivePermissions.ALLOW, true));
    }

    @Test
    void denyRemovesAPermissionTheRoleGrants() {
        assertFalse(EffectivePermissions.granted(true, EffectivePermissions.DENY, true));
    }

    @Test
    void allowOfAnIneligiblePermissionDoesNotGrantIt() {
        assertFalse(EffectivePermissions.granted(false, EffectivePermissions.ALLOW, false));
        assertTrue(EffectivePermissions.granted(true, EffectivePermissions.ALLOW, false));
    }

    @Test
    void notesExplainARemovalAndAnAddedGrant() {
        assertEquals(
                "Explicit denial. The role grants this, and this person does not.",
                EffectivePermissions.note(true, EffectivePermissions.DENY));
        assertEquals(
                "Explicit grant. The role does not include this, and this person does.",
                EffectivePermissions.note(false, EffectivePermissions.ALLOW));
        assertEquals("", EffectivePermissions.note(true, EffectivePermissions.INHERIT));
    }
}

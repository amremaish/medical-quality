package com.app.medicalquality.auth.security;

/**
 * ALLOW and DENY beat the role. INHERIT means there is no override.
 * An ALLOW for a permission that is not eligible for user-level delegation does not grant it.
 */
public final class EffectivePermissions {

    public static final String INHERIT = "INHERIT";
    public static final String ALLOW = "ALLOW";
    public static final String DENY = "DENY";

    private EffectivePermissions() {}

    public static boolean granted(boolean a_roleGrants, String a_effect, boolean a_eligible) {
        if (DENY.equals(a_effect)) {
            return false;
        }
        if (ALLOW.equals(a_effect) && a_eligible) {
            return true;
        }
        return a_roleGrants;
    }

    public static String note(boolean a_roleGrants, String a_effect) {
        if (DENY.equals(a_effect) && a_roleGrants) {
            return "Explicit denial. The role grants this, and this person does not.";
        }
        if (ALLOW.equals(a_effect) && !a_roleGrants) {
            return "Explicit grant. The role does not include this, and this person does.";
        }
        return "";
    }
}

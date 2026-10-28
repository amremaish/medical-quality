package com.app.medicalquality.organization.dto;

public final class OrganizationViews {

    private OrganizationViews() {}

    public record Summary(
            Long id, String name, String orgType, String status, String ownerEmail, String ownerName) {}

    public record Invitation(Long organizationId, String organizationName, String ownerEmail, String setupUrl) {}

    public record Owner(Long id, String name, String email, Long organizationId, String organizationName, String status) {}
}

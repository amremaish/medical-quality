package com.app.medicalquality.organization.service;

import com.app.medicalquality.auth.AccountStatus;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.service.SetupTokenService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.organization.FacilityType;
import com.app.medicalquality.organization.dto.OrganizationRequests;
import com.app.medicalquality.organization.dto.OrganizationViews;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.organization.repository.OrganizationRepository;
import com.app.medicalquality.role.RoleCodes;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.repository.RoleRepository;
import com.app.medicalquality.role.service.RoleCloneService;
import java.util.List;
import java.util.Locale;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationProvisioningService {

    public static final String OWNER_ROLE = "Hospital Owner";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleCloneService roleCloneService;
    private final SetupTokenService setupTokenService;

    public OrganizationProvisioningService(
            OrganizationRepository a_organizationRepository,
            UserRepository a_userRepository,
            RoleRepository a_roleRepository,
            PasswordEncoder a_passwordEncoder,
            RoleCloneService a_roleCloneService,
            SetupTokenService a_setupTokenService) {
        this.organizationRepository = a_organizationRepository;
        this.userRepository = a_userRepository;
        this.roleRepository = a_roleRepository;
        this.passwordEncoder = a_passwordEncoder;
        this.roleCloneService = a_roleCloneService;
        this.setupTokenService = a_setupTokenService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationViews.Summary> list(Authentication a_authentication) {
        requireSuperAdmin(a_authentication);
        List<User> owners = hospitalOwners();
        return organizationRepository.findByDeletedAtIsNullOrderByNameAsc().stream()
                .map(a_organization -> summary(a_organization, owners))
                .toList();
    }

    @Transactional
    public OrganizationViews.Invitation provision(
            OrganizationRequests.Create a_request, Authentication a_authentication, String a_baseUrl) {
        requireSuperAdmin(a_authentication);
        String email = a_request.getOwnerEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByUsername(email).isPresent()) {
            throw new AppException("error.owner.email_taken");
        }
        Organization organization = saveOrganization(a_request);
        return inviteOwner(organization, a_request.getOwnerFirstName(), a_request.getOwnerLastName(), email, a_baseUrl);
    }

    @Transactional(readOnly = true)
    public List<OrganizationViews.Owner> owners(Authentication a_authentication) {
        requireSuperAdmin(a_authentication);
        return hospitalOwners().stream().map(this::ownerView).toList();
    }

    @Transactional
    public OrganizationViews.Invitation invite(
            OrganizationRequests.Invite a_request, Authentication a_authentication, String a_baseUrl) {
        requireSuperAdmin(a_authentication);
        Organization organization = organizationRepository
                .findById(a_request.getOrganizationId())
                .filter(a_organization -> a_organization.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("error.organization.not_found"));
        return inviteOwner(
                organization, a_request.getOwnerFirstName(), a_request.getOwnerLastName(), a_request.getOwnerEmail(), a_baseUrl);
    }

    private List<User> hospitalOwners() {
        return userRepository.findByOrganizationIsNotNullAndDeletedAtIsNull().stream()
                .filter(a_user -> a_user.getRole() != null
                        && a_user.getRole().isBuiltIn()
                        && RoleCodes.HOSPITAL_OWNER.equals(a_user.getRole().getRoleCode()))
                .toList();
    }

    private Organization saveOrganization(OrganizationRequests.Create a_request) {
        Organization organization = new Organization();
        organization.setName(a_request.getName().trim());
        organization.setOrgType(type(a_request.getOrgType()));
        organization.setStatus("Active");
        return organizationRepository.save(organization);
    }

    private OrganizationViews.Invitation inviteOwner(
            Organization a_organization, String a_firstName, String a_lastName, String a_email, String a_baseUrl) {
        String email = a_email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByUsername(email).isPresent()) {
            throw new AppException("error.owner.email_taken");
        }
        roleCloneService.ensureCopies(a_organization);
        Role role = roleRepository
                .findByRoleCodeAndOrganizationIdAndDeletedAtIsNull(RoleCodes.HOSPITAL_OWNER, a_organization.getId())
                .orElseThrow(() -> new AppException("error.role.not_found"));
        User owner = new User();
        owner.setUsername(email);
        owner.setEmail(email);
        owner.setFirstName(a_firstName.trim());
        owner.setLastName(blank(a_lastName));
        owner.setPasswordHash(SetupTokenService.discardedPasswordHash(passwordEncoder));
        owner.setRole(role);
        owner.setEnabled(false);
        owner.setSuperAdmin(false);
        owner.setMustChangePassword(false);
        owner.setOrganization(a_organization);
        userRepository.save(owner);
        return new OrganizationViews.Invitation(
                a_organization.getId(),
                a_organization.getName(),
                email,
                setupTokenService.issueUrl(owner, a_baseUrl));
    }

    private OrganizationViews.Summary summary(Organization a_organization, List<User> a_owners) {
        User owner = a_owners.stream()
                .filter(a_user -> a_user.getOrganization().getId().equals(a_organization.getId()))
                .findFirst()
                .orElse(null);
        String email = owner == null ? "" : owner.getEmail();
        String name = owner == null ? "" : owner.getFirstName() + (owner.getLastName() == null ? "" : " " + owner.getLastName());
        return new OrganizationViews.Summary(
                a_organization.getId(),
                a_organization.getName(),
                a_organization.getOrgType().name(),
                a_organization.getStatus(),
                email,
                name.trim());
    }

    private OrganizationViews.Owner ownerView(User a_user) {
        String name = a_user.getFirstName() + (a_user.getLastName() == null ? "" : " " + a_user.getLastName());
        String status = AccountStatus.of(a_user.isEnabled());
        return new OrganizationViews.Owner(
                a_user.getId(),
                name.trim(),
                a_user.getEmail(),
                a_user.getOrganization().getId(),
                a_user.getOrganization().getName(),
                status);
    }

    private FacilityType type(String a_value) {
        try {
            return FacilityType.valueOf(a_value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new AppException("error.organization.type");
        }
    }

    private String blank(String a_value) {
        if (a_value == null || a_value.isBlank()) {
            return null;
        }
        return a_value.trim();
    }

    public void authorize(Authentication a_authentication) {
        requireSuperAdmin(a_authentication);
    }

    private void requireSuperAdmin(Authentication a_authentication) {
        User user = userRepository
                .findByUsername(a_authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Access denied"));
        if (!user.isSuperAdmin()) {
            throw new AccessDeniedException("Access denied");
        }
    }
}

package com.app.medicalquality.organization.service;

import com.app.medicalquality.auth.AccountStatus;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.auth.security.EffectivePermissionService;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.auth.service.SetupTokenService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.hospital.entity.Department;
import com.app.medicalquality.hospital.entity.UserDepartmentAccess;
import com.app.medicalquality.hospital.repository.AssessorRepository;
import com.app.medicalquality.hospital.repository.DepartmentRepository;
import com.app.medicalquality.hospital.repository.UserDepartmentAccessRepository;
import com.app.medicalquality.hospital.service.HospitalContext;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.organization.repository.OrganizationRepository;
import com.app.medicalquality.role.CustomRolePolicy;
import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.role.RoleCodes;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import com.app.medicalquality.role.repository.PermissionRepository;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import com.app.medicalquality.role.repository.RoleRepository;
import com.app.medicalquality.role.service.RoleCloneService;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectoryService {

    private final HospitalContext hospitalContext;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final DepartmentRepository departmentRepository;
    private final AssessorRepository assessorRepository;
    private final UserDepartmentAccessRepository accessRepository;
    private final RoleCloneService roleCloneService;
    private final SetupTokenService setupTokenService;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final EffectivePermissionService effectivePermissionService;
    private final PermissionEvaluator permissionEvaluator;

    public DirectoryService(
            HospitalContext a_hospitalContext,
            OrganizationRepository a_organizationRepository,
            UserRepository a_userRepository,
            RoleRepository a_roleRepository,
            RolePermissionRepository a_rolePermissionRepository,
            PermissionRepository a_permissionRepository,
            DepartmentRepository a_departmentRepository,
            AssessorRepository a_assessorRepository,
            UserDepartmentAccessRepository a_accessRepository,
            RoleCloneService a_roleCloneService,
            SetupTokenService a_setupTokenService,
            CurrentUserService a_currentUserService,
            PasswordEncoder a_passwordEncoder,
            EffectivePermissionService a_effectivePermissionService,
            PermissionEvaluator a_permissionEvaluator) {
        this.hospitalContext = a_hospitalContext;
        this.organizationRepository = a_organizationRepository;
        this.userRepository = a_userRepository;
        this.roleRepository = a_roleRepository;
        this.rolePermissionRepository = a_rolePermissionRepository;
        this.permissionRepository = a_permissionRepository;
        this.departmentRepository = a_departmentRepository;
        this.assessorRepository = a_assessorRepository;
        this.accessRepository = a_accessRepository;
        this.roleCloneService = a_roleCloneService;
        this.setupTokenService = a_setupTokenService;
        this.currentUserService = a_currentUserService;
        this.passwordEncoder = a_passwordEncoder;
        this.effectivePermissionService = a_effectivePermissionService;
        this.permissionEvaluator = a_permissionEvaluator;
    }

    @Transactional
    public List<UserView> users(Long a_organizationId, Authentication a_authentication) {
        Organization organization = openUsers(a_organizationId, a_authentication);
        return userRepository.findByOrganizationIdAndDeletedAtIsNullOrderByEmailAsc(organization.getId()).stream()
                .filter(a_user -> !a_user.isSuperAdmin())
                .map(a_user -> userView(a_user, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserView user(Long a_organizationId, Long a_userId, Authentication a_authentication) {
        Organization organization = openUsers(a_organizationId, a_authentication);
        User user = userRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(a_userId, organization.getId())
                .filter(a_row -> !a_row.isSuperAdmin())
                .orElseThrow(() -> new NotFoundException("error.user.not_found"));
        List<Long> departmentIds = accessRepository.findByUserIdAndDeletedAtIsNull(user.getId()).stream()
                .map(a_access -> a_access.getDepartment().getId())
                .toList();
        return userView(user, departmentIds);
    }

    @Transactional
    public SetupLink invite(Long a_organizationId, InviteForm a_form, Authentication a_authentication, String a_baseUrl) {
        Organization organization = open(a_organizationId, a_authentication);
        roleCloneService.ensureCopies(organization);
        String email = email(a_form.getEmail());
        if (userRepository.findByUsername(email).isPresent()) {
            throw new AppException("error.owner.email_taken");
        }
        Role role = hospitalRole(a_form.getRoleId(), organization.getId());
        User user = new User();
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(required(a_form.getFirstName()));
        user.setLastName(blank(a_form.getLastName()));
        user.setPasswordHash(SetupTokenService.discardedPasswordHash(passwordEncoder));
        user.setRole(role);
        user.setEnabled(false);
        user.setSuperAdmin(false);
        user.setMustChangePassword(false);
        user.setOrganization(organization);
        userRepository.save(user);
        applyScope(user, role, organization, a_form.getAssessorId(), a_form.getDepartmentId(), a_form.getDepartmentIds());
        return new SetupLink(email, setupTokenService.issueUrl(user, a_baseUrl));
    }

    @Transactional
    public SetupLink updateUser(
            Long a_organizationId, Long a_userId, UserPatch a_form, Authentication a_authentication, String a_baseUrl) {
        Organization organization = open(a_organizationId, a_authentication);
        User user = userRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(a_userId, organization.getId())
                .filter(a_user -> !a_user.isSuperAdmin())
                .orElseThrow(() -> new NotFoundException("error.user.not_found"));
        Role next = a_form.getRoleId() == null ? user.getRole() : hospitalRole(a_form.getRoleId(), organization.getId());
        boolean reissue = Boolean.TRUE.equals(a_form.getReissueSetup());
        boolean willBeEnabled = reissue ? false : (a_form.getEnabled() == null ? user.isEnabled() : a_form.getEnabled());
        guardOwner(organization.getId(), user, next, willBeEnabled);
        applyProfile(user, a_form);
        if (a_form.getRoleId() != null || a_form.getAssessorId() != null || a_form.getDepartmentId() != null
                || a_form.getDepartmentIds() != null) {
            user.setRole(next);
            applyScope(user, next, organization, a_form.getAssessorId(), a_form.getDepartmentId(), a_form.getDepartmentIds());
        }
        if (reissue) {
            user.setEnabled(false);
            user.setMustChangePassword(false);
            user.setPasswordHash(SetupTokenService.discardedPasswordHash(passwordEncoder));
            return new SetupLink(user.getEmail(), setupTokenService.issueUrl(user, a_baseUrl));
        }
        if (a_form.getEnabled() != null) {
            boolean turningOff = user.isEnabled() && !a_form.getEnabled();
            user.setEnabled(a_form.getEnabled());
            if (turningOff) {
                user.setMustChangePassword(false);
                setupTokenService.revokeUnused(user.getId());
            }
        }
        return new SetupLink(user.getEmail(), null);
    }

    @Transactional
    public List<RoleView> roles(Long a_organizationId, Authentication a_authentication) {
        Organization organization = open(a_organizationId, a_authentication);
        roleCloneService.ensureCopies(organization);
        return roleRepository.findByOrganizationIdAndDeletedAtIsNullOrderByNameAsc(organization.getId()).stream()
                .map(this::roleView)
                .toList();
    }

    @Transactional
    public RoleView createRole(Long a_organizationId, RoleForm a_form, Authentication a_authentication) {
        Organization organization = open(a_organizationId, a_authentication);
        String name = required(a_form.getName());
        String scope = scope(a_form.getScopeMode());
        if (roleRepository.existsByOrganizationIdAndNameAndDeletedAtIsNull(organization.getId(), name)) {
            throw new AppException("error.role.already_exists");
        }
        CustomRolePolicy.check(a_form.getPermissions(), held(a_authentication), currentUserService.require(a_authentication).isSuperAdmin());
        Role role = new Role();
        role.setName(name);
        role.setRoleCode(customCode());
        role.setOrganization(organization);
        role.setBuiltIn(false);
        role.setTemplate(false);
        role.setScopeMode(scope);
        roleRepository.save(role);
        grant(role, a_form.getPermissions());
        return roleView(role);
    }

    @Transactional
    public RoleView updateRole(Long a_organizationId, Long a_roleId, RoleForm a_form, Authentication a_authentication) {
        Organization organization = open(a_organizationId, a_authentication);
        Role role = hospitalRole(a_roleId, organization.getId());
        if (role.isBuiltIn()) {
            throw new AppException("error.role.builtin");
        }
        String scope = scope(a_form.getScopeMode());
        if (!scope.equals(role.getScopeMode()) && userRepository.countByRoleIdAndDeletedAtIsNull(role.getId()) > 0) {
            throw new AppException("error.role.scope_locked");
        }
        String name = required(a_form.getName());
        if (!name.equals(role.getName())
                && roleRepository.existsByOrganizationIdAndNameAndDeletedAtIsNull(organization.getId(), name)) {
            throw new AppException("error.role.already_exists");
        }
        CustomRolePolicy.check(a_form.getPermissions(), held(a_authentication), currentUserService.require(a_authentication).isSuperAdmin());
        role.setName(name);
        role.setScopeMode(scope);
        rolePermissionRepository.deleteAll(rolePermissionRepository.findByRoleAndDeletedAtIsNull(role));
        rolePermissionRepository.flush();
        grant(role, a_form.getPermissions());
        return roleView(role);
    }

    @Transactional
    public void deleteRole(Long a_organizationId, Long a_roleId, Authentication a_authentication) {
        Organization organization = open(a_organizationId, a_authentication);
        Role role = hospitalRole(a_roleId, organization.getId());
        if (role.isBuiltIn()) {
            throw new AppException("error.role.builtin");
        }
        if (userRepository.countByRoleIdAndDeletedAtIsNull(role.getId()) > 0) {
            throw new AppException("error.role.in_use");
        }
        role.setDeletedAt(java.time.LocalDateTime.now());
    }

    private Organization openUsers(Long a_organizationId, Authentication a_authentication) {
        Organization acting = hospitalContext.require(a_authentication);
        if (a_organizationId == null || !acting.getId().equals(a_organizationId)) {
            throw new AccessDeniedException("Access denied");
        }
        if (!hospitalContext.managesDirectory(a_authentication)
                && !permissionEvaluator.hasPermission(
                        a_authentication, PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES.name())) {
            throw new AccessDeniedException("Access denied");
        }
        return organizationRepository
                .findById(a_organizationId)
                .filter(a_organization -> a_organization.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("error.organization.not_found"));
    }

    private Organization open(Long a_organizationId, Authentication a_authentication) {
        hospitalContext.requireDirectoryAdmin(a_authentication, a_organizationId);
        return organizationRepository
                .findById(a_organizationId)
                .filter(a_organization -> a_organization.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("error.organization.not_found"));
    }

    private void guardOwner(Long a_organizationId, User a_user, Role a_next, boolean a_willBeEnabled) {
        organizationRepository.lockById(a_organizationId).orElseThrow(() -> new NotFoundException("error.organization.not_found"));
        List<User> owners = userRepository.lockEnabledOwners(a_organizationId);
        boolean currently = a_user.isEnabled() && ownerRole(a_user.getRole());
        boolean will = a_willBeEnabled && ownerRole(a_next);
        if (CustomRolePolicy.removesLastOwner(owners.size(), currently, will)) {
            throw new AppException("error.owner.last");
        }
    }

    private boolean ownerRole(Role a_role) {
        return a_role != null && a_role.isBuiltIn() && RoleCodes.HOSPITAL_OWNER.equals(a_role.getRoleCode());
    }

    private void applyScope(
            User a_user,
            Role a_role,
            Organization a_organization,
            Long a_assessorId,
            Long a_departmentId,
            List<Long> a_departmentIds) {
        if (a_user.getId() != null) {
            accessRepository.deleteByUserId(a_user.getId());
        }
        a_user.setAssessor(null);
        a_user.setDepartment(null);
        switch (a_role.getScopeMode()) {
            case RoleCodes.SCOPE_HOSPITAL -> {
                return;
            }
            case RoleCodes.SCOPE_ASSESSOR -> {
                if (a_assessorId == null) {
                    throw new AppException("error.user.scope");
                }
                if (userRepository.existsByAssessorIdAndIdNotAndDeletedAtIsNull(
                        a_assessorId, a_user.getId() == null ? -1L : a_user.getId())) {
                    throw new AppException("error.user.scope");
                }
                a_user.setAssessor(assessorRepository
                        .findByIdAndOrganizationIdAndDeletedAtIsNull(a_assessorId, a_organization.getId())
                        .orElseThrow(() -> new NotFoundException("error.assessor.not_found")));
            }
            case RoleCodes.SCOPE_DEPARTMENT -> a_user.setDepartment(department(a_departmentId, a_organization.getId()));
            case RoleCodes.SCOPE_DEPARTMENT_LIST -> {
                if (a_departmentIds == null || a_departmentIds.isEmpty()) {
                    throw new AppException("error.user.scope");
                }
                for (Long departmentId : a_departmentIds.stream().distinct().toList()) {
                    UserDepartmentAccess access = new UserDepartmentAccess();
                    access.setUser(a_user);
                    access.setDepartment(department(departmentId, a_organization.getId()));
                    access.setOrganization(a_organization);
                    accessRepository.save(access);
                }
            }
            default -> throw new AppException("error.user.scope");
        }
    }

    private Department department(Long a_departmentId, Long a_organizationId) {
        if (a_departmentId == null) {
            throw new AppException("error.user.scope");
        }
        return departmentRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(a_departmentId, a_organizationId)
                .orElseThrow(() -> new NotFoundException("error.department.not_found"));
    }

    private Role hospitalRole(Long a_roleId, Long a_organizationId) {
        if (a_roleId == null) {
            throw new NotFoundException("error.role.not_found");
        }
        return roleRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(a_roleId, a_organizationId)
                .orElseThrow(() -> new NotFoundException("error.role.not_found"));
    }

    private void grant(Role a_role, List<PermissionEnum> a_permissions) {
        for (PermissionEnum permission : a_permissions.stream().distinct().toList()) {
            RolePermission link = new RolePermission();
            link.setRole(a_role);
            link.setPermission(permissionRepository
                    .findByName(permission.name())
                    .orElseThrow(() -> new AppException("error.role.permission_not_found", new Object[] {permission.name()})));
            rolePermissionRepository.save(link);
        }
    }

    private Set<PermissionEnum> held(Authentication a_authentication) {
        User caller = currentUserService.require(a_authentication);
        if (caller.getRole() == null) {
            return Set.of();
        }
        return effectivePermissionService.names(caller).stream()
                .map(PermissionEnum::valueOf)
                .collect(Collectors.toSet());
    }

    private String customCode() {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        return "CUSTOM_" + HexFormat.of().formatHex(bytes).toUpperCase();
    }

    private String scope(String a_scope) {
        if (a_scope == null || !RoleCodes.SCOPES.contains(a_scope)) {
            throw new AppException("error.user.scope");
        }
        return a_scope;
    }

    private String email(String a_email) {
        String email = required(a_email).toLowerCase(Locale.ROOT);
        if (!email.contains("@") || email.length() > 320) {
            throw new AppException("error.validation.failed");
        }
        return email;
    }

    private String required(String a_value) {
        String value = blank(a_value);
        if (value == null) {
            throw new AppException("error.validation.failed");
        }
        return value;
    }

    private String blank(String a_value) {
        if (a_value == null || a_value.isBlank()) {
            return null;
        }
        String value = a_value.trim();
        if (value.length() > 128) {
            throw new AppException("error.validation.failed");
        }
        return value;
    }

    private void applyProfile(User a_user, UserPatch a_form) {
        if (a_form.getFirstName() != null) {
            a_user.setFirstName(required(a_form.getFirstName()));
        }
        if (a_form.getLastName() != null) {
            a_user.setLastName(blank(a_form.getLastName()));
        }
        if (a_form.getEmail() == null) {
            return;
        }
        String email = email(a_form.getEmail());
        if (email.equals(a_user.getUsername())) {
            return;
        }
        userRepository.findByUsername(email).ifPresent(a_existing -> {
            if (!a_existing.getId().equals(a_user.getId())) {
                throw new AppException("error.owner.email_taken");
            }
        });
        a_user.setUsername(email);
        a_user.setEmail(email);
    }

    private UserView userView(User a_user, List<Long> a_departmentIds) {
        Role role = a_user.getRole();
        Long assessorId = a_user.getAssessor() == null ? null : a_user.getAssessor().getId();
        Long departmentId = a_user.getDepartment() == null ? null : a_user.getDepartment().getId();
        return new UserView(
                a_user.getId(),
                a_user.getEmail(),
                a_user.getFirstName(),
                a_user.getLastName(),
                role == null ? null : role.getId(),
                role == null ? "" : role.getName(),
                role == null ? "" : role.getRoleCode(),
                a_user.isEnabled(),
                AccountStatus.of(a_user.isEnabled()),
                assessorId,
                departmentId,
                a_departmentIds);
    }

    private RoleView roleView(Role a_role) {
        List<String> permissions = rolePermissionRepository.findByRoleAndDeletedAtIsNull(a_role).stream()
                .map(a_link -> a_link.getPermission().getName())
                .sorted()
                .toList();
        return new RoleView(
                a_role.getId(), a_role.getName(), a_role.getRoleCode(), a_role.getScopeMode(), a_role.isBuiltIn(), permissions);
    }

    public record UserView(
            Long id,
            String email,
            String firstName,
            String lastName,
            Long roleId,
            String roleName,
            String roleCode,
            boolean enabled,
            String status,
            Long assessorId,
            Long departmentId,
            List<Long> departmentIds) {}

    public record RoleView(
            Long id, String name, String roleCode, String scopeMode, boolean builtIn, List<String> permissions) {}

    public record SetupLink(String email, String setupUrl) {}

    @Getter
    @Setter
    public static class InviteForm {
        private String firstName;
        private String lastName;
        private String email;
        private Long roleId;
        private Long assessorId;
        private Long departmentId;
        private List<Long> departmentIds;
    }

    @Getter
    @Setter
    public static class UserPatch {
        private String firstName;
        private String lastName;
        private String email;
        private Boolean enabled;
        private Long roleId;
        private Boolean reissueSetup;
        private Long assessorId;
        private Long departmentId;
        private List<Long> departmentIds;
    }

    @Getter
    @Setter
    public static class RoleForm {
        private String name;
        private String scopeMode;
        private List<PermissionEnum> permissions;
    }
}

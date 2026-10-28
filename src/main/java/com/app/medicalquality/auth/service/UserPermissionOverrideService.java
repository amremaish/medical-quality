package com.app.medicalquality.auth.service;

import com.app.medicalquality.auth.dto.PermissionOverrideMatrix;
import com.app.medicalquality.auth.dto.PermissionOverrideMatrix.PermissionOverrideRow;
import com.app.medicalquality.auth.dto.UpdatePermissionOverridesRequest;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.entity.UserPermissionOverride;
import com.app.medicalquality.auth.entity.UserPermissionOverrideEvent;
import com.app.medicalquality.auth.repository.UserPermissionOverrideEventRepository;
import com.app.medicalquality.auth.repository.UserPermissionOverrideRepository;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.security.EffectivePermissionService;
import com.app.medicalquality.auth.security.EffectivePermissions;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.hospital.service.HospitalContext;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.role.PermissionText;
import com.app.medicalquality.role.UserPermissionOverridePolicy;
import com.app.medicalquality.role.entity.Permission;
import com.app.medicalquality.role.repository.PermissionRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPermissionOverrideService {

    private static final Set<String> EFFECTS = Set.of(
            EffectivePermissions.INHERIT, EffectivePermissions.ALLOW, EffectivePermissions.DENY);

    private final HospitalContext hospitalContext;
    private final CurrentUserService currentUserService;
    private final PermissionEvaluator permissionEvaluator;
    private final EffectivePermissionService effectivePermissionService;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionOverrideRepository overrideRepository;
    private final UserPermissionOverrideEventRepository eventRepository;

    public UserPermissionOverrideService(
            HospitalContext a_hospitalContext,
            CurrentUserService a_currentUserService,
            PermissionEvaluator a_permissionEvaluator,
            EffectivePermissionService a_effectivePermissionService,
            UserRepository a_userRepository,
            PermissionRepository a_permissionRepository,
            UserPermissionOverrideRepository a_overrideRepository,
            UserPermissionOverrideEventRepository a_eventRepository) {
        this.hospitalContext = a_hospitalContext;
        this.currentUserService = a_currentUserService;
        this.permissionEvaluator = a_permissionEvaluator;
        this.effectivePermissionService = a_effectivePermissionService;
        this.userRepository = a_userRepository;
        this.permissionRepository = a_permissionRepository;
        this.overrideRepository = a_overrideRepository;
        this.eventRepository = a_eventRepository;
    }

    @Transactional(readOnly = true)
    public PermissionOverrideMatrix matrix(Long a_organizationId, Long a_userId, Authentication a_authentication) {
        Organization organization = authorize(a_organizationId, a_authentication);
        User target = userRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(a_userId, organization.getId())
                .filter(a_user -> !a_user.isSuperAdmin())
                .orElseThrow(() -> new NotFoundException("error.user.not_found"));
        return matrixOf(target);
    }

    @Transactional
    public PermissionOverrideMatrix update(
            Long a_organizationId,
            Long a_userId,
            UpdatePermissionOverridesRequest a_request,
            Authentication a_authentication) {
        Organization organization = authorize(a_organizationId, a_authentication);
        User actor = currentUserService.require(a_authentication);
        User target = userRepository
                .lockInOrganization(a_userId, organization.getId())
                .filter(a_user -> !a_user.isSuperAdmin())
                .orElseThrow(() -> new NotFoundException("error.user.not_found"));
        List<UpdatePermissionOverridesRequest.Change> changes =
                a_request == null || a_request.getChanges() == null ? List.of() : a_request.getChanges();
        List<ValidatedChange> validated = new ArrayList<>();
        Set<PermissionEnum> seen = new HashSet<>();
        for (UpdatePermissionOverridesRequest.Change change : changes) {
            PermissionEnum permission = permissionOf(change);
            if (!seen.add(permission)) {
                throw new AppException("error.override.duplicate");
            }
            String effect = effectOf(change);
            if (permission == PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES
                    && actor.getId() != null
                    && actor.getId().equals(target.getId())) {
                throw new AppException("error.override.self_management");
            }
            if (!UserPermissionOverridePolicy.eligible(permission)) {
                throw new AppException("error.override.permission_forbidden", new Object[] {permission.name()});
            }
            validated.add(new ValidatedChange(permission, effect));
        }
        for (ValidatedChange change : validated) {
            apply(target, actor, change.permission(), change.effect());
        }
        return matrixOf(target);
    }

    private record ValidatedChange(PermissionEnum permission, String effect) {}

    private Organization authorize(Long a_organizationId, Authentication a_authentication) {
        Organization acting = hospitalContext.require(a_authentication);
        if (a_organizationId == null || !acting.getId().equals(a_organizationId)) {
            throw new AccessDeniedException("Access denied");
        }
        if (!permissionEvaluator.hasPermission(
                a_authentication, PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES.name())) {
            throw new AccessDeniedException("Access denied");
        }
        return acting;
    }

    private void apply(User a_target, User a_actor, PermissionEnum a_permission, String a_effect) {
        Permission permission = permissionRepository
                .findByName(a_permission.name())
                .filter(a_row -> a_row.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(
                        "error.role.permission_not_found", new Object[] {a_permission.name()}));
        UserPermissionOverride existing = overrideRepository
                .findByUser_IdAndPermission_IdAndDeletedAtIsNull(a_target.getId(), permission.getId())
                .orElse(null);
        String previous = existing == null ? null : existing.getEffect();
        String next = EffectivePermissions.INHERIT.equals(a_effect) ? null : a_effect;
        if (Objects.equals(previous, next)) {
            return;
        }
        try {
            if (next == null) {
                existing.setDeletedAt(LocalDateTime.now());
                existing.setUpdatedBy(a_actor);
                overrideRepository.saveAndFlush(existing);
            } else if (existing != null) {
                existing.setEffect(next);
                existing.setUpdatedBy(a_actor);
                overrideRepository.saveAndFlush(existing);
            } else {
                UserPermissionOverride created = new UserPermissionOverride();
                created.setOrganization(a_target.getOrganization());
                created.setUser(a_target);
                created.setPermission(permission);
                created.setEffect(next);
                created.setCreatedBy(a_actor);
                created.setUpdatedBy(a_actor);
                overrideRepository.saveAndFlush(created);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new AppException("error.override.conflict");
        }
        UserPermissionOverrideEvent event = new UserPermissionOverrideEvent();
        event.setOrganization(a_target.getOrganization());
        event.setUser(a_target);
        event.setPermission(permission);
        event.setPreviousEffect(previous);
        event.setNewEffect(next);
        event.setActor(a_actor);
        eventRepository.save(event);
    }

    private PermissionOverrideMatrix matrixOf(User a_target) {
        Set<String> roleNames = effectivePermissionService.roleNames(a_target.getRole());
        Map<String, String> effects = effectivePermissionService.effects(a_target.getId());
        List<PermissionOverrideRow> rows = new ArrayList<>();
        for (PermissionEnum permission : UserPermissionOverridePolicy.ELIGIBLE) {
            boolean roleGranted = roleNames.contains(permission.name());
            String effect = effects.getOrDefault(permission.name(), EffectivePermissions.INHERIT);
            if (!EffectivePermissions.ALLOW.equals(effect) && !EffectivePermissions.DENY.equals(effect)) {
                effect = EffectivePermissions.INHERIT;
            }
            rows.add(new PermissionOverrideRow(
                    permission.name(),
                    PermissionText.label(permission),
                    PermissionText.description(permission),
                    roleGranted,
                    EffectivePermissions.granted(roleGranted, effect, true),
                    effect,
                    EffectivePermissions.note(roleGranted, effect)));
        }
        return new PermissionOverrideMatrix(rows);
    }

    private PermissionEnum permissionOf(UpdatePermissionOverridesRequest.Change a_change) {
        String name = a_change == null ? null : a_change.getPermission();
        if (name == null || name.isBlank()) {
            throw new AppException("error.override.permission_forbidden", new Object[] {"none"});
        }
        try {
            return PermissionEnum.valueOf(name.trim());
        } catch (IllegalArgumentException ex) {
            throw new AppException("error.override.permission_forbidden", new Object[] {name});
        }
    }

    private String effectOf(UpdatePermissionOverridesRequest.Change a_change) {
        String effect = a_change == null ? null : a_change.getEffect();
        if (effect == null || !EFFECTS.contains(effect)) {
            throw new AppException("error.override.effect");
        }
        return effect;
    }
}

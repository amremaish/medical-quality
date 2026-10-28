package com.app.medicalquality.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.medicalquality.auth.dto.PermissionOverrideMatrix;
import com.app.medicalquality.auth.dto.UpdatePermissionOverridesRequest;
import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.entity.UserPermissionOverride;
import com.app.medicalquality.auth.entity.UserPermissionOverrideEvent;
import com.app.medicalquality.auth.repository.UserPermissionOverrideEventRepository;
import com.app.medicalquality.auth.repository.UserPermissionOverrideRepository;
import com.app.medicalquality.auth.repository.UserRepository;
import com.app.medicalquality.auth.security.EffectivePermissionService;
import com.app.medicalquality.auth.security.PermissionEvaluator;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.hospital.service.HospitalContext;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.role.PermissionEnum;
import com.app.medicalquality.role.entity.Permission;
import com.app.medicalquality.role.entity.Role;
import com.app.medicalquality.role.entity.RolePermission;
import com.app.medicalquality.role.repository.PermissionRepository;
import com.app.medicalquality.role.repository.RolePermissionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

class UserPermissionOverrideServiceTest {

    private final HospitalContext hospitalContext = mock(HospitalContext.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final PermissionEvaluator permissionEvaluator = mock(PermissionEvaluator.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PermissionRepository permissionRepository = mock(PermissionRepository.class);
    private final RolePermissionRepository rolePermissionRepository = mock(RolePermissionRepository.class);
    private final UserPermissionOverrideRepository overrideRepository = mock(UserPermissionOverrideRepository.class);
    private final UserPermissionOverrideEventRepository eventRepository = mock(UserPermissionOverrideEventRepository.class);
    private final List<UserPermissionOverride> stored = new ArrayList<>();
    private final List<UserPermissionOverrideEvent> events = new ArrayList<>();
    private final AtomicReference<UserPermissionOverride> untouched = new AtomicReference<>();
    private User actor;
    private User target;
    private Organization organization;
    private Role role;
    private UserPermissionOverrideService service;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(1L);
        role = new Role();
        role.setId(3L);
        actor = user(2L);
        target = user(5L);
        target.setRole(role);
        target.setOrganization(organization);
        User other = user(6L);
        UserPermissionOverride otherOverride = override(other, permission(PermissionEnum.CONDUCT_AUDIT, 11L), "ALLOW");
        untouched.set(otherOverride);
        when(hospitalContext.require(any())).thenReturn(organization);
        when(permissionEvaluator.hasPermission(any(), eq(PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES.name())))
                .thenReturn(true);
        when(currentUserService.require(any())).thenReturn(actor);
        when(userRepository.lockInOrganization(5L, 1L)).thenReturn(Optional.of(target));
        when(userRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.of(target));
        when(rolePermissionRepository.findByRoleAndDeletedAtIsNull(role)).thenReturn(List.of());
        when(overrideRepository.findByUser_IdAndDeletedAtIsNull(5L)).thenAnswer(invocation -> active(5L));
        when(overrideRepository.findByUser_IdAndPermission_IdAndDeletedAtIsNull(any(), any()))
                .thenAnswer(invocation -> active(invocation.getArgument(0)).stream()
                        .filter(row -> row.getPermission().getId().equals(invocation.getArgument(1)))
                        .findFirst());
        when(overrideRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            UserPermissionOverride row = invocation.getArgument(0);
            if (row == null || row.getUser() == null || row.getPermission() == null) {
                return row;
            }
            stored.removeIf(existing -> existing.getUser().getId().equals(row.getUser().getId())
                    && existing.getPermission().getId().equals(row.getPermission().getId())
                    && existing.getDeletedAt() == null
                    && existing != row);
            if (!stored.contains(row)) {
                stored.add(row);
            }
            return row;
        });
        when(eventRepository.save(any())).thenAnswer(invocation -> {
            events.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        service = new UserPermissionOverrideService(
                hospitalContext,
                currentUserService,
                permissionEvaluator,
                new EffectivePermissionService(rolePermissionRepository, overrideRepository),
                userRepository,
                permissionRepository,
                overrideRepository,
                eventRepository);
    }

    @Test
    void allowThenDenyThenInherit() {
        permit(PermissionEnum.CONDUCT_AUDIT, 11L);
        RolePermission link = new RolePermission();
        link.setPermission(permission(PermissionEnum.MANAGE_CAPA, 12L));
        when(rolePermissionRepository.findByRoleAndDeletedAtIsNull(role)).thenReturn(List.of(link));

        PermissionOverrideMatrix allowed = service.update(1L, 5L, request("CONDUCT_AUDIT", "ALLOW"), mock(Authentication.class));
        PermissionOverrideMatrix.PermissionOverrideRow conduct = row(allowed, "CONDUCT_AUDIT");
        assertEquals("ALLOW", conduct.effect());
        assertTrue(conduct.effective());
        assertEquals(false, conduct.roleGranted());
        assertTrue(conduct.note().contains("Explicit grant"));

        permit(PermissionEnum.MANAGE_CAPA, 12L);
        PermissionOverrideMatrix denied = service.update(1L, 5L, request("MANAGE_CAPA", "DENY"), mock(Authentication.class));
        PermissionOverrideMatrix.PermissionOverrideRow capa = row(denied, "MANAGE_CAPA");
        assertEquals("DENY", capa.effect());
        assertEquals(false, capa.effective());
        assertTrue(capa.roleGranted());
        assertTrue(capa.note().contains("Explicit denial"));

        PermissionOverrideMatrix inherited = service.update(1L, 5L, request("MANAGE_CAPA", "INHERIT"), mock(Authentication.class));
        PermissionOverrideMatrix.PermissionOverrideRow restored = row(inherited, "MANAGE_CAPA");
        assertEquals("INHERIT", restored.effect());
        assertTrue(restored.effective());
        assertEquals("ALLOW", untouched.get().getEffect());
        assertEquals(6L, untouched.get().getUser().getId());
        assertEquals(3, events.size());
        assertEquals(null, events.get(2).getNewEffect());
        assertEquals("DENY", events.get(2).getPreviousEffect());
    }

    @Test
    void aCallerWithoutTheManagementPermissionCannotReadOrChangeOverrides() {
        when(permissionEvaluator.hasPermission(any(), eq(PermissionEnum.MANAGE_USER_PERMISSION_OVERRIDES.name())))
                .thenReturn(false);
        Authentication authentication = mock(Authentication.class);
        assertThrows(AccessDeniedException.class, () -> service.matrix(1L, 5L, authentication));
        assertThrows(AccessDeniedException.class, () -> service.update(1L, 5L, request("CONDUCT_AUDIT", "ALLOW"), authentication));
        verify(overrideRepository, never()).saveAndFlush(any());
    }

    @Test
    void anotherOrganizationIsRejected() {
        Organization elsewhere = new Organization();
        elsewhere.setId(9L);
        when(hospitalContext.require(any())).thenReturn(elsewhere);
        Authentication authentication = mock(Authentication.class);
        assertThrows(AccessDeniedException.class, () -> service.matrix(1L, 5L, authentication));
        assertThrows(AccessDeniedException.class, () -> service.update(1L, 5L, request("CONDUCT_AUDIT", "DENY"), authentication));
        verify(overrideRepository, never()).saveAndFlush(any());
    }

    @Test
    void ineligibleAndSelfManagementPermissionsAreRejected() {
        Authentication authentication = mock(Authentication.class);
        AppException platform = assertThrows(
                AppException.class,
                () -> service.update(1L, 5L, request("RESET_DATA", "ALLOW"), authentication));
        assertEquals("error.override.permission_forbidden", platform.getMessageKey());
        AppException scope = assertThrows(
                AppException.class,
                () -> service.update(1L, 5L, request("VIEW_ALL_ASSESSORS_DATA", "ALLOW"), authentication));
        assertEquals("error.override.permission_forbidden", scope.getMessageKey());
        actor.setId(5L);
        AppException self = assertThrows(
                AppException.class,
                () -> service.update(1L, 5L, request("MANAGE_USER_PERMISSION_OVERRIDES", "ALLOW"), authentication));
        assertEquals("error.override.self_management", self.getMessageKey());
        verify(overrideRepository, never()).saveAndFlush(any());
        assertTrue(events.isEmpty());
    }

    @Test
    void aDuplicateActiveRowIsRejected() {
        permit(PermissionEnum.CONDUCT_AUDIT, 11L);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(overrideRepository)
                .saveAndFlush(any());
        stored.clear();
        AppException conflict = assertThrows(
                AppException.class,
                () -> service.update(1L, 5L, request("CONDUCT_AUDIT", "ALLOW"), mock(Authentication.class)));
        assertEquals("error.override.conflict", conflict.getMessageKey());
        verify(eventRepository, never()).save(any());
    }

    private void permit(PermissionEnum a_permission, long a_id) {
        when(permissionRepository.findByName(a_permission.name())).thenReturn(Optional.of(permission(a_permission, a_id)));
    }

    private List<UserPermissionOverride> active(Long a_userId) {
        return stored.stream()
                .filter(row -> row.getUser().getId().equals(a_userId) && row.getDeletedAt() == null)
                .toList();
    }

    private static UserPermissionOverride override(User a_user, Permission a_permission, String a_effect) {
        UserPermissionOverride row = new UserPermissionOverride();
        row.setUser(a_user);
        row.setPermission(a_permission);
        row.setEffect(a_effect);
        return row;
    }

    private static PermissionOverrideMatrix.PermissionOverrideRow row(PermissionOverrideMatrix a_matrix, String a_name) {
        return a_matrix.permissions().stream()
                .filter(row -> a_name.equals(row.permission()))
                .findFirst()
                .orElseThrow();
    }

    private static UpdatePermissionOverridesRequest request(String a_permission, String a_effect) {
        UpdatePermissionOverridesRequest.Change change = new UpdatePermissionOverridesRequest.Change();
        change.setPermission(a_permission);
        change.setEffect(a_effect);
        UpdatePermissionOverridesRequest request = new UpdatePermissionOverridesRequest();
        request.setChanges(List.of(change));
        return request;
    }

    private User user(Long a_id) {
        User user = new User();
        user.setId(a_id);
        user.setOrganization(organization);
        return user;
    }

    private static Permission permission(PermissionEnum a_permission, long a_id) {
        Permission permission = new Permission();
        permission.setId(a_id);
        permission.setName(a_permission.name());
        return permission;
    }
}

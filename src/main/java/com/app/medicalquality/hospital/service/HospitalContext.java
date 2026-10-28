package com.app.medicalquality.hospital.service;

import com.app.medicalquality.auth.entity.User;
import com.app.medicalquality.auth.service.CurrentUserService;
import com.app.medicalquality.exception.AppException;
import com.app.medicalquality.exception.NotFoundException;
import com.app.medicalquality.hospital.entity.UserDepartmentAccess;
import com.app.medicalquality.hospital.repository.UserDepartmentAccessRepository;
import com.app.medicalquality.organization.entity.Organization;
import com.app.medicalquality.organization.repository.OrganizationRepository;
import com.app.medicalquality.role.RoleCodes;
import com.app.medicalquality.role.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class HospitalContext {

    public static final String ORGANIZATION_HEADER = "X-Organization-Id";

    private final CurrentUserService currentUserService;
    private final OrganizationRepository organizationRepository;
    private final UserDepartmentAccessRepository accessRepository;

    public HospitalContext(
            CurrentUserService a_currentUserService,
            OrganizationRepository a_organizationRepository,
            UserDepartmentAccessRepository a_accessRepository) {
        this.currentUserService = a_currentUserService;
        this.organizationRepository = a_organizationRepository;
        this.accessRepository = a_accessRepository;
    }

    @Transactional(readOnly = true)
    public Organization require(Authentication a_authentication) {
        User user = currentUserService.require(a_authentication);
        Long headerId = headerOrganizationId();
        if (user.isSuperAdmin()) {
            if (headerId == null) {
                throw new AppException("error.hospital.required");
            }
            return organization(headerId);
        }
        Organization own = ownOrganization(user);
        if (own == null) {
            throw new AppException("error.hospital.required");
        }
        if (headerId != null && !own.getId().equals(headerId)) {
            throw new AccessDeniedException("Access denied");
        }
        return own;
    }

    @Transactional(readOnly = true)
    public void requireManager(Authentication a_authentication) {
        User user = currentUserService.require(a_authentication);
        Organization acting = require(a_authentication);
        if (user.isSuperAdmin()) {
            return;
        }
        Role role = user.getRole();
        if (role == null
                || !role.isBuiltIn()
                || !RoleCodes.HOSPITAL_OWNER.equals(role.getRoleCode())
                || role.getOrganization() == null
                || !acting.getId().equals(role.getOrganization().getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    public void requireDirectoryAdmin(Authentication a_authentication, Long a_organizationId) {
        Organization acting = require(a_authentication);
        if (a_organizationId == null || !acting.getId().equals(a_organizationId)) {
            throw new AccessDeniedException("Access denied");
        }
        requireManager(a_authentication);
    }

    public boolean managesDirectory(Authentication a_authentication) {
        try {
            requireManager(a_authentication);
            return true;
        } catch (AccessDeniedException ex) {
            return false;
        }
    }

    private Organization ownOrganization(User a_user) {
        if (a_user.getOrganization() != null && a_user.getOrganization().getDeletedAt() == null) {
            return a_user.getOrganization();
        }
        if (a_user.getDepartment() != null
                && a_user.getDepartment().getOrganization() != null
                && a_user.getDepartment().getOrganization().getDeletedAt() == null) {
            return a_user.getDepartment().getOrganization();
        }
        if (a_user.getAssessor() != null
                && a_user.getAssessor().getOrganization() != null
                && a_user.getAssessor().getOrganization().getDeletedAt() == null) {
            return a_user.getAssessor().getOrganization();
        }
        for (UserDepartmentAccess access : accessRepository.findByUserIdAndDeletedAtIsNull(a_user.getId())) {
            if (access.getDepartment() != null
                    && access.getDepartment().getOrganization() != null
                    && access.getDepartment().getOrganization().getDeletedAt() == null) {
                return access.getDepartment().getOrganization();
            }
        }
        return null;
    }

    private Organization organization(Long a_id) {
        return organizationRepository
                .findById(a_id)
                .filter(a_organization -> a_organization.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("error.organization.not_found"));
    }

    private Long headerOrganizationId() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String value = request.getHeader(ORGANIZATION_HEADER);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new NotFoundException("error.organization.not_found");
        }
    }
}

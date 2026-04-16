package org.backend.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.backend.auth.dto.ApiResponse;
import org.backend.backend.auth.dto.AssignRolesRequest;
import org.backend.backend.auth.dto.UserDTO;
import org.backend.backend.auth.exception.UserNotFoundException;
import org.backend.backend.auth.model.Role;
import org.backend.backend.auth.model.RoleType;
import org.backend.backend.auth.model.User;
import org.backend.backend.auth.repository.RoleRepository;
import org.backend.backend.auth.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;

    /**
     * Return paginated list of all users (lightweight DTOs).
     */
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(authService::convertToUserDTO);
    }

    /**
     * Find a single user by ID.
     */
    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(authService::convertToUserDTO)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    /**
     * Replace the user's role set with the provided roles.
     * Guards: at least one admin must remain in the system.
     */
    @Transactional
    public ApiResponse assignRoles(Long userId, AssignRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        Set<Role> newRoles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            RoleType roleType;
            try {
                roleType = RoleType.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + roleName +
                        ". Valid roles: ROLE_USER, ROLE_ADMIN, ROLE_VENDOR");
            }
            Role role = roleRepository.findByName(roleType)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            newRoles.add(role);
        }

        // Safety: prevent removing themselves as last admin
        boolean wasAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN);
        boolean stillAdmin = newRoles.stream()
                .anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN);

        if (wasAdmin && !stillAdmin) {
            long adminCount = userRepository.countByRoleName(RoleType.ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot remove the last ADMIN from the system");
            }
        }

        user.setRoles(newRoles);
        userRepository.save(user);
        log.info("Roles updated for user={} → {}", user.getEmail(), request.getRoles());

        return new ApiResponse(true, "Roles updated successfully", authService.convertToUserDTO(user));
    }

    /**
     * Enable or disable a user account.
     */
    @Transactional
    public ApiResponse setUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        user.setEnabled(enabled);
        userRepository.save(user);
        String status = enabled ? "enabled" : "disabled";
        log.info("User {} account {}", user.getEmail(), status);

        return new ApiResponse(true, "User account " + status, authService.convertToUserDTO(user));
    }

    /**
     * Soft delete: disable account.
     */
    @Transactional
    public ApiResponse deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Safety: can't delete last admin
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleType.ROLE_ADMIN);
        if (isAdmin) {
            long adminCount = userRepository.countByRoleName(RoleType.ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new IllegalStateException("Cannot delete the last ADMIN from the system");
            }
        }

        user.setEnabled(false);
        userRepository.save(user);
        return new ApiResponse(true, "User disabled (soft deleted)", null);
    }

    /**
     * Admin global stats for dashboard.
     */
    public java.util.Map<String, Object> getDashboardStats() {
        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.countByEnabledTrue();
        long adminCount = userRepository.countByRoleName(RoleType.ROLE_ADMIN);
        long vendorCount = userRepository.countByRoleName(RoleType.ROLE_VENDOR);

        return java.util.Map.of(
                "totalUsers", totalUsers,
                "enabledUsers", enabledUsers,
                "adminCount", adminCount,
                "vendorCount", vendorCount
        );
    }
}

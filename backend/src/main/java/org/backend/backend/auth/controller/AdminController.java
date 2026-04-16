package org.backend.backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.backend.backend.auth.dto.ApiResponse;
import org.backend.backend.auth.dto.AssignRolesRequest;
import org.backend.backend.auth.dto.UserDTO;
import org.backend.backend.auth.service.AdminUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin", description = "Administration endpoints — requires ROLE_ADMIN")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Admin dashboard statistics")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse> getDashboard() {
        return ResponseEntity.ok(new ApiResponse(true, "Admin dashboard", adminUserService.getDashboardStats()));
    }

    @Operation(summary = "Get all users (paginated)")
    @GetMapping("/users")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(adminUserService.getAllUsers(pageable));
    }

    @Operation(summary = "Get user by ID")
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse(true, "User found", adminUserService.getUserById(id)));
    }

    @Operation(summary = "Assign roles to a user (replaces existing roles)")
    @PatchMapping("/users/{id}/roles")
    public ResponseEntity<ApiResponse> assignRoles(
            @PathVariable Long id,
            @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(adminUserService.assignRoles(id, request));
    }

    @Operation(summary = "Enable or disable user account")
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse> setStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        return ResponseEntity.ok(adminUserService.setUserStatus(id, enabled));
    }

    @Operation(summary = "Soft-delete a user account (disables it)")
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.deleteUser(id));
    }
}

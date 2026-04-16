package org.backend.backend.auth.config;

import lombok.RequiredArgsConstructor;
import org.backend.backend.auth.model.Role;
import org.backend.backend.auth.model.RoleType;
import org.backend.backend.auth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        // Initialize roles if they don't exist
        for (RoleType roleType : RoleType.values()) {
            if (roleRepository.findByName(roleType).isEmpty()) {
                Role role = new Role();
                role.setName(roleType);
                roleRepository.save(role);
            }
        }
    }
}
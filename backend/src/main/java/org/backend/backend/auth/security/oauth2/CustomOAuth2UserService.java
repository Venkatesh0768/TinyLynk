package org.backend.backend.auth.security.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.backend.auth.model.Role;
import org.backend.backend.auth.model.RoleType;
import org.backend.backend.auth.model.User;
import org.backend.backend.auth.repository.RoleRepository;
import org.backend.backend.auth.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google" | "github"
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email      = extractEmail(provider, attributes);
        String firstName  = extractFirstName(provider, attributes);
        String lastName   = extractLastName(provider, attributes);
        String providerId = extractProviderId(provider, attributes);
        String imageUrl   = extractImageUrl(provider, attributes);

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not returned by " + provider + " provider");
        }

        User user = userRepository.findByEmail(email)
                .map(existing -> updateExistingUser(existing, provider, providerId, imageUrl))
                .orElseGet(() -> createNewUser(email, firstName, lastName, provider, providerId, imageUrl));

        return new OAuth2UserPrincipal(user, attributes);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private User updateExistingUser(User user, String provider, String providerId, String imageUrl) {
        user.setProvider(provider);
        user.setProviderId(providerId);
        if (imageUrl != null) user.setProfileImageUrl(imageUrl);
        return userRepository.save(user);
    }

    private User createNewUser(String email, String firstName, String lastName,
                               String provider, String providerId, String imageUrl) {
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found — did DataInitializer run?"));

        User user = User.builder()
                .email(email)
                .password(null)          // No local password for social users
                .firstName(firstName != null ? firstName : "")
                .lastName(lastName != null ? lastName : "")
                .emailVerified(true)     // Trusted — email already verified by provider
                .enabled(true)
                .provider(provider)
                .providerId(providerId)
                .profileImageUrl(imageUrl)
                .roles(Set.of(userRole))
                .build();

        log.info("Creating new OAuth2 user from provider={} email={}", provider, email);
        return userRepository.save(user);
    }

    // ─── Attribute extractors per provider ───────────────────────────────────

    private String extractEmail(String provider, Map<String, Object> attrs) {
        return switch (provider) {
            case "google" -> (String) attrs.get("email");
            case "github" -> (String) attrs.get("email");
            default       -> null;
        };
    }

    private String extractFirstName(String provider, Map<String, Object> attrs) {
        return switch (provider) {
            case "google" -> (String) attrs.get("given_name");
            case "github" -> {
                String name = (String) attrs.get("name");
                yield name != null && name.contains(" ") ? name.split(" ")[0] : name;
            }
            default -> "";
        };
    }

    private String extractLastName(String provider, Map<String, Object> attrs) {
        return switch (provider) {
            case "google" -> (String) attrs.get("family_name");
            case "github" -> {
                String name = (String) attrs.get("name");
                yield name != null && name.contains(" ") ? name.split(" ", 2)[1] : "";
            }
            default -> "";
        };
    }

    private String extractProviderId(String provider, Map<String, Object> attrs) {
        return switch (provider) {
            case "google" -> (String) attrs.get("sub");
            case "github" -> String.valueOf(attrs.get("id"));
            default       -> null;
        };
    }

    private String extractImageUrl(String provider, Map<String, Object> attrs) {
        return switch (provider) {
            case "google" -> (String) attrs.get("picture");
            case "github" -> (String) attrs.get("avatar_url");
            default       -> null;
        };
    }
}

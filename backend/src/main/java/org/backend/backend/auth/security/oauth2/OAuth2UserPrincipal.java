package org.backend.backend.auth.security.oauth2;

import org.backend.backend.auth.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;


public class OAuth2UserPrincipal implements OAuth2User, UserDetails {

    private final User user;
    private final Map<String, Object> attributes;

    public OAuth2UserPrincipal(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    /** Returns the underlying domain {@link User} entity. */
    public User getUser() {
        return user;
    }

    // ─── OAuth2User ──────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    // ─── UserDetails ─────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }

    @Override public String getPassword()                  { return null; }
    @Override public String getUsername()                  { return user.getEmail(); }
    @Override public boolean isAccountNonExpired()         { return true; }
    @Override public boolean isAccountNonLocked()          { return user.isEnabled(); }
    @Override public boolean isCredentialsNonExpired()     { return true; }
    @Override public boolean isEnabled()                   { return user.isEmailVerified(); }
}

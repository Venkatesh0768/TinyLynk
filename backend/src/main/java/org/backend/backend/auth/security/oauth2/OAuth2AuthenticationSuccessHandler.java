package org.backend.backend.auth.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.backend.backend.auth.model.User;
import org.backend.backend.auth.security.JwtTokenProvider;
import org.backend.backend.auth.service.OTPService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final OTPService otpService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        User user = principal.getUser();

        // Generate JWT access token (roles embedded in claims)
        String accessToken = tokenProvider.generateTokenForUser(user);

        // Create / rotate refresh token
        otpService.createRefreshToken(user);

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("token", accessToken)
                .build().toUriString();

        log.info("OAuth2 login success for user={} provider={}", user.getEmail(), user.getProvider());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

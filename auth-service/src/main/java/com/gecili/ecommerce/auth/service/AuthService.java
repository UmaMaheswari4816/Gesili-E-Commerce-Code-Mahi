package com.gecili.ecommerce.auth.service;

import com.gecili.ecommerce.auth.dto.AuthDtos.LoginRequest;
import com.gecili.ecommerce.auth.dto.AuthDtos.RegisterRequest;
import com.gecili.ecommerce.auth.dto.AuthDtos.TokenResponse;
import com.gecili.ecommerce.auth.dto.AuthDtos.UserResponse;
import com.gecili.ecommerce.auth.entity.UserAccount;
import com.gecili.ecommerce.auth.repository.UserAccountRepository;
import com.gecili.ecommerce.auth.security.JwtService;
import com.gecili.ecommerce.auth.service.RefreshTokenService.IssuedToken;
import com.gecili.ecommerce.auth.service.RefreshTokenService.RotationResult;
import com.gecili.ecommerce.common.exception.ConflictException;
import com.gecili.ecommerce.common.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Core auth use-cases: register, login, refresh, logout, profile lookup. */
@Slf4j
@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokens;

    public AuthService(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ConflictException("Email already registered");
        }
        UserAccount user =
                UserAccount.create(
                        req.email().toLowerCase(),
                        passwordEncoder.encode(req.password()),
                        req.displayName());
        users.save(user);
        log.info("Registered user {}", user.getId());
        return toResponse(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        UserAccount user =
                users
                        .findByEmailIgnoreCase(req.email())
                        .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isEnabled() || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        String access = jwtService.issueAccessToken(user);
        IssuedToken refresh = refreshTokens.issueNewFamily(user.getId());
        return new TokenResponse(access, refresh.rawToken(), "Bearer", jwtService.accessTtlSeconds());
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RotationResult result = refreshTokens.rotate(refreshToken);
        UserAccount user =
                users
                        .findById(result.userId())
                        .orElseThrow(() -> ResourceNotFoundException.of("User", result.userId()));
        String access = jwtService.issueAccessToken(user);
        return new TokenResponse(
                access, result.issued().rawToken(), "Bearer", jwtService.accessTtlSeconds());
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokens.revokeAllForUser(userId);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId) {
        return users
                .findById(userId)
                .map(this::toResponse)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    private UserResponse toResponse(UserAccount u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getRoles());
    }
}

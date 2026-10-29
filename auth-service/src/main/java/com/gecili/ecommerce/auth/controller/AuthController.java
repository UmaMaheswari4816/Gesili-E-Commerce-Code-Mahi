package com.gecili.ecommerce.auth.controller;

import com.gecili.ecommerce.auth.dto.AuthDtos.LoginRequest;
import com.gecili.ecommerce.auth.dto.AuthDtos.RefreshRequest;
import com.gecili.ecommerce.auth.dto.AuthDtos.RegisterRequest;
import com.gecili.ecommerce.auth.dto.AuthDtos.TokenResponse;
import com.gecili.ecommerce.auth.dto.AuthDtos.UserResponse;
import com.gecili.ecommerce.auth.service.AuthService;
import com.gecili.ecommerce.common.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public auth endpoints plus an authenticated profile endpoint. */
@RestController
@RequestMapping(ApiConstants.API_V1 + "/auth")
@Tag(name = "Authentication", description = "Registration, login and token management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @Operation(summary = "Log in and receive an access + refresh token pair")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated"),
        @ApiResponse(responseCode = "400", description = "Validation failed"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @Operation(summary = "Exchange a refresh token for a new token pair (rotating)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New token pair issued"),
        @ApiResponse(responseCode = "409", description = "Invalid/expired/reused refresh token")
    })
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @Operation(summary = "Log out (revoke all refresh tokens)", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logged out"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        authService.logout(UUID.fromString(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get the authenticated user's profile", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.getById(UUID.fromString(jwt.getSubject()));
    }
}

package com.gecili.ecommerce.auth.dto;

import com.gecili.ecommerce.common.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

/** Request/response DTOs for the auth API, with OpenAPI schema annotations. */
public final class AuthDtos {

    private AuthDtos() {}

    @Schema(name = "RegisterRequest", description = "New user registration payload")
    public record RegisterRequest(
            @Schema(description = "Unique email address", example = "jane@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
                    @NotBlank
                    @Email
                    String email,
            @Schema(description = "Password (min 8 chars)", example = "S3curePass!", requiredMode = Schema.RequiredMode.REQUIRED)
                    @NotBlank
                    @Size(min = 8, max = 100)
                    String password,
            @Schema(description = "Display name", example = "Jane Doe")
                    @Size(max = 100)
                    String displayName) {}

    @Schema(name = "LoginRequest", description = "Login payload")
    public record LoginRequest(
            @Schema(example = "jane@example.com", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank @Email String email,
            @Schema(example = "S3curePass!", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String password) {}

    @Schema(name = "RefreshRequest", description = "Exchange a refresh token for a new token pair")
    public record RefreshRequest(
            @Schema(description = "The current refresh token", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank String refreshToken) {}

    @Schema(name = "TokenResponse", description = "Issued token pair")
    public record TokenResponse(
            @Schema(description = "Short-lived RS256 access token (JWT)") String accessToken,
            @Schema(description = "Long-lived rotating refresh token") String refreshToken,
            @Schema(description = "Token type", example = "Bearer") String tokenType,
            @Schema(description = "Access token lifetime in seconds", example = "900") long expiresIn) {}

    @Schema(name = "UserResponse", description = "Public user profile")
    public record UserResponse(
            @Schema(example = "3f2504e0-4f89-41d3-9a0c-0305e82c3301") UUID id,
            @Schema(example = "jane@example.com") String email,
            @Schema(example = "Jane Doe") String displayName,
            @Schema(description = "Assigned roles") Set<Role> roles) {}
}

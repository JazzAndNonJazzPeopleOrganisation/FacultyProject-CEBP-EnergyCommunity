package EnergyCommunity.core.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Authentication response containing the administrator session token and profile")
public record AdminAuthResponse(
        @Schema(description = "Session token to pass in the X-Admin-Token header for protected admin endpoints",
                example = "dGhpcyBpcyBhIHNlY3VyZSBhZG1pbiB0b2tlbg")
        String token,

        @Schema(description = "Expiration timestamp of the session token (UTC)",
                example = "2026-11-29T18:00:00Z")
        Instant expiresAt,

        @Schema(description = "Authenticated administrator details")
        Admin admin
) {
}

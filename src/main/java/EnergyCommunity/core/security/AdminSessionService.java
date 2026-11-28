package EnergyCommunity.core.security;

import EnergyCommunity.core.model.Admin;
import EnergyCommunity.core.model.AdminAuthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active administrator session tokens and validates access to protected Admin endpoints.
 */
@Component
public class AdminSessionService {

    public static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    public static final String AUTHENTICATED_ADMIN_ID_ATTR = "authenticatedAdminId";
    public static final String AUTHENTICATED_ADMIN_TOKEN_ATTR = "authenticatedAdminToken";

    private static final Duration SESSION_TTL = Duration.ofHours(12);
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SessionEntry> activeSessions = new ConcurrentHashMap<>();

    private record SessionEntry(Long adminId, String username, Instant expiresAt) {
    }

    public AdminAuthResponse createSession(Admin admin) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant expiresAt = Instant.now().plus(SESSION_TTL);

        activeSessions.put(token, new SessionEntry(admin.getId(), admin.getUsername(), expiresAt));
        return new AdminAuthResponse(token, expiresAt, admin);
    }

    /**
     * Validates the session token and returns the authenticated administrator's ID.
     */
    public Long validateAndGetAdminId(String rawToken) {
        String token = extractToken(rawToken);
        if (token == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Missing admin session token in " + ADMIN_TOKEN_HEADER + " header");
        }
        SessionEntry session = activeSessions.get(token);
        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid or unknown admin session token");
        }
        if (Instant.now().isAfter(session.expiresAt())) {
            activeSessions.remove(token);
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Admin session token has expired; please log in again");
        }
        return session.adminId();
    }

    public void invalidateToken(String rawToken) {
        String token = extractToken(rawToken);
        if (token == null || activeSessions.remove(token) == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid or already expired admin session token");
        }
    }

    /**
     * Revokes all active session tokens belonging to the specified administrator ID.
     */
    public void invalidateSessionsForAdmin(Long adminId) {
        if (adminId == null) {
            return;
        }
        activeSessions.entrySet().removeIf(entry -> adminId.equals(entry.getValue().adminId()));
    }

    public String extractToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        String trimmed = rawToken.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String bearerToken = trimmed.substring(7).trim();
            return bearerToken.isEmpty() ? null : bearerToken;
        }
        return trimmed;
    }
}

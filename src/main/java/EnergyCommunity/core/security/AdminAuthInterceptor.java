package EnergyCommunity.core.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC HandlerInterceptor that automatically enforces {@code X-Admin-Token} authentication
 * on all {@code /api/v1/admin/**} endpoints (except {@code /api/v1/admin/login}).
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final AdminSessionService adminSessionService;

    public AdminAuthInterceptor(AdminSessionService adminSessionService) {
        this.adminSessionService = adminSessionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String rawToken = request.getHeader(AdminSessionService.ADMIN_TOKEN_HEADER);
        if (rawToken == null || rawToken.isBlank()) {
            rawToken = request.getHeader("Authorization");
        }
        Long adminId = adminSessionService.validateAndGetAdminId(rawToken);
        request.setAttribute(AdminSessionService.AUTHENTICATED_ADMIN_ID_ATTR, adminId);
        request.setAttribute(AdminSessionService.AUTHENTICATED_ADMIN_TOKEN_ATTR, rawToken);
        return true;
    }
}

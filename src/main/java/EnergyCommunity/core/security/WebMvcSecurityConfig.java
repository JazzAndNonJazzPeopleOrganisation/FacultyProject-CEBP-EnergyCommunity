package EnergyCommunity.core.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link AdminAuthInterceptor} on all {@code /api/v1/admin/**} routes except {@code /api/v1/admin/login}.
 */
@Configuration
public class WebMvcSecurityConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    public WebMvcSecurityConfig(AdminAuthInterceptor adminAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/v1/admin", "/api/v1/admin/**")
                .excludePathPatterns("/api/v1/admin/login");
    }
}

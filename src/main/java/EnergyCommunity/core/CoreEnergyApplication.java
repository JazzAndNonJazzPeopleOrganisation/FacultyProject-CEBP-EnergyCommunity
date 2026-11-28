package EnergyCommunity.core;

import EnergyCommunity.core.security.AdminSessionService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "Core Energy Service API",
                version = "1.0",
                description = "Single source of truth for managing users, smart houses, batteries, and solar panels in the Energy Community."
        )
)
@SecurityScheme(
        name = "AdminToken",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.HEADER,
        paramName = AdminSessionService.ADMIN_TOKEN_HEADER,
        description = "Admin session token returned by POST /api/v1/admin/login"
)
public class CoreEnergyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreEnergyApplication.class, args);
    }
}

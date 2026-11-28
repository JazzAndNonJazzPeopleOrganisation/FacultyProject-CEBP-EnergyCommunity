package EnergyCommunity.core.security;

import EnergyCommunity.core.model.Admin;
import EnergyCommunity.core.repository.AdminRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures a default bootstrap administrator account exists in the database on application startup.
 */
@Component
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final AdminRepository adminRepository;
    private final String defaultUsername;
    private final String defaultEmail;
    private final String defaultPassword;

    public AdminDataInitializer(
            AdminRepository adminRepository,
            @Value("${energy.admin.default.username:admin}") String defaultUsername,
            @Value("${energy.admin.default.email:admin@energy.ro}") String defaultEmail,
            @Value("${energy.admin.default.password:Admin123!}") String defaultPassword) {
        this.adminRepository = adminRepository;
        this.defaultUsername = defaultUsername;
        this.defaultEmail = defaultEmail;
        this.defaultPassword = defaultPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!adminRepository.existsByUsername(defaultUsername) && !adminRepository.existsByEmail(defaultEmail)) {
            Admin bootstrapAdmin = new Admin(
                    null,
                    defaultUsername,
                    defaultEmail,
                    PasswordHasher.hash(defaultPassword)
            );
            adminRepository.save(bootstrapAdmin);
            log.info("Default bootstrap administrator created (username='{}', email='{}')",
                    defaultUsername, defaultEmail);
        }
    }
}

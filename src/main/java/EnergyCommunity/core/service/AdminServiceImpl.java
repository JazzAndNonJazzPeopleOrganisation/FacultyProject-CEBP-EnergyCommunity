package EnergyCommunity.core.service;

import EnergyCommunity.core.model.Admin;
import EnergyCommunity.core.model.AdminAuthResponse;
import EnergyCommunity.core.model.User;
import EnergyCommunity.core.repository.AdminRepository;
import EnergyCommunity.core.security.AdminSessionService;
import EnergyCommunity.core.security.PasswordHasher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final UserService userService;
    private final AdminSessionService adminSessionService;

    public AdminServiceImpl(AdminRepository adminRepository,
                            UserService userService,
                            AdminSessionService adminSessionService) {
        this.adminRepository = adminRepository;
        this.userService = userService;
        this.adminSessionService = adminSessionService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> findAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Admin findAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Admin not found with id: " + id));
    }

    @Override
    public Admin createAdmin(Admin admin) {
        if (admin.getUsername() != null && adminRepository.existsByUsername(admin.getUsername())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "An admin with username " + admin.getUsername() + " already exists");
        }
        if (admin.getEmail() != null && adminRepository.existsByEmail(admin.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "An admin with email " + admin.getEmail() + " already exists");
        }
        if (admin.getPassword() == null || admin.getPassword().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Admin password is required");
        }
        admin.setPassword(PasswordHasher.hash(admin.getPassword()));
        return adminRepository.save(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAuthResponse authenticateAdmin(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username/email and password are required");
        }
        String identifier = usernameOrEmail.trim();
        Admin admin = adminRepository.findByUsername(identifier)
                .or(() -> adminRepository.findByEmail(identifier))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid admin credentials"));

        if (!PasswordHasher.verify(password, admin.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
        }
        return adminSessionService.createSession(admin);
    }

    @Override
    public void logoutAdmin(String adminToken) {
        adminSessionService.invalidateToken(adminToken);
    }

    @Override
    public void requireValidSession(String adminToken) {
        adminSessionService.validateToken(adminToken);
    }

    @Override
    public void deleteAdmin(Long id) {
        Admin existing = findAdminById(id);
        adminRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUserAccounts() {
        return userService.findAllUsers();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserAccountById(Long userId) {
        return userService.findUserById(userId);
    }

    @Override
    public void deleteUserAccount(Long userId) {
        userService.deleteUser(userId);
    }
}

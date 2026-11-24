package EnergyCommunity.core.service;

import EnergyCommunity.core.model.Admin;
import EnergyCommunity.core.model.User;
import EnergyCommunity.core.repository.AdminRepository;
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

    public AdminServiceImpl(AdminRepository adminRepository, UserService userService) {
        this.adminRepository = adminRepository;
        this.userService = userService;
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
        return adminRepository.save(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public Admin authenticateAdmin(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username/email and password are required");
        }
        Admin admin = adminRepository.findByUsername(usernameOrEmail)
                .or(() -> adminRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid admin credentials"));

        if (!password.equals(admin.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
        }
        return admin;
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

package EnergyCommunity.core.controller;

import EnergyCommunity.core.model.Admin;
import EnergyCommunity.core.model.AdminAuthResponse;
import EnergyCommunity.core.model.User;
import EnergyCommunity.core.security.AdminSessionService;
import EnergyCommunity.core.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController implements AdminAPI {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    @GetMapping
    public ResponseEntity<List<Admin>> getAllAdmins() {
        return ResponseEntity.ok(adminService.findAllAdmins());
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<Admin> getAdminById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.findAdminById(id));
    }

    @Override
    @PostMapping
    public ResponseEntity<Admin> createAdmin(@Valid @RequestBody Admin admin) {
        Admin created = adminService.createAdmin(admin);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AdminAuthResponse> loginAdmin(
            @RequestParam String usernameOrEmail,
            @RequestParam String password) {
        return ResponseEntity.ok(adminService.authenticateAdmin(usernameOrEmail, password));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logoutAdmin(
            @RequestAttribute(AdminSessionService.AUTHENTICATED_ADMIN_TOKEN_ATTR) String adminToken) {
        adminService.logoutAdmin(adminToken);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(
            @PathVariable Long id,
            @RequestAttribute(AdminSessionService.AUTHENTICATED_ADMIN_ID_ATTR) Long currentAdminId) {
        adminService.deleteAdmin(id, currentAdminId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUserAccounts() {
        return ResponseEntity.ok(adminService.getAllUserAccounts());
    }

    @Override
    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUserAccountById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserAccountById(userId));
    }

    @Override
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUserAccount(@PathVariable Long userId) {
        adminService.deleteUserAccount(userId);
        return ResponseEntity.noContent().build();
    }
}

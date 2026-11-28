package EnergyCommunity.core.controller;

import EnergyCommunity.core.model.Admin;
import EnergyCommunity.core.model.AdminAuthResponse;
import EnergyCommunity.core.model.User;
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
    public ResponseEntity<List<Admin>> getAllAdmins(@RequestHeader("X-Admin-Token") String adminToken) {
        adminService.requireValidSession(adminToken);
        return ResponseEntity.ok(adminService.findAllAdmins());
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<Admin> getAdminById(
            @RequestHeader("X-Admin-Token") String adminToken,
            @PathVariable Long id) {
        adminService.requireValidSession(adminToken);
        return ResponseEntity.ok(adminService.findAdminById(id));
    }

    @Override
    @PostMapping
    public ResponseEntity<Admin> createAdmin(
            @RequestHeader("X-Admin-Token") String adminToken,
            @Valid @RequestBody Admin admin) {
        adminService.requireValidSession(adminToken);
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
    public ResponseEntity<Void> logoutAdmin(@RequestHeader("X-Admin-Token") String adminToken) {
        adminService.logoutAdmin(adminToken);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdmin(
            @RequestHeader("X-Admin-Token") String adminToken,
            @PathVariable Long id) {
        adminService.requireValidSession(adminToken);
        adminService.deleteAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUserAccounts(@RequestHeader("X-Admin-Token") String adminToken) {
        adminService.requireValidSession(adminToken);
        return ResponseEntity.ok(adminService.getAllUserAccounts());
    }

    @Override
    @GetMapping("/users/{userId}")
    public ResponseEntity<User> getUserAccountById(
            @RequestHeader("X-Admin-Token") String adminToken,
            @PathVariable Long userId) {
        adminService.requireValidSession(adminToken);
        return ResponseEntity.ok(adminService.getUserAccountById(userId));
    }

    @Override
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUserAccount(
            @RequestHeader("X-Admin-Token") String adminToken,
            @PathVariable Long userId) {
        adminService.requireValidSession(adminToken);
        adminService.deleteUserAccount(userId);
        return ResponseEntity.noContent().build();
    }
}

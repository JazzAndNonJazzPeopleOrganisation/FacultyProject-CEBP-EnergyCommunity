package EnergyCommunity.core.service;

import EnergyCommunity.core.model.Admin;
import EnergyCommunity.core.model.AdminAuthResponse;
import EnergyCommunity.core.model.User;

import java.util.List;

public interface AdminService {
    List<Admin> findAllAdmins();
    Admin findAdminById(Long id);
    Admin createAdmin(Admin admin);
    AdminAuthResponse authenticateAdmin(String usernameOrEmail, String password);
    void logoutAdmin(String adminToken);
    void requireValidSession(String adminToken);
    void deleteAdmin(Long id);

    List<User> getAllUserAccounts();
    User getUserAccountById(Long userId);
    void deleteUserAccount(Long userId);
}

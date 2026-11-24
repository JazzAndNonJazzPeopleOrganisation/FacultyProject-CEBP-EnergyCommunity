package EnergyCommunity.core.service;

import EnergyCommunity.core.model.Admin;
import EnergyCommunity.core.model.User;

import java.util.List;

public interface AdminService {
    List<Admin> findAllAdmins();
    Admin findAdminById(Long id);
    Admin createAdmin(Admin admin);
    Admin authenticateAdmin(String usernameOrEmail, String password);
    void deleteAdmin(Long id);

    List<User> getAllUserAccounts();
    User getUserAccountById(Long userId);
    void deleteUserAccount(Long userId);
}

package EnergyCommunity.core.controller;

import EnergyCommunity.core.model.Admin;
import EnergyCommunity.core.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin API", description = "Endpoints for administrators to manage, view, and delete user accounts")
@RequestMapping("/api/v1/admin")
public interface AdminAPI {

    @Operation(summary = "Get all administrators", description = "Retrieves a list of all registered administrators")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of administrators")
    })
    @GetMapping
    ResponseEntity<List<Admin>> getAllAdmins();

    @Operation(summary = "Get administrator by ID", description = "Retrieves a specific administrator by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Administrator found"),
            @ApiResponse(responseCode = "404", description = "Administrator not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<Admin> getAdminById(
            @Parameter(description = "ID of the administrator", required = true)
            @PathVariable Long id);

    @Operation(summary = "Create a new administrator", description = "Registers a new administrator account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Administrator successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or duplicate username/email")
    })
    @PostMapping
    ResponseEntity<Admin> createAdmin(@Valid @RequestBody Admin admin);

    @Operation(summary = "Authenticate administrator", description = "Authenticates an administrator using username/email and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Missing credentials"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    ResponseEntity<Admin> loginAdmin(
            @Parameter(description = "Username or email of the administrator", required = true)
            @RequestParam String usernameOrEmail,
            @Parameter(description = "Password of the administrator", required = true)
            @RequestParam String password);

    @Operation(summary = "Delete an administrator", description = "Removes an administrator account from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Administrator successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Administrator not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> deleteAdmin(
            @Parameter(description = "ID of the administrator to delete", required = true)
            @PathVariable Long id);

    // --- User Account Management by Admin ---

    @Operation(summary = "View all user accounts", description = "Allows an administrator to view all registered user accounts in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all user accounts")
    })
    @GetMapping("/users")
    ResponseEntity<List<User>> getAllUserAccounts();

    @Operation(summary = "View a specific user account", description = "Allows an administrator to inspect a specific user account and its associated houses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User account found"),
            @ApiResponse(responseCode = "404", description = "User account not found")
    })
    @GetMapping("/users/{userId}")
    ResponseEntity<User> getUserAccountById(
            @Parameter(description = "ID of the user account to view", required = true)
            @PathVariable Long userId);

    @Operation(summary = "Delete a user account", description = "Allows an administrator to delete a user account and all its associated houses/hardware")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User account successfully deleted"),
            @ApiResponse(responseCode = "404", description = "User account not found")
    })
    @DeleteMapping("/users/{userId}")
    ResponseEntity<Void> deleteUserAccount(
            @Parameter(description = "ID of the user account to delete", required = true)
            @PathVariable Long userId);
}

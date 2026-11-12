package EnergyCommunity.core.database.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Schema(description = "Entity representing a registered user in the Energy Community")
public class UserDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the user", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "User name is required")
    @Column(nullable = false)
    @Schema(description = "Full name of the user", example = "Ion Popescu")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true)
    @Schema(description = "Email address of the user", example = "ion.popescu@energy.ro")
    private String email;

    @Column
    @Schema(description = "Phone number of the user", example = "+40722123456")
    private String phone;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("user")
    @Schema(description = "List of houses owned by the user")
    private List<HouseDTO> houses = new ArrayList<>();

    public UserDTO() {
    }

    public UserDTO(Long id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public void addHouse(HouseDTO house) {
        houses.add(house);
        house.setUser(this);
    }

    public void removeHouse(HouseDTO house) {
        houses.remove(house);
        house.setUser(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<HouseDTO> getHouses() {
        return houses;
    }

    public void setHouses(List<HouseDTO> houses) {
        this.houses = houses;
        if (this.houses != null) {
            for (HouseDTO house : this.houses) {
                house.setUser(this);
            }
        }
    }
}

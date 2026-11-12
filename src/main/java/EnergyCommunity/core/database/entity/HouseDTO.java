package EnergyCommunity.core.database.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "houses")
@Schema(description = "Entity representing a smart house and its hardware configuration")
public class HouseDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the house", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "House name or label is required")
    @Column(nullable = false)
    @Schema(description = "Name or label of the house", example = "Casa Timisoara Nord")
    private String name;

    @NotBlank(message = "Address is required")
    @Column(nullable = false)
    @Schema(description = "Physical address of the house", example = "Str. Vasile Parvan nr. 2, Timisoara")
    private String address;

    @Column
    @Schema(description = "Geographic latitude of the house", example = "45.7489")
    private Double latitude;

    @Column
    @Schema(description = "Geographic longitude of the house", example = "21.2087")
    private Double longitude;

    @PositiveOrZero(message = "Current consumption must be zero or positive")
    @Column(nullable = false)
    @Schema(description = "Current baseline power consumption in kW", example = "1.8")
    private Double currentConsumptionKw = 0.0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties("houses")
    @Schema(description = "Owner of the house")
    private UserDTO user;

    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("house")
    @Schema(description = "Batteries installed in the house")
    private List<BatteryDTO> batteries = new ArrayList<>();

    @OneToMany(mappedBy = "house", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("house")
    @Schema(description = "Solar panels installed in the house")
    private List<SolarPanelDTO> solarPanels = new ArrayList<>();

    public HouseDTO() {
    }

    public HouseDTO(Long id, String name, String address, Double latitude, Double longitude, Double currentConsumptionKw) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.currentConsumptionKw = currentConsumptionKw != null ? currentConsumptionKw : 0.0;
    }

    public void addBattery(BatteryDTO battery) {
        batteries.add(battery);
        battery.setHouse(this);
    }

    public void removeBattery(BatteryDTO battery) {
        batteries.remove(battery);
        battery.setHouse(null);
    }

    public void addSolarPanel(SolarPanelDTO solarPanel) {
        solarPanels.add(solarPanel);
        solarPanel.setHouse(this);
    }

    public void removeSolarPanel(SolarPanelDTO solarPanel) {
        solarPanels.remove(solarPanel);
        solarPanel.setHouse(null);
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getCurrentConsumptionKw() {
        return currentConsumptionKw;
    }

    public void setCurrentConsumptionKw(Double currentConsumptionKw) {
        this.currentConsumptionKw = currentConsumptionKw;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    public List<BatteryDTO> getBatteries() {
        return batteries;
    }

    public void setBatteries(List<BatteryDTO> batteries) {
        this.batteries = batteries;
        if (this.batteries != null) {
            for (BatteryDTO battery : this.batteries) {
                battery.setHouse(this);
            }
        }
    }

    public List<SolarPanelDTO> getSolarPanels() {
        return solarPanels;
    }

    public void setSolarPanels(List<SolarPanelDTO> solarPanels) {
        this.solarPanels = solarPanels;
        if (this.solarPanels != null) {
            for (SolarPanelDTO panel : this.solarPanels) {
                panel.setHouse(this);
            }
        }
    }
}

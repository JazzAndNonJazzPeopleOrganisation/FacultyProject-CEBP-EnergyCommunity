package EnergyCommunity.core.database.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "battery")
@Schema(description = "Entity representing a battery storage unit installed in a house")
public class BatteryDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the battery", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Model or name of the battery unit", example = "Tesla Powerwall 3")
    private String model = "Standard Battery";

    @NotNull(message = "Battery capacity in kWh is required")
    @Positive(message = "Battery capacity must be positive")
    @Column(nullable = false)
    @Schema(description = "Total energy storage capacity in kWh", example = "13.5")
    private Double capacityKwh;

    @Min(value = 0, message = "Charge percentage cannot be less than 0")
    @Max(value = 100, message = "Charge percentage cannot exceed 100")
    @Column(nullable = false)
    @Schema(description = "Current state of charge in percentage (0 - 100)", example = "75.0")
    private Double currentChargePercentage = 50.0;

    @Positive(message = "Max charge/discharge rate must be positive")
    @Column(nullable = false)
    @Schema(description = "Maximum charge/discharge power rate in kW", example = "5.0")
    private Double maxPowerKw = 5.0;

    @Column(nullable = false)
    @Schema(description = "Operating status of the battery (IDLE, CHARGING, DISCHARGING, HOLD_FOR_EV)", example = "IDLE")
    private String status = "IDLE";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "house_id")
    @JsonIgnoreProperties({"batteries", "solarPanels", "user"})
    @Schema(description = "The house where this battery is installed")
    private HouseDTO house;

    public BatteryDTO() {
    }

    public BatteryDTO(Long id, String model, Double capacityKwh, Double currentChargePercentage, Double maxPowerKw, String status) {
        this.id = id;
        this.model = model != null ? model : "Standard Battery";
        this.capacityKwh = capacityKwh;
        this.currentChargePercentage = currentChargePercentage != null ? currentChargePercentage : 50.0;
        this.maxPowerKw = maxPowerKw != null ? maxPowerKw : 5.0;
        this.status = status != null ? status : "IDLE";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getCapacityKwh() {
        return capacityKwh;
    }

    public void setCapacityKwh(Double capacityKwh) {
        this.capacityKwh = capacityKwh;
    }

    public Double getCurrentChargePercentage() {
        return currentChargePercentage;
    }

    public void setCurrentChargePercentage(Double currentChargePercentage) {
        this.currentChargePercentage = currentChargePercentage;
    }

    public Double getMaxPowerKw() {
        return maxPowerKw;
    }

    public void setMaxPowerKw(Double maxPowerKw) {
        this.maxPowerKw = maxPowerKw;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public HouseDTO getHouse() {
        return house;
    }

    public void setHouse(HouseDTO house) {
        this.house = house;
    }
}

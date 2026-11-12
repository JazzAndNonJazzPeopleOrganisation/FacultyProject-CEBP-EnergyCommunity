package EnergyCommunity.core.database.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "solar_panels")
@Schema(description = "Entity representing a solar panel array installed in a house")
public class SolarPanelDTO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Unique identifier of the solar panel", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Column(nullable = false)
    @Schema(description = "Model or description of the solar panel", example = "SunPower Maxeon 6")
    private String model = "Standard Solar Panel";

    @NotNull(message = "Max power capacity in kW is required")
    @Positive(message = "Max power capacity must be positive")
    @Column(nullable = false)
    @Schema(description = "Peak power generation capacity in kW (kWp)", example = "6.5")
    private Double maxCapacityKw;

    @Min(value = 0, message = "Efficiency cannot be less than 0")
    @Max(value = 100, message = "Efficiency cannot exceed 100")
    @Column(nullable = false)
    @Schema(description = "Panel conversion efficiency percentage", example = "22.8")
    private Double efficiency = 20.0;

    @Column(nullable = false)
    @Schema(description = "Panel orientation (e.g., SOUTH, SOUTH_WEST, EAST)", example = "SOUTH")
    private String orientation = "SOUTH";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "house_id")
    @JsonIgnoreProperties({"batteries", "solarPanels", "user"})
    @Schema(description = "The house where this solar panel is installed")
    private HouseDTO house;

    public SolarPanelDTO() {
    }

    public SolarPanelDTO(Long id, String model, Double maxCapacityKw, Double efficiency, String orientation) {
        this.id = id;
        this.model = model != null ? model : "Standard Solar Panel";
        this.maxCapacityKw = maxCapacityKw;
        this.efficiency = efficiency != null ? efficiency : 20.0;
        this.orientation = orientation != null ? orientation : "SOUTH";
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

    public Double getMaxCapacityKw() {
        return maxCapacityKw;
    }

    public void setMaxCapacityKw(Double maxCapacityKw) {
        this.maxCapacityKw = maxCapacityKw;
    }

    public Double getEfficiency() {
        return efficiency;
    }

    public void setEfficiency(Double efficiency) {
        this.efficiency = efficiency;
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    public HouseDTO getHouse() {
        return house;
    }

    public void setHouse(HouseDTO house) {
        this.house = house;
    }
}

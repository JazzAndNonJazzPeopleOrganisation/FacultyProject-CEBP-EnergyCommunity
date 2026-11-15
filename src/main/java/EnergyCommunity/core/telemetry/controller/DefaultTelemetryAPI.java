package EnergyCommunity.core.telemetry.controller;

import EnergyCommunity.core.telemetry.model.AggregatedMetricsResponse;
import EnergyCommunity.core.telemetry.model.ForecastResponse;
import EnergyCommunity.core.telemetry.model.HouseStatusResponse;
import EnergyCommunity.core.telemetry.model.SimulationStatusResponse;
import EnergyCommunity.core.telemetry.service.ForecastService;
import EnergyCommunity.core.telemetry.service.HouseStatusService;
import EnergyCommunity.core.telemetry.service.TelemetryAggregationService;
import EnergyCommunity.core.telemetry.simulation.TelemetrySimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/telemetry")
public class DefaultTelemetryAPI implements TelemetryAPI {

    private final HouseStatusService houseStatusService;
    private final ForecastService forecastService;
    private final TelemetryAggregationService aggregationService;
    private final TelemetrySimulationService simulationService;

    @Autowired
    public DefaultTelemetryAPI(
            @Autowired(required = false) HouseStatusService houseStatusService,
            @Autowired(required = false) ForecastService forecastService,
            @Autowired(required = false) TelemetryAggregationService aggregationService,
            @Autowired(required = false) TelemetrySimulationService simulationService) {
        this.houseStatusService = houseStatusService;
        this.forecastService = forecastService;
        this.aggregationService = aggregationService;
        this.simulationService = simulationService;
    }

    @Override
    public ResponseEntity<HouseStatusResponse> getHouseStatus(Long houseId) {
        if (houseStatusService != null) {
            return houseStatusService.getHouseStatus(houseId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<HouseStatusResponse> getHouseStatusByUserId(Long userId) {
        if (houseStatusService != null) {
            return houseStatusService.getHouseStatusByUserId(userId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<List<HouseStatusResponse>> getAllHouseStatuses() {
        List<HouseStatusResponse> list = houseStatusService != null ? houseStatusService.getAllHouseStatuses() : List.of();
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ForecastResponse> getForecast(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return ResponseEntity.badRequest().build();
        }
        ForecastResponse forecast = forecastService != null ? forecastService.get24HourForecast(latitude, longitude) : null;
        return forecast != null ? ResponseEntity.ok(forecast) : ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<AggregatedMetricsResponse> getHouseMetrics(Long houseId) {
        if (aggregationService != null) {
            AggregatedMetricsResponse metrics = aggregationService.getDailyMetrics(houseId);
            return metrics != null ? ResponseEntity.ok(metrics) : ResponseEntity.notFound().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<Void> updateEvProximity(Long houseId, boolean approaching) {
        if (houseStatusService != null) {
            houseStatusService.setEvProximityStatus(houseId, approaching);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<SimulationStatusResponse> getSimulationStatus() {
        return simulationService != null ? ResponseEntity.ok(simulationService.getStatus()) : ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<SimulationStatusResponse> startSimulation(int intervalSeconds) {
        if (simulationService != null) {
            simulationService.startSimulation(intervalSeconds);
            return ResponseEntity.ok(simulationService.getStatus());
        }
        return ResponseEntity.notFound().build();
    }

    @Override
    public ResponseEntity<SimulationStatusResponse> stopSimulation() {
        if (simulationService != null) {
            simulationService.stopSimulation();
            return ResponseEntity.ok(simulationService.getStatus());
        }
        return ResponseEntity.notFound().build();
    }
}

import { HttpClient } from '../interceptor/HttpClient';
import {
  AggregatedMetricsResponseDto,
  ForecastResponseDto,
  HouseStatusResponseDto,
  SimulationStatusResponseDto,
} from './dto/TelemetryDtos';

/**
 * Contract catre Telemetry & Optimization Service (Membru 2):
 * Mirrors EnergyCommunity.core.telemetry.controller.TelemetryAPI (/api/v1/telemetry)
 */
export interface TelemetryApi {
  getHouseStatus(houseId: number): Promise<HouseStatusResponseDto>;
  getHouseStatusByUserId(userId: number): Promise<HouseStatusResponseDto>;
  getAllHouseStatuses(): Promise<HouseStatusResponseDto[]>;
  getForecast(latitude: number, longitude: number): Promise<ForecastResponseDto>;
  getHouseMetrics(houseId: number): Promise<AggregatedMetricsResponseDto>;
  updateEvProximity(houseId: number, approaching: boolean): Promise<void>;
  getSimulationStatus(): Promise<SimulationStatusResponseDto>;
  startSimulation(intervalSeconds?: number): Promise<SimulationStatusResponseDto>;
  stopSimulation(): Promise<SimulationStatusResponseDto>;
}

export class HttpTelemetryApi implements TelemetryApi {
  constructor(private readonly http: HttpClient) {}

  getHouseStatus(houseId: number): Promise<HouseStatusResponseDto> {
    return this.http.get<HouseStatusResponseDto>(`/api/v1/telemetry/houses/${houseId}/status`);
  }

  getHouseStatusByUserId(userId: number): Promise<HouseStatusResponseDto> {
    return this.http.get<HouseStatusResponseDto>(`/api/v1/telemetry/users/${userId}/status`);
  }

  getAllHouseStatuses(): Promise<HouseStatusResponseDto[]> {
    return this.http.get<HouseStatusResponseDto[]>('/api/v1/telemetry/houses/status');
  }

  getForecast(latitude: number, longitude: number): Promise<ForecastResponseDto> {
    return this.http.get<ForecastResponseDto>('/api/v1/telemetry/forecasts', {
      latitude,
      longitude,
    });
  }

  getHouseMetrics(houseId: number): Promise<AggregatedMetricsResponseDto> {
    return this.http.get<AggregatedMetricsResponseDto>(
      `/api/v1/telemetry/houses/${houseId}/metrics`,
    );
  }

  updateEvProximity(houseId: number, approaching: boolean): Promise<void> {
    return this.http.post<void>(`/api/v1/telemetry/houses/${houseId}/proximity`, undefined, {
      approaching,
    });
  }

  getSimulationStatus(): Promise<SimulationStatusResponseDto> {
    return this.http.get<SimulationStatusResponseDto>('/api/v1/telemetry/simulation/status');
  }

  startSimulation(intervalSeconds: number = 5): Promise<SimulationStatusResponseDto> {
    return this.http.post<SimulationStatusResponseDto>(
      '/api/v1/telemetry/simulation/start',
      undefined,
      { intervalSeconds },
    );
  }

  stopSimulation(): Promise<SimulationStatusResponseDto> {
    return this.http.post<SimulationStatusResponseDto>('/api/v1/telemetry/simulation/stop');
  }
}

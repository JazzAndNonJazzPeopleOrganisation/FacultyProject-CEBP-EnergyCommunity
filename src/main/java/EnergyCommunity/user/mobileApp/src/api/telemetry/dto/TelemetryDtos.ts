/**
 * Mirrors EnergyCommunity.core.telemetry.model.HouseStatusResponse
 */
export interface HouseStatusResponseDto {
  houseId: number;
  userId: number;
  batteryLevelPercent: number;
  batteryCapacityKwh: number;
  currentConsumptionKw: number;
  currentSolarProductionKw: number;
  evApproachingHome: boolean;
  updatedAt: string;
}

/**
 * Mirrors EnergyCommunity.core.telemetry.model.ForecastPoint
 */
export interface ForecastPointDto {
  time: string;
  solarRadiationWm2: number;
  pricePerKwh: number;
}

/**
 * Mirrors EnergyCommunity.core.telemetry.model.ForecastResponse
 */
export interface ForecastResponseDto {
  latitude: number;
  longitude: number;
  regionName?: string;
  generatedAt: string;
  horizonHours: number;
  points: ForecastPointDto[];
}

/**
 * Mirrors EnergyCommunity.core.telemetry.model.AggregatedMetricsResponse
 */
export interface AggregatedMetricsResponseDto {
  houseId: number;
  averageConsumptionKw: number;
  peakConsumptionKw: number;
  totalConsumptionKwh: number;
  totalSolarProductionKwh: number;
  netEnergyBalanceKwh: number;
  periodStart: string;
  periodEnd: string;
}

/**
 * Mirrors EnergyCommunity.core.telemetry.model.SimulationStatusResponse
 */
export interface SimulationStatusResponseDto {
  running: boolean;
  simulatedHouseCount: number;
  tickIntervalSeconds: number;
  lastTickAt?: string | null;
}

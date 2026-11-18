export enum UserRole {
  USER = 'USER',
  ADMIN = 'ADMIN',
}

export enum BatteryStatus {
  IDLE = 'IDLE',
  CHARGING = 'CHARGING',
  DISCHARGING = 'DISCHARGING',
  HOLD_FOR_EV = 'HOLD_FOR_EV',
}

export enum PanelOrientation {
  NORTH = 'NORTH',
  NORTH_EAST = 'NORTH_EAST',
  EAST = 'EAST',
  SOUTH_EAST = 'SOUTH_EAST',
  SOUTH = 'SOUTH',
  SOUTH_WEST = 'SOUTH_WEST',
  WEST = 'WEST',
  NORTH_WEST = 'NORTH_WEST',
}

export enum BatteryActionType {
  SELL_TO_GRID = 'SELL_TO_GRID',
  CHARGE_FROM_SOLAR = 'CHARGE_FROM_SOLAR',
  CHARGE_FROM_GRID = 'CHARGE_FROM_GRID',
  HOLD_FOR_EV = 'HOLD_FOR_EV',
  DISCHARGE_TO_HOME = 'DISCHARGE_TO_HOME',
}

export interface User {
  id: string;
  name: string;
  email: string;
  phone?: string | null;
  role: UserRole;
  houseId?: string | null;
}

export interface GeoPoint {
  latitude: number;
  longitude: number;
}

export interface BatteryUnit {
  id?: number;
  model: string;
  capacityKwh: number;
  currentChargePercentage: number;
  maxPowerKw: number;
  status: BatteryStatus;
}

export interface SolarPanelUnit {
  id?: number;
  model: string;
  maxCapacityKw: number;
  efficiency: number;
  orientation: PanelOrientation;
}

/** Configuratia hardware a casei (Core Energy Service). */
export interface HouseConfig {
  houseId: string;
  ownerId: string;
  name?: string;
  address?: string;
  location: GeoPoint;
  currentConsumptionKw?: number;
  batteryCapacityKwh: number;
  solarPanelCount: number;
  batteries?: BatteryUnit[];
  solarPanels?: SolarPanelUnit[];
}

/** Stare curenta a casei (Telemetry Service). */
export interface HouseStatus {
  houseId: string;
  userId?: string;
  batteryLevelPercent: number;
  batteryCapacityKwh?: number;
  currentConsumptionKw: number;
  currentSolarProductionKw: number;
  evApproachingHome?: boolean;
  updatedAt: string;
}

export interface AggregatedMetrics {
  houseId: string;
  averageConsumptionKw: number;
  peakConsumptionKw: number;
  totalConsumptionKwh: number;
  totalSolarProductionKwh: number;
  netEnergyBalanceKwh: number;
  periodStart: string;
  periodEnd: string;
}

export interface SimulationStatus {
  running: boolean;
  simulatedHouseCount: number;
  tickIntervalSeconds: number;
  lastTickAt?: string | null;
}

export interface ForecastPoint {
  time: string;
  solarRadiationWm2: number;
  pricePerKwh: number;
}

export interface Forecast {
  latitude?: number;
  longitude?: number;
  regionName?: string;
  generatedAt?: string;
  horizonHours?: number;
  points: ForecastPoint[];
}

export enum ProposalStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED',
  EXPIRED = 'EXPIRED',
}

/** Plan propus de agentul AI (ex. "vinde 40% din baterie la 19:00"). */
export interface BatteryProposal {
  id: string;
  userId?: string;
  houseId: string;
  description: string;
  reasoning?: string;
  actionType?: BatteryActionType;
  sellPercent: number;
  executeAt: string;
  estimatedProfit?: number | null;
  status: ProposalStatus;
  createdAt?: string;
}

export enum ProximityEvent {
  APPROACHING_HOME = 'APPROACHING_HOME',
  LEFT_HOME_AREA = 'LEFT_HOME_AREA',
}

export type Result<T> =
  | { ok: true; value: T }
  | { ok: false; error: Error };

export const Result = {
  success<T>(value: T): Result<T> {
    return { ok: true, value };
  },
  failure<T = never>(error: Error | string): Result<T> {
    return {
      ok: false,
      error: typeof error === 'string' ? new Error(error) : error,
    };
  },
};

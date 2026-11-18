import {
  AggregatedMetrics,
  BatteryProposal,
  BatteryUnit,
  Forecast,
  GeoPoint,
  HouseConfig,
  HouseStatus,
  ProximityEvent,
  Result,
  SimulationStatus,
  SolarPanelUnit,
  User,
} from '../model/Models';

export type Unsubscribe = () => void;

export interface Observable<T> {
  subscribe(listener: (value: T) => void): Unsubscribe;
}

export interface AuthRepository {
  login(email: string, password: string): Promise<Result<User>>;
  logout(): Promise<void>;
  currentUser(): Observable<User | null>;
}

/** Citire status si configuratie casa + hardware (baterii, panouri, metrici). */
export interface HouseRepository {
  getHouseStatus(houseId: string): Promise<Result<HouseStatus>>;
  getHouseConfig(houseId: string): Promise<Result<HouseConfig>>;
  getHouseMetrics?(houseId: string): Promise<Result<AggregatedMetrics>>;
  getBatteries?(houseId: string): Promise<Result<BatteryUnit[]>>;
  addBattery?(houseId: string, battery: BatteryUnit): Promise<Result<BatteryUnit>>;
  updateBattery?(batteryId: number, battery: BatteryUnit): Promise<Result<BatteryUnit>>;
  deleteBattery?(batteryId: number): Promise<Result<void>>;
  getSolarPanels?(houseId: string): Promise<Result<SolarPanelUnit[]>>;
  addSolarPanel?(houseId: string, panel: SolarPanelUnit): Promise<Result<SolarPanelUnit>>;
  updateSolarPanel?(panelId: number, panel: SolarPanelUnit): Promise<Result<SolarPanelUnit>>;
  deleteSolarPanel?(panelId: number): Promise<Result<void>>;
}

export interface ForecastRepository {
  getForecast(location: GeoPoint): Promise<Result<Forecast>>;
}

export interface ProposalRepository {
  observePendingProposals(houseId: string): Observable<BatteryProposal[]>;
  accept(proposalId: string): Promise<Result<void>>;
  reject(proposalId: string): Promise<Result<void>>;
}

/** Anunta sistemul ca utilizatorul se apropie/pleaca de casa (POST /api/v1/telemetry/houses/{houseId}/proximity). */
export interface ProximityRepository {
  notifyProximity(houseId: string, event: ProximityEvent): Promise<Result<void>>;
}

/** Operatii admin, scrise prin Core Energy Service & Telemetry Service. */
export interface AdminRepository {
  listUsers(): Promise<Result<User[]>>;
  getUserById?(userId: string): Promise<Result<User>>;
  createUser?(user: Omit<User, 'id' | 'role'>): Promise<Result<User>>;
  updateUser?(userId: string, user: Partial<User> & { name: string; email: string }): Promise<Result<User>>;
  deleteUser?(userId: string): Promise<Result<void>>;

  listHouses(): Promise<Result<HouseConfig[]>>;
  getHousesByUserId?(userId: string): Promise<Result<HouseConfig[]>>;
  createHouse?(config: HouseConfig): Promise<Result<HouseConfig>>;
  createHouseForUser?(userId: string, config: HouseConfig): Promise<Result<HouseConfig>>;
  updateHouse(config: HouseConfig): Promise<Result<HouseConfig>>;
  deleteHouse?(houseId: string): Promise<Result<void>>;

  getSimulationStatus?(): Promise<Result<SimulationStatus>>;
  startSimulation?(intervalSeconds?: number): Promise<Result<SimulationStatus>>;
  stopSimulation?(): Promise<Result<SimulationStatus>>;
}

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
import {
  AdminRepository,
  AuthRepository,
  ForecastRepository,
  HouseRepository,
  Observable,
  ProposalRepository,
  ProximityRepository,
} from '../repository/Repositories';

export class GetHouseStatusUseCase {
  constructor(private readonly houses: HouseRepository) {}
  execute(houseId: string): Promise<Result<HouseStatus>> {
    return this.houses.getHouseStatus(houseId);
  }
}

export class GetHouseConfigUseCase {
  constructor(private readonly houses: HouseRepository) {}
  execute(houseId: string): Promise<Result<HouseConfig>> {
    return this.houses.getHouseConfig(houseId);
  }
}

export class GetHouseMetricsUseCase {
  constructor(private readonly houses: HouseRepository) {}
  async execute(houseId: string): Promise<Result<AggregatedMetrics>> {
    if (!this.houses.getHouseMetrics) {
      return Result.failure('getHouseMetrics nu este suportat de acest repository');
    }
    return this.houses.getHouseMetrics(houseId);
  }
}

export class ManageHouseHardwareUseCase {
  constructor(private readonly houses: HouseRepository) {}

  async getBatteries(houseId: string): Promise<Result<BatteryUnit[]>> {
    if (!this.houses.getBatteries) return Result.failure('Neimplementat');
    return this.houses.getBatteries(houseId);
  }

  async addBattery(houseId: string, battery: BatteryUnit): Promise<Result<BatteryUnit>> {
    if (!this.houses.addBattery) return Result.failure('Neimplementat');
    return this.houses.addBattery(houseId, battery);
  }

  async updateBattery(batteryId: number, battery: BatteryUnit): Promise<Result<BatteryUnit>> {
    if (!this.houses.updateBattery) return Result.failure('Neimplementat');
    return this.houses.updateBattery(batteryId, battery);
  }

  async deleteBattery(batteryId: number): Promise<Result<void>> {
    if (!this.houses.deleteBattery) return Result.failure('Neimplementat');
    return this.houses.deleteBattery(batteryId);
  }

  async getSolarPanels(houseId: string): Promise<Result<SolarPanelUnit[]>> {
    if (!this.houses.getSolarPanels) return Result.failure('Neimplementat');
    return this.houses.getSolarPanels(houseId);
  }

  async addSolarPanel(houseId: string, panel: SolarPanelUnit): Promise<Result<SolarPanelUnit>> {
    if (!this.houses.addSolarPanel) return Result.failure('Neimplementat');
    return this.houses.addSolarPanel(houseId, panel);
  }

  async updateSolarPanel(panelId: number, panel: SolarPanelUnit): Promise<Result<SolarPanelUnit>> {
    if (!this.houses.updateSolarPanel) return Result.failure('Neimplementat');
    return this.houses.updateSolarPanel(panelId, panel);
  }

  async deleteSolarPanel(panelId: number): Promise<Result<void>> {
    if (!this.houses.deleteSolarPanel) return Result.failure('Neimplementat');
    return this.houses.deleteSolarPanel(panelId);
  }
}

export class ObservePendingProposalsUseCase {
  constructor(private readonly proposals: ProposalRepository) {}
  execute(houseId: string): Observable<BatteryProposal[]> {
    return this.proposals.observePendingProposals(houseId);
  }
}

export class AcceptProposalUseCase {
  constructor(private readonly proposals: ProposalRepository) {}
  execute(proposalId: string): Promise<Result<void>> {
    return this.proposals.accept(proposalId);
  }
}

export class RejectProposalUseCase {
  constructor(private readonly proposals: ProposalRepository) {}
  execute(proposalId: string): Promise<Result<void>> {
    return this.proposals.reject(proposalId);
  }
}

export class NotifyProximityUseCase {
  constructor(private readonly proximity: ProximityRepository) {}
  execute(houseId: string, event: ProximityEvent): Promise<Result<void>> {
    return this.proximity.notifyProximity(houseId, event);
  }
}

export class LoginUseCase {
  constructor(private readonly auth: AuthRepository) {}
  execute(email: string, password: string): Promise<Result<User>> {
    return this.auth.login(email, password);
  }
}

export class LogoutUseCase {
  constructor(private readonly auth: AuthRepository) {}
  execute(): Promise<void> {
    return this.auth.logout();
  }
}

export class ObserveCurrentUserUseCase {
  constructor(private readonly auth: AuthRepository) {}
  execute(): Observable<User | null> {
    return this.auth.currentUser();
  }
}

export class GetForecastUseCase {
  constructor(private readonly forecasts: ForecastRepository) {}
  execute(location: GeoPoint): Promise<Result<Forecast>> {
    return this.forecasts.getForecast(location);
  }
}

export class ListUsersUseCase {
  constructor(private readonly admin: AdminRepository) {}
  execute(): Promise<Result<User[]>> {
    return this.admin.listUsers();
  }
}

export class ManageUsersUseCase {
  constructor(private readonly admin: AdminRepository) {}

  async createUser(user: Omit<User, 'id' | 'role'>): Promise<Result<User>> {
    if (!this.admin.createUser) return Result.failure('Neimplementat');
    return this.admin.createUser(user);
  }

  async updateUser(
    userId: string,
    user: Partial<User> & { name: string; email: string },
  ): Promise<Result<User>> {
    if (!this.admin.updateUser) return Result.failure('Neimplementat');
    return this.admin.updateUser(userId, user);
  }

  async deleteUser(userId: string): Promise<Result<void>> {
    if (!this.admin.deleteUser) return Result.failure('Neimplementat');
    return this.admin.deleteUser(userId);
  }
}

export class ListHousesUseCase {
  constructor(private readonly admin: AdminRepository) {}
  execute(): Promise<Result<HouseConfig[]>> {
    return this.admin.listHouses();
  }
}

export class UpdateHouseUseCase {
  constructor(private readonly admin: AdminRepository) {}
  execute(config: HouseConfig): Promise<Result<HouseConfig>> {
    return this.admin.updateHouse(config);
  }
}

export class ManageHousesUseCase {
  constructor(private readonly admin: AdminRepository) {}

  async createHouse(config: HouseConfig): Promise<Result<HouseConfig>> {
    if (!this.admin.createHouse) return Result.failure('Neimplementat');
    return this.admin.createHouse(config);
  }

  async createHouseForUser(userId: string, config: HouseConfig): Promise<Result<HouseConfig>> {
    if (!this.admin.createHouseForUser) return Result.failure('Neimplementat');
    return this.admin.createHouseForUser(userId, config);
  }

  async deleteHouse(houseId: string): Promise<Result<void>> {
    if (!this.admin.deleteHouse) return Result.failure('Neimplementat');
    return this.admin.deleteHouse(houseId);
  }
}

export class ManageSimulationUseCase {
  constructor(private readonly admin: AdminRepository) {}

  async getStatus(): Promise<Result<SimulationStatus>> {
    if (!this.admin.getSimulationStatus) return Result.failure('Neimplementat');
    return this.admin.getSimulationStatus();
  }

  async start(intervalSeconds?: number): Promise<Result<SimulationStatus>> {
    if (!this.admin.startSimulation) return Result.failure('Neimplementat');
    return this.admin.startSimulation(intervalSeconds);
  }

  async stop(): Promise<Result<SimulationStatus>> {
    if (!this.admin.stopSimulation) return Result.failure('Neimplementat');
    return this.admin.stopSimulation();
  }
}

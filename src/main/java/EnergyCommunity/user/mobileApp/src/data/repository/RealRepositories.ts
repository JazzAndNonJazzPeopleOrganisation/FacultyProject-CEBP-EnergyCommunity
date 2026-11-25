import { CoreEnergyApi } from '../../api/core/CoreEnergyApi';
import { TelemetryApi } from '../../api/telemetry/TelemetryApi';
import {
  AggregatedMetrics,
  BatteryProposal,
  BatteryUnit,
  Forecast,
  GeoPoint,
  HouseConfig,
  HouseStatus,
  ProposalStatus,
  ProximityEvent,
  Result,
  SimulationStatus,
  SolarPanelUnit,
  User,
} from '../../domain/model/Models';
import {
  AdminRepository,
  AuthRepository,
  ForecastRepository,
  HouseRepository,
  Observable,
  ProposalRepository,
  ProximityRepository,
  Unsubscribe,
} from '../../domain/repository/Repositories';
import { LocalDao } from '../local/LocalDao';
import { CoreMappers, TelemetryMappers } from '../mapper/Mappers';

export class RealAuthRepository implements AuthRepository {
  constructor(
    private readonly coreApi: CoreEnergyApi,
    private readonly dao: LocalDao,
  ) {}

  async login(email: string, password: string): Promise<Result<User>> {
    try {
      const normalized = email.trim().toLowerCase();
      if (!normalized || !password) {
        return Result.failure('Introduceti email si parola');
      }

      const users = await this.coreApi.getAllUsers();
      const matched = users.find((u) => u.email.trim().toLowerCase() === normalized);
      if (!matched || matched.id == null) {
        return Result.failure('Utilizator inexistent in Core Energy Service');
      }

      let domainUser = CoreMappers.toDomainUser(matched);

      // Daca userul nu avea houses incarcate pe obiectul principal, le interogam explicit din /api/v1/houses/user/{userId}
      if (!domainUser.houseId) {
        try {
          const houses = await this.coreApi.getHousesByUserId(matched.id);
          if (houses.length > 0 && houses[0].id != null) {
            const firstConfig = CoreMappers.toDomainHouseConfig(houses[0], domainUser.id);
            this.dao.saveHouseConfig(firstConfig);
            domainUser = { ...domainUser, houseId: firstConfig.houseId };
          }
        } catch {
          // Ignoram daca nu are inca o casa asociata
        }
      } else if (matched.houses && matched.houses.length > 0) {
        const firstConfig = CoreMappers.toDomainHouseConfig(matched.houses[0], domainUser.id);
        this.dao.saveHouseConfig(firstConfig);
      }

      this.dao.setCurrentUser(domainUser);
      return Result.success(domainUser);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async logout(): Promise<void> {
    this.dao.setCurrentUser(null);
  }

  currentUser(): Observable<User | null> {
    return this.dao.observeCurrentUser();
  }
}

export class RealHouseRepository implements HouseRepository {
  constructor(
    private readonly coreApi: CoreEnergyApi,
    private readonly telemetryApi: TelemetryApi,
    private readonly dao: LocalDao,
  ) {}

  async getHouseStatus(houseId: string): Promise<Result<HouseStatus>> {
    const numericId = Number(houseId);
    if (!Number.isFinite(numericId)) {
      return Result.failure(`ID casa invalid: ${houseId}`);
    }

    // 1. Incercam mai intai TelemetryAPI (/api/v1/telemetry/houses/{houseId}/status)
    try {
      const dto = await this.telemetryApi.getHouseStatus(numericId);
      const status = TelemetryMappers.toDomainHouseStatus(dto);
      this.dao.saveHouseStatus(status);
      return Result.success(status);
    } catch {
      // 2. Fallback pe Core HouseAPI (/api/v1/houses/{id}) daca simulatorul de telemetrie nu este pornit
      try {
        const houseDto = await this.coreApi.getHouseById(numericId);
        const status = CoreMappers.toFallbackHouseStatus(houseDto);
        this.dao.saveHouseStatus(status);
        return Result.success(status);
      } catch (err) {
        return Result.failure(err instanceof Error ? err : String(err));
      }
    }
  }

  async getHouseConfig(houseId: string): Promise<Result<HouseConfig>> {
    const numericId = Number(houseId);
    if (!Number.isFinite(numericId)) {
      return Result.failure(`ID casa invalid: ${houseId}`);
    }

    try {
      const dto = await this.coreApi.getHouseById(numericId);
      const config = CoreMappers.toDomainHouseConfig(dto);
      this.dao.saveHouseConfig(config);
      return Result.success(config);
    } catch (err) {
      const cached = this.dao.getHouseConfig(houseId);
      if (cached) {
        return Result.success(cached);
      }
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async getHouseMetrics(houseId: string): Promise<Result<AggregatedMetrics>> {
    try {
      const dto = await this.telemetryApi.getHouseMetrics(Number(houseId));
      return Result.success(TelemetryMappers.toDomainAggregatedMetrics(dto));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async getBatteries(houseId: string): Promise<Result<BatteryUnit[]>> {
    try {
      const dtos = await this.coreApi.getBatteriesByHouseId(Number(houseId));
      return Result.success(dtos.map(CoreMappers.toDomainBattery));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async addBattery(houseId: string, battery: BatteryUnit): Promise<Result<BatteryUnit>> {
    try {
      const created = await this.coreApi.addBatteryToHouse(
        Number(houseId),
        CoreMappers.toDtoBattery(battery),
      );
      return Result.success(CoreMappers.toDomainBattery(created));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async updateBattery(batteryId: number, battery: BatteryUnit): Promise<Result<BatteryUnit>> {
    try {
      const updated = await this.coreApi.updateBattery(
        batteryId,
        CoreMappers.toDtoBattery(battery),
      );
      return Result.success(CoreMappers.toDomainBattery(updated));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async deleteBattery(batteryId: number): Promise<Result<void>> {
    try {
      await this.coreApi.deleteBattery(batteryId);
      return Result.success(undefined);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async getSolarPanels(houseId: string): Promise<Result<SolarPanelUnit[]>> {
    try {
      const dtos = await this.coreApi.getSolarPanelsByHouseId(Number(houseId));
      return Result.success(dtos.map(CoreMappers.toDomainSolarPanel));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async addSolarPanel(houseId: string, panel: SolarPanelUnit): Promise<Result<SolarPanelUnit>> {
    try {
      const created = await this.coreApi.addSolarPanelToHouse(
        Number(houseId),
        CoreMappers.toDtoSolarPanel(panel),
      );
      return Result.success(CoreMappers.toDomainSolarPanel(created));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async updateSolarPanel(
    panelId: number,
    panel: SolarPanelUnit,
  ): Promise<Result<SolarPanelUnit>> {
    try {
      const updated = await this.coreApi.updateSolarPanel(
        panelId,
        CoreMappers.toDtoSolarPanel(panel),
      );
      return Result.success(CoreMappers.toDomainSolarPanel(updated));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async deleteSolarPanel(panelId: number): Promise<Result<void>> {
    try {
      await this.coreApi.deleteSolarPanel(panelId);
      return Result.success(undefined);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }
}

export class RealForecastRepository implements ForecastRepository {
  constructor(private readonly telemetryApi: TelemetryApi) {}

  async getForecast(location: GeoPoint): Promise<Result<Forecast>> {
    try {
      const dto = await this.telemetryApi.getForecast(location.latitude, location.longitude);
      return Result.success(TelemetryMappers.toDomainForecast(dto));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }
}

export class RealProposalRepository implements ProposalRepository {
  constructor(
    private readonly coreApi: CoreEnergyApi,
    private readonly dao: LocalDao,
  ) {}

  observePendingProposals(houseId: string): Observable<BatteryProposal[]> {
    return {
      subscribe: (listener: (value: BatteryProposal[]) => void): Unsubscribe => {
        const unsubDao = this.dao.observePendingProposals(houseId).subscribe(listener);

        const refresh = async () => {
          try {
            const dtos = await this.coreApi.getPendingProposalsByHouseId(houseId);
            this.dao.setProposalsForHouse(
              houseId,
              dtos.map(CoreMappers.toDomainProposal),
            );
          } catch {
            // Pastram propunerile din cache-ul local daca endpoint-ul MCP nu e pornit
          }
        };

        void refresh();
        return () => {
          unsubDao();
        };
      },
    };
  }

  async accept(proposalId: string): Promise<Result<void>> {
    try {
      await this.coreApi.acceptProposal(proposalId);
      this.dao.updateProposalStatus(proposalId, ProposalStatus.ACCEPTED);
      return Result.success(undefined);
    } catch (err) {
      if (this.dao.updateProposalStatus(proposalId, ProposalStatus.ACCEPTED)) {
        return Result.success(undefined);
      }
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async reject(proposalId: string): Promise<Result<void>> {
    try {
      await this.coreApi.rejectProposal(proposalId);
      this.dao.updateProposalStatus(proposalId, ProposalStatus.REJECTED);
      return Result.success(undefined);
    } catch (err) {
      if (this.dao.updateProposalStatus(proposalId, ProposalStatus.REJECTED)) {
        return Result.success(undefined);
      }
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }
}

export class RealProximityRepository implements ProximityRepository {
  constructor(private readonly telemetryApi: TelemetryApi) {}

  async notifyProximity(houseId: string, event: ProximityEvent): Promise<Result<void>> {
    try {
      const approaching = event === ProximityEvent.APPROACHING_HOME;
      await this.telemetryApi.updateEvProximity(Number(houseId), approaching);
      return Result.success(undefined);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }
}

export class RealAdminRepository implements AdminRepository {
  constructor(
    private readonly coreApi: CoreEnergyApi,
    private readonly telemetryApi: TelemetryApi,
    private readonly dao: LocalDao,
  ) {}

  async listUsers(): Promise<Result<User[]>> {
    try {
      const dtos = await this.coreApi.getAllUsers();
      return Result.success(dtos.map(CoreMappers.toDomainUser));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async getUserById(userId: string): Promise<Result<User>> {
    try {
      const dto = await this.coreApi.getUserById(Number(userId));
      return Result.success(CoreMappers.toDomainUser(dto));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async createUser(user: Omit<User, 'id' | 'role'>): Promise<Result<User>> {
    try {
      const dto = await this.coreApi.createUser(CoreMappers.toDtoUser(user));
      return Result.success(CoreMappers.toDomainUser(dto));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async updateUser(
    userId: string,
    user: Partial<User> & { name: string; email: string },
  ): Promise<Result<User>> {
    try {
      const dto = await this.coreApi.updateUser(Number(userId), CoreMappers.toDtoUser(user));
      return Result.success(CoreMappers.toDomainUser(dto));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async deleteUser(userId: string): Promise<Result<void>> {
    try {
      await this.coreApi.deleteUser(Number(userId));
      return Result.success(undefined);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async listHouses(): Promise<Result<HouseConfig[]>> {
    try {
      const dtos = await this.coreApi.getAllHouses();
      const houses = dtos.map((d) => CoreMappers.toDomainHouseConfig(d));
      houses.forEach((h) => this.dao.saveHouseConfig(h));
      return Result.success(houses);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async getHousesByUserId(userId: string): Promise<Result<HouseConfig[]>> {
    try {
      const dtos = await this.coreApi.getHousesByUserId(Number(userId));
      const houses = dtos.map((d) => CoreMappers.toDomainHouseConfig(d, userId));
      houses.forEach((h) => this.dao.saveHouseConfig(h));
      return Result.success(houses);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async createHouse(config: HouseConfig): Promise<Result<HouseConfig>> {
    try {
      const dto = await this.coreApi.createHouse(CoreMappers.toDtoHouse(config));
      const created = CoreMappers.toDomainHouseConfig(dto, config.ownerId);
      this.dao.saveHouseConfig(created);
      return Result.success(created);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async createHouseForUser(userId: string, config: HouseConfig): Promise<Result<HouseConfig>> {
    try {
      const dto = await this.coreApi.createHouseForUser(
        Number(userId),
        CoreMappers.toDtoHouse(config),
      );
      const created = CoreMappers.toDomainHouseConfig(dto, userId);
      this.dao.saveHouseConfig(created);
      return Result.success(created);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async updateHouse(config: HouseConfig): Promise<Result<HouseConfig>> {
    try {
      const dto = await this.coreApi.updateHouse(
        Number(config.houseId),
        CoreMappers.toDtoHouse(config),
      );
      const updated = CoreMappers.toDomainHouseConfig(dto, config.ownerId);
      this.dao.saveHouseConfig(updated);
      return Result.success(updated);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async deleteHouse(houseId: string): Promise<Result<void>> {
    try {
      await this.coreApi.deleteHouse(Number(houseId));
      this.dao.removeHouseConfig(houseId);
      return Result.success(undefined);
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async getSimulationStatus(): Promise<Result<SimulationStatus>> {
    try {
      const dto = await this.telemetryApi.getSimulationStatus();
      return Result.success(TelemetryMappers.toDomainSimulationStatus(dto));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async startSimulation(intervalSeconds?: number): Promise<Result<SimulationStatus>> {
    try {
      const dto = await this.telemetryApi.startSimulation(intervalSeconds);
      return Result.success(TelemetryMappers.toDomainSimulationStatus(dto));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }

  async stopSimulation(): Promise<Result<SimulationStatus>> {
    try {
      const dto = await this.telemetryApi.stopSimulation();
      return Result.success(TelemetryMappers.toDomainSimulationStatus(dto));
    } catch (err) {
      return Result.failure(err instanceof Error ? err : String(err));
    }
  }
}

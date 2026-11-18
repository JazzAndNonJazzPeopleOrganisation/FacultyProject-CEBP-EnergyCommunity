import {
  BatteryDto,
  BatteryProposalDto,
  HouseDto,
  SolarPanelDto,
  UserDto,
} from '../../api/core/dto/CoreDtos';
import {
  AggregatedMetricsResponseDto,
  ForecastPointDto,
  ForecastResponseDto,
  HouseStatusResponseDto,
  SimulationStatusResponseDto,
} from '../../api/telemetry/dto/TelemetryDtos';
import {
  AggregatedMetrics,
  BatteryProposal,
  BatteryStatus,
  BatteryUnit,
  Forecast,
  ForecastPoint,
  HouseConfig,
  HouseStatus,
  PanelOrientation,
  SimulationStatus,
  SolarPanelUnit,
  User,
  UserRole,
} from '../../domain/model/Models';

export const CoreMappers = {
  toDomainBattery(dto: BatteryDto): BatteryUnit {
    return {
      id: dto.id,
      model: dto.model,
      capacityKwh: dto.capacityKwh,
      currentChargePercentage: dto.currentChargePercentage,
      maxPowerKw: dto.maxPowerKw,
      status: dto.status ?? BatteryStatus.IDLE,
    };
  },

  toDtoBattery(domain: BatteryUnit): BatteryDto {
    return {
      id: domain.id,
      model: domain.model,
      capacityKwh: domain.capacityKwh,
      currentChargePercentage: domain.currentChargePercentage,
      maxPowerKw: domain.maxPowerKw,
      status: domain.status,
    };
  },

  toDomainSolarPanel(dto: SolarPanelDto): SolarPanelUnit {
    return {
      id: dto.id,
      model: dto.model,
      maxCapacityKw: dto.maxCapacityKw,
      efficiency: dto.efficiency,
      orientation: dto.orientation ?? PanelOrientation.SOUTH,
    };
  },

  toDtoSolarPanel(domain: SolarPanelUnit): SolarPanelDto {
    return {
      id: domain.id,
      model: domain.model,
      maxCapacityKw: domain.maxCapacityKw,
      efficiency: domain.efficiency,
      orientation: domain.orientation,
    };
  },

  toDomainHouseConfig(dto: HouseDto, fallbackOwnerId?: string): HouseConfig {
    const batteries = (dto.batteries ?? []).map(CoreMappers.toDomainBattery);
    const solarPanels = (dto.solarPanels ?? []).map(CoreMappers.toDomainSolarPanel);
    const totalBatteryKwh = batteries.reduce((sum, b) => sum + (b.capacityKwh ?? 0), 0);

    return {
      houseId: String(dto.id ?? ''),
      ownerId: dto.user?.id != null ? String(dto.user.id) : (fallbackOwnerId ?? ''),
      name: dto.name,
      address: dto.address,
      location: {
        latitude: dto.latitude ?? 45.7489,
        longitude: dto.longitude ?? 21.2087,
      },
      currentConsumptionKw: dto.currentConsumptionKw ?? 0,
      batteryCapacityKwh: totalBatteryKwh,
      solarPanelCount: solarPanels.length,
      batteries,
      solarPanels,
    };
  },

  toDtoHouse(config: HouseConfig): HouseDto {
    const numericId = Number(config.houseId);
    const numericOwnerId = Number(config.ownerId);

    return {
      ...(Number.isFinite(numericId) && numericId > 0 ? { id: numericId } : {}),
      name: config.name || `House ${config.houseId}`,
      address: config.address || 'Str. Vasile Parvan nr. 2, Timisoara',
      latitude: config.location.latitude,
      longitude: config.location.longitude,
      currentConsumptionKw: config.currentConsumptionKw ?? 0,
      ...(Number.isFinite(numericOwnerId) && numericOwnerId > 0
        ? { user: { id: numericOwnerId } }
        : {}),
      ...(config.batteries
        ? { batteries: config.batteries.map(CoreMappers.toDtoBattery) }
        : {}),
      ...(config.solarPanels
        ? { solarPanels: config.solarPanels.map(CoreMappers.toDtoSolarPanel) }
        : {}),
    };
  },

  toDomainUser(dto: UserDto): User {
    const firstHouseId =
      dto.houses && dto.houses.length > 0 && dto.houses[0].id != null
        ? String(dto.houses[0].id)
        : null;
    const isAdmin = dto.email.toLowerCase().includes('admin');

    return {
      id: String(dto.id ?? ''),
      name: dto.name,
      email: dto.email,
      phone: dto.phone ?? null,
      role: isAdmin ? UserRole.ADMIN : UserRole.USER,
      houseId: firstHouseId,
    };
  },

  toDtoUser(user: Partial<User> & { name: string; email: string }): UserDto {
    const numericId = user.id ? Number(user.id) : NaN;
    return {
      ...(Number.isFinite(numericId) && numericId > 0 ? { id: numericId } : {}),
      name: user.name,
      email: user.email,
      phone: user.phone ?? null,
    };
  },

  toDomainProposal(dto: BatteryProposalDto): BatteryProposal {
    return {
      id: dto.id,
      userId: dto.userId,
      houseId: String(dto.houseId),
      description: dto.description,
      reasoning: dto.reasoning,
      actionType: dto.actionType,
      sellPercent: dto.sellPercent,
      executeAt: dto.executeAt,
      estimatedProfit: dto.estimatedProfit ?? null,
      status: dto.status,
      createdAt: dto.createdAt,
    };
  },

  /**
   * Derives a fallback HouseStatus snapshot directly from Core HouseDto
   * when Telemetry simulation has not emitted a snapshot yet.
   */
  toFallbackHouseStatus(dto: HouseDto): HouseStatus {
    const batteries = dto.batteries ?? [];
    const totalCapacity = batteries.reduce((acc, b) => acc + (b.capacityKwh ?? 0), 0);
    const avgCharge =
      batteries.length > 0
        ? Math.round(
            batteries.reduce((acc, b) => acc + (b.currentChargePercentage ?? 0), 0) /
              batteries.length,
          )
        : 0;
    const solarPanels = dto.solarPanels ?? [];
    const estimatedSolarKw = Number(
      solarPanels
        .reduce((acc, p) => acc + (p.maxCapacityKw ?? 0) * ((p.efficiency ?? 20) / 100), 0)
        .toFixed(2),
    );

    return {
      houseId: String(dto.id ?? ''),
      userId: dto.user?.id != null ? String(dto.user.id) : undefined,
      batteryLevelPercent: avgCharge,
      batteryCapacityKwh: totalCapacity,
      currentConsumptionKw: dto.currentConsumptionKw ?? 0,
      currentSolarProductionKw: estimatedSolarKw,
      evApproachingHome: false,
      updatedAt: new Date().toISOString(),
    };
  },
};

export const TelemetryMappers = {
  toDomainHouseStatus(dto: HouseStatusResponseDto): HouseStatus {
    return {
      houseId: String(dto.houseId),
      userId: String(dto.userId),
      batteryLevelPercent: dto.batteryLevelPercent,
      batteryCapacityKwh: dto.batteryCapacityKwh,
      currentConsumptionKw: dto.currentConsumptionKw,
      currentSolarProductionKw: dto.currentSolarProductionKw,
      evApproachingHome: dto.evApproachingHome,
      updatedAt: dto.updatedAt,
    };
  },

  toDomainForecastPoint(dto: ForecastPointDto): ForecastPoint {
    return {
      time: dto.time,
      solarRadiationWm2: dto.solarRadiationWm2,
      pricePerKwh: dto.pricePerKwh,
    };
  },

  toDomainForecast(dto: ForecastResponseDto): Forecast {
    return {
      latitude: dto.latitude,
      longitude: dto.longitude,
      regionName: dto.regionName,
      generatedAt: dto.generatedAt,
      horizonHours: dto.horizonHours,
      points: (dto.points ?? []).map(TelemetryMappers.toDomainForecastPoint),
    };
  },

  toDomainAggregatedMetrics(dto: AggregatedMetricsResponseDto): AggregatedMetrics {
    return {
      houseId: String(dto.houseId),
      averageConsumptionKw: dto.averageConsumptionKw,
      peakConsumptionKw: dto.peakConsumptionKw,
      totalConsumptionKwh: dto.totalConsumptionKwh,
      totalSolarProductionKwh: dto.totalSolarProductionKwh,
      netEnergyBalanceKwh: dto.netEnergyBalanceKwh,
      periodStart: dto.periodStart,
      periodEnd: dto.periodEnd,
    };
  },

  toDomainSimulationStatus(dto: SimulationStatusResponseDto): SimulationStatus {
    return {
      running: dto.running,
      simulatedHouseCount: dto.simulatedHouseCount,
      tickIntervalSeconds: dto.tickIntervalSeconds,
      lastTickAt: dto.lastTickAt ?? null,
    };
  },
};

import { HttpClient } from '../interceptor/HttpClient';
import {
  BatteryDto,
  BatteryProposalDto,
  HouseDto,
  SolarPanelDto,
  UserDto,
} from './dto/CoreDtos';

/**
 * Contract catre Core Energy Service (Membru 1):
 * - UserAPI (/api/v1/users)
 * - HouseAPI (/api/v1/houses, batteries, solar-panels)
 * - Proposals (/api/v1/proposals)
 */
export interface CoreEnergyApi {
  // UserAPI (/api/v1/users)
  getAllUsers(): Promise<UserDto[]>;
  getUserById(id: number): Promise<UserDto>;
  createUser(user: UserDto): Promise<UserDto>;
  updateUser(id: number, user: UserDto): Promise<UserDto>;
  deleteUser(id: number): Promise<void>;

  // HouseAPI (/api/v1/houses)
  getAllHouses(): Promise<HouseDto[]>;
  getHouseById(id: number): Promise<HouseDto>;
  getHousesByUserId(userId: number): Promise<HouseDto[]>;
  createHouse(house: HouseDto): Promise<HouseDto>;
  createHouseForUser(userId: number, house: HouseDto): Promise<HouseDto>;
  updateHouse(id: number, house: HouseDto): Promise<HouseDto>;
  deleteHouse(id: number): Promise<void>;

  // Battery Endpoints (/api/v1/houses/{houseId}/batteries & /api/v1/houses/batteries/{batteryId})
  getBatteriesByHouseId(houseId: number): Promise<BatteryDto[]>;
  addBatteryToHouse(houseId: number, battery: BatteryDto): Promise<BatteryDto>;
  updateBattery(batteryId: number, battery: BatteryDto): Promise<BatteryDto>;
  deleteBattery(batteryId: number): Promise<void>;

  // Solar Panel Endpoints (/api/v1/houses/{houseId}/solar-panels & /api/v1/houses/solar-panels/{panelId})
  getSolarPanelsByHouseId(houseId: number): Promise<SolarPanelDto[]>;
  addSolarPanelToHouse(houseId: number, solarPanel: SolarPanelDto): Promise<SolarPanelDto>;
  updateSolarPanel(panelId: number, solarPanel: SolarPanelDto): Promise<SolarPanelDto>;
  deleteSolarPanel(panelId: number): Promise<void>;

  // Proposals Endpoints
  getPendingProposalsByHouseId(houseId: string): Promise<BatteryProposalDto[]>;
  acceptProposal(proposalId: string): Promise<void>;
  rejectProposal(proposalId: string): Promise<void>;
}

export class HttpCoreEnergyApi implements CoreEnergyApi {
  constructor(private readonly http: HttpClient) {}

  // --- UserAPI ---
  getAllUsers(): Promise<UserDto[]> {
    return this.http.get<UserDto[]>('/api/v1/users');
  }

  getUserById(id: number): Promise<UserDto> {
    return this.http.get<UserDto>(`/api/v1/users/${id}`);
  }

  createUser(user: UserDto): Promise<UserDto> {
    return this.http.post<UserDto>('/api/v1/users', user);
  }

  updateUser(id: number, user: UserDto): Promise<UserDto> {
    return this.http.put<UserDto>(`/api/v1/users/${id}`, user);
  }

  deleteUser(id: number): Promise<void> {
    return this.http.delete(`/api/v1/users/${id}`);
  }

  // --- HouseAPI ---
  getAllHouses(): Promise<HouseDto[]> {
    return this.http.get<HouseDto[]>('/api/v1/houses');
  }

  getHouseById(id: number): Promise<HouseDto> {
    return this.http.get<HouseDto>(`/api/v1/houses/${id}`);
  }

  getHousesByUserId(userId: number): Promise<HouseDto[]> {
    return this.http.get<HouseDto[]>(`/api/v1/houses/user/${userId}`);
  }

  createHouse(house: HouseDto): Promise<HouseDto> {
    return this.http.post<HouseDto>('/api/v1/houses', house);
  }

  createHouseForUser(userId: number, house: HouseDto): Promise<HouseDto> {
    return this.http.post<HouseDto>(`/api/v1/houses/user/${userId}`, house);
  }

  updateHouse(id: number, house: HouseDto): Promise<HouseDto> {
    return this.http.put<HouseDto>(`/api/v1/houses/${id}`, house);
  }

  deleteHouse(id: number): Promise<void> {
    return this.http.delete(`/api/v1/houses/${id}`);
  }

  // --- Batteries ---
  getBatteriesByHouseId(houseId: number): Promise<BatteryDto[]> {
    return this.http.get<BatteryDto[]>(`/api/v1/houses/${houseId}/batteries`);
  }

  addBatteryToHouse(houseId: number, battery: BatteryDto): Promise<BatteryDto> {
    return this.http.post<BatteryDto>(`/api/v1/houses/${houseId}/batteries`, battery);
  }

  updateBattery(batteryId: number, battery: BatteryDto): Promise<BatteryDto> {
    return this.http.put<BatteryDto>(`/api/v1/houses/batteries/${batteryId}`, battery);
  }

  deleteBattery(batteryId: number): Promise<void> {
    return this.http.delete(`/api/v1/houses/batteries/${batteryId}`);
  }

  // --- Solar Panels ---
  getSolarPanelsByHouseId(houseId: number): Promise<SolarPanelDto[]> {
    return this.http.get<SolarPanelDto[]>(`/api/v1/houses/${houseId}/solar-panels`);
  }

  addSolarPanelToHouse(houseId: number, solarPanel: SolarPanelDto): Promise<SolarPanelDto> {
    return this.http.post<SolarPanelDto>(`/api/v1/houses/${houseId}/solar-panels`, solarPanel);
  }

  updateSolarPanel(panelId: number, solarPanel: SolarPanelDto): Promise<SolarPanelDto> {
    return this.http.put<SolarPanelDto>(`/api/v1/houses/solar-panels/${panelId}`, solarPanel);
  }

  deleteSolarPanel(panelId: number): Promise<void> {
    return this.http.delete(`/api/v1/houses/solar-panels/${panelId}`);
  }

  // --- Proposals ---
  getPendingProposalsByHouseId(houseId: string): Promise<BatteryProposalDto[]> {
    return this.http.get<BatteryProposalDto[]>(`/api/v1/proposals/house/${houseId}`, {
      status: 'PENDING',
    });
  }

  acceptProposal(proposalId: string): Promise<void> {
    return this.http.post<void>(`/api/v1/proposals/${proposalId}/accept`);
  }

  rejectProposal(proposalId: string): Promise<void> {
    return this.http.post<void>(`/api/v1/proposals/${proposalId}/reject`);
  }
}

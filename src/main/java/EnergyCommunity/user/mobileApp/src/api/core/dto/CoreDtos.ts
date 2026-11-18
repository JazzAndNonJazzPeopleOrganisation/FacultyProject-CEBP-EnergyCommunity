import {
  BatteryActionType,
  BatteryStatus,
  PanelOrientation,
  ProposalStatus,
} from '../../../domain/model/Models';

/**
 * Mirrors EnergyCommunity.core.model.Battery
 */
export interface BatteryDto {
  id?: number;
  model: string;
  capacityKwh: number;
  currentChargePercentage: number;
  maxPowerKw: number;
  status: BatteryStatus;
}

/**
 * Mirrors EnergyCommunity.core.model.SolarPanel
 */
export interface SolarPanelDto {
  id?: number;
  model: string;
  maxCapacityKw: number;
  efficiency: number;
  orientation: PanelOrientation;
}

/**
 * Mirrors EnergyCommunity.core.model.House
 */
export interface HouseDto {
  id?: number;
  name: string;
  address: string;
  latitude?: number | null;
  longitude?: number | null;
  currentConsumptionKw: number;
  user?: {
    id?: number;
    name?: string;
    email?: string;
    phone?: string | null;
  } | null;
  batteries?: BatteryDto[];
  solarPanels?: SolarPanelDto[];
}

/**
 * Mirrors EnergyCommunity.core.model.User
 */
export interface UserDto {
  id?: number;
  name: string;
  email: string;
  phone?: string | null;
  houses?: HouseDto[];
}

/**
 * Mirrors EnergyCommunity.core.mcpServer.tools.model.BatteryProposal
 */
export interface BatteryProposalDto {
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

/**
 * Mirrors EnergyCommunity.core.mcpServer.tools.model.BatteryActionPlan
 */
export interface BatteryActionPlanDto {
  houseId?: string;
  actionType: BatteryActionType;
  batteryPercentage: number;
  executeAt: string;
  estimatedProfitEur?: number | null;
  description: string;
  reasoning?: string;
}

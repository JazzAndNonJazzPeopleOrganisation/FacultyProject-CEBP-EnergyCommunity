import { AppContainer } from '../../di/AppContainer';
import {
  BatteryProposal,
  Forecast,
  ForecastPoint,
  GeoPoint,
  HouseConfig,
  HouseStatus,
  ProposalStatus,
  ProximityEvent,
  Result,
  User,
  UserRole,
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
import {
  GeofenceManager,
  LocationProvider,
  ProximityDetector,
} from '../../geolocation/Geolocation';

const FAKE_DELAY_MS = 300;
const fakeHome: GeoPoint = { latitude: 44.4268, longitude: 26.1025 };

const fakeUser: User = {
  id: 'u-1',
  name: 'Test User',
  email: 'user@test.com',
  role: UserRole.USER,
  houseId: 'h-1',
};

const fakeAdmin: User = {
  id: 'u-0',
  name: 'Test Admin',
  email: 'admin@test.com',
  role: UserRole.ADMIN,
  houseId: null,
};

const fakeHouse: HouseConfig = {
  houseId: 'h-1',
  ownerId: 'u-1',
  location: fakeHome,
  batteryCapacityKwh: 13.5,
  solarPanelCount: 12,
};

const delay = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

class StateStream<T> implements Observable<T> {
  private listeners = new Set<(value: T) => void>();

  constructor(private currentValue: T) {}

  get value(): T {
    return this.currentValue;
  }

  set value(next: T) {
    this.currentValue = next;
    this.listeners.forEach((listener) => listener(next));
  }

  subscribe(listener: (value: T) => void): Unsubscribe {
    this.listeners.add(listener);
    listener(this.currentValue);
    return () => {
      this.listeners.delete(listener);
    };
  }

  map<R>(transform: (value: T) => R): Observable<R> {
    return {
      subscribe: (listener: (value: R) => void): Unsubscribe =>
        this.subscribe((val) => listener(transform(val))),
    };
  }
}

export class FakeAuthRepository implements AuthRepository {
  private readonly user = new StateStream<User | null>(null);

  async login(email: string, password: string): Promise<Result<User>> {
    await delay(FAKE_DELAY_MS);
    const found = [fakeUser, fakeAdmin].find((u) => u.email === email);
    if (found && password.length > 0) {
      this.user.value = found;
      return Result.success(found);
    }
    return Result.failure('Credentiale invalide');
  }

  async logout(): Promise<void> {
    this.user.value = null;
  }

  currentUser(): Observable<User | null> {
    return this.user;
  }
}

export class FakeHouseRepository implements HouseRepository {
  async getHouseStatus(houseId: string): Promise<Result<HouseStatus>> {
    await delay(FAKE_DELAY_MS);
    return Result.success({
      houseId,
      batteryLevelPercent: 72,
      currentConsumptionKw: 1.4,
      currentSolarProductionKw: 3.2,
      updatedAt: new Date().toISOString(),
    });
  }

  async getHouseConfig(houseId: string): Promise<Result<HouseConfig>> {
    await delay(FAKE_DELAY_MS);
    return Result.success({ ...fakeHouse, houseId });
  }
}

export class FakeForecastRepository implements ForecastRepository {
  async getForecast(_location: GeoPoint): Promise<Result<Forecast>> {
    await delay(FAKE_DELAY_MS);
    const now = new Date();
    now.setMinutes(0, 0, 0);
    const points: ForecastPoint[] = Array.from({ length: 24 }, (_, h) => ({
      time: new Date(now.getTime() + h * 3600_000).toISOString(),
      solarRadiationWm2: (h * 40) % 800,
      pricePerKwh: 0.5 + (h % 6) * 0.1,
    }));
    return Result.success({ points });
  }
}

export class FakeProposalRepository implements ProposalRepository {
  private readonly proposals = new StateStream<BatteryProposal[]>([
    {
      id: 'p-1',
      houseId: 'h-1',
      description: 'Vinde 40% din baterie la 19:00',
      sellPercent: 40,
      executeAt: new Date(Date.now() + 4 * 3600_000).toISOString(),
      estimatedProfit: 12.5,
      status: ProposalStatus.PENDING,
    },
    {
      id: 'p-2',
      houseId: 'h-1',
      description: 'Vinde 20% din baterie la 21:00',
      sellPercent: 20,
      executeAt: new Date(Date.now() + 6 * 3600_000).toISOString(),
      estimatedProfit: null,
      status: ProposalStatus.PENDING,
    },
  ]);

  observePendingProposals(houseId: string): Observable<BatteryProposal[]> {
    return this.proposals.map((list) =>
      list.filter((p) => p.houseId === houseId && p.status === ProposalStatus.PENDING),
    );
  }

  accept(proposalId: string): Promise<Result<void>> {
    return this.update(proposalId, ProposalStatus.ACCEPTED);
  }

  reject(proposalId: string): Promise<Result<void>> {
    return this.update(proposalId, ProposalStatus.REJECTED);
  }

  private async update(id: string, status: ProposalStatus): Promise<Result<void>> {
    await delay(FAKE_DELAY_MS);
    if (!this.proposals.value.some((p) => p.id === id)) {
      return Result.failure(`Propunere inexistenta: ${id}`);
    }
    this.proposals.value = this.proposals.value.map((p) =>
      p.id === id ? { ...p, status } : p,
    );
    return Result.success(undefined);
  }
}

export class FakeProximityRepository implements ProximityRepository {
  readonly received: Array<{ houseId: string; event: ProximityEvent }> = [];

  async notifyProximity(houseId: string, event: ProximityEvent): Promise<Result<void>> {
    this.received.push({ houseId, event });
    return Result.success(undefined);
  }
}

export class FakeAdminRepository implements AdminRepository {
  private readonly houses: HouseConfig[] = [fakeHouse];

  async listUsers(): Promise<Result<User[]>> {
    return Result.success([fakeUser, fakeAdmin]);
  }

  async listHouses(): Promise<Result<HouseConfig[]>> {
    return Result.success([...this.houses]);
  }

  async updateHouse(config: HouseConfig): Promise<Result<HouseConfig>> {
    const idx = this.houses.findIndex((h) => h.houseId === config.houseId);
    if (idx < 0) {
      return Result.failure(`Casa inexistenta: ${config.houseId}`);
    }
    this.houses[idx] = config;
    return Result.success(config);
  }
}

export class FakeLocationProvider implements LocationProvider {
  locationUpdates(): Observable<GeoPoint> {
    return {
      subscribe(listener: (value: GeoPoint) => void): Unsubscribe {
        let step = 0;
        const emit = () => {
          listener({
            latitude: fakeHome.latitude + 0.01 - step * 0.001,
            longitude: fakeHome.longitude,
          });
          step = (step + 1) % 12;
        };
        emit();
        const timer = setInterval(emit, 1000);
        return () => clearInterval(timer);
      },
    };
  }

  async lastKnownLocation(): Promise<GeoPoint> {
    return fakeHome;
  }
}

export class FakeGeofenceManager implements GeofenceManager {
  registerHomeGeofence(_home: GeoPoint, _radiusMeters: number): void {}
  unregisterHomeGeofence(): void {}
}

/** Detector simplu: compara distanta fata de casa cu raza. */
export class FakeProximityDetector implements ProximityDetector {
  constructor(private readonly location: LocationProvider) {}

  observe(home: GeoPoint, radiusMeters: number): Observable<ProximityEvent> {
    return {
      subscribe: (listener: (value: ProximityEvent) => void): Unsubscribe => {
        let inside: boolean | null = null;
        return this.location.locationUpdates().subscribe((p) => {
          const nowInside = this.distanceMeters(home, p) <= radiusMeters;
          if (inside !== null && inside !== nowInside) {
            listener(
              nowInside ? ProximityEvent.APPROACHING_HOME : ProximityEvent.LEFT_HOME_AREA,
            );
          }
          inside = nowInside;
        });
      },
    };
  }

  private distanceMeters(a: GeoPoint, b: GeoPoint): number {
    const dLat = (b.latitude - a.latitude) * 111_320.0;
    const dLon =
      (b.longitude - a.longitude) * 111_320.0 * Math.cos((a.latitude * Math.PI) / 180.0);
    return Math.sqrt(dLat * dLat + dLon * dLon);
  }
}

export class FakeAppContainer implements AppContainer {
  readonly authRepository = new FakeAuthRepository();
  readonly houseRepository = new FakeHouseRepository();
  readonly forecastRepository = new FakeForecastRepository();
  readonly proposalRepository = new FakeProposalRepository();
  readonly proximityRepository = new FakeProximityRepository();
  readonly adminRepository = new FakeAdminRepository();
  readonly locationProvider = new FakeLocationProvider();
  readonly proximityDetector = new FakeProximityDetector(this.locationProvider);
  readonly geofenceManager = new FakeGeofenceManager();
}

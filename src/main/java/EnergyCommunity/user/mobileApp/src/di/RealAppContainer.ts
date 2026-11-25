import { CoreEnergyApi, HttpCoreEnergyApi } from '../api/core/CoreEnergyApi';
import { DEFAULT_BASE_URL, HttpClient } from '../api/interceptor/HttpClient';
import { HttpTelemetryApi, TelemetryApi } from '../api/telemetry/TelemetryApi';
import {
  FakeGeofenceManager,
  FakeLocationProvider,
  FakeProximityDetector,
} from '../data/fake/FakeAppContainer';
import { LocalDao } from '../data/local/LocalDao';
import {
  RealAdminRepository,
  RealAuthRepository,
  RealForecastRepository,
  RealHouseRepository,
  RealProposalRepository,
  RealProximityRepository,
} from '../data/repository/RealRepositories';
import { AppContainer } from './AppContainer';

export class RealAppContainer implements AppContainer {
  readonly httpClient: HttpClient;
  readonly coreEnergyApi: CoreEnergyApi;
  readonly telemetryApi: TelemetryApi;
  readonly localDao: LocalDao;

  readonly authRepository: RealAuthRepository;
  readonly houseRepository: RealHouseRepository;
  readonly forecastRepository: RealForecastRepository;
  readonly proposalRepository: RealProposalRepository;
  readonly proximityRepository: RealProximityRepository;
  readonly adminRepository: RealAdminRepository;

  readonly locationProvider = new FakeLocationProvider();
  readonly proximityDetector = new FakeProximityDetector(this.locationProvider);
  readonly geofenceManager = new FakeGeofenceManager();

  constructor(baseUrl: string = DEFAULT_BASE_URL) {
    this.httpClient = new HttpClient(baseUrl);
    this.coreEnergyApi = new HttpCoreEnergyApi(this.httpClient);
    this.telemetryApi = new HttpTelemetryApi(this.httpClient);
    this.localDao = new LocalDao();

    this.authRepository = new RealAuthRepository(this.coreEnergyApi, this.localDao);
    this.houseRepository = new RealHouseRepository(
      this.coreEnergyApi,
      this.telemetryApi,
      this.localDao,
    );
    this.forecastRepository = new RealForecastRepository(this.telemetryApi);
    this.proposalRepository = new RealProposalRepository(this.coreEnergyApi, this.localDao);
    this.proximityRepository = new RealProximityRepository(this.telemetryApi);
    this.adminRepository = new RealAdminRepository(
      this.coreEnergyApi,
      this.telemetryApi,
      this.localDao,
    );
  }
}

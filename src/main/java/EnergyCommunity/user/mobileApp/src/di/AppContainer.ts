import {
  AdminRepository,
  AuthRepository,
  ForecastRepository,
  HouseRepository,
  ProposalRepository,
  ProximityRepository,
} from '../domain/repository/Repositories';
import {
  GeofenceManager,
  LocationProvider,
  ProximityDetector,
} from '../geolocation/Geolocation';

/**
 * Singurul loc care cunoaste implementarile concrete.
 * Hook-urile / ViewModel-urile primesc doar interfete prin ViewModelFactory.
 */
export interface AppContainer {
  readonly authRepository: AuthRepository;
  readonly houseRepository: HouseRepository;
  readonly forecastRepository: ForecastRepository;
  readonly proposalRepository: ProposalRepository;
  readonly proximityRepository: ProximityRepository;
  readonly adminRepository: AdminRepository;
  readonly locationProvider: LocationProvider;
  readonly proximityDetector: ProximityDetector;
  readonly geofenceManager: GeofenceManager;
}

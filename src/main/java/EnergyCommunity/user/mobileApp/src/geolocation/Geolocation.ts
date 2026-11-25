import { GeoPoint, ProximityEvent } from '../domain/model/Models';
import { Observable } from '../domain/repository/Repositories';

/** Emite pozitia telefonului. Implementare reala: expo-location (in `impl`). */
export interface LocationProvider {
  locationUpdates(): Observable<GeoPoint>;
  lastKnownLocation(): Promise<GeoPoint | null>;
}

/** Inregistreaza o zona (geofence) in jurul casei. */
export interface GeofenceManager {
  registerHomeGeofence(home: GeoPoint, radiusMeters: number): void;
  unregisterHomeGeofence(): void;
}

/** Decide cand utilizatorul "se apropie de casa" pe baza pozitiilor. */
export interface ProximityDetector {
  observe(home: GeoPoint, radiusMeters: number): Observable<ProximityEvent>;
}

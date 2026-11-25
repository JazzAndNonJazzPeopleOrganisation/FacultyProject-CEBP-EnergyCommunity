import {
  BatteryProposal,
  HouseConfig,
  HouseStatus,
  ProposalStatus,
  User,
} from '../../domain/model/Models';
import { Observable, Unsubscribe } from '../../domain/repository/Repositories';

export class BehaviorStream<T> implements Observable<T> {
  private readonly listeners = new Set<(value: T) => void>();

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

/**
 * DAO local (in-memory reactive store) pentru sesiunea curenta,
 * cache-ul configuratiilor de case si propunerile primite de la agentul AI.
 */
export class LocalDao {
  private readonly currentUserStream = new BehaviorStream<User | null>(null);
  private readonly housesById = new Map<string, HouseConfig>();
  private readonly statusByHouseId = new Map<string, HouseStatus>();
  private readonly proposalsStream = new BehaviorStream<BatteryProposal[]>([]);

  // --- Session DAO ---
  observeCurrentUser(): Observable<User | null> {
    return this.currentUserStream;
  }

  getCurrentUser(): User | null {
    return this.currentUserStream.value;
  }

  setCurrentUser(user: User | null): void {
    this.currentUserStream.value = user;
  }

  // --- House Cache DAO ---
  saveHouseConfig(config: HouseConfig): void {
    this.housesById.set(config.houseId, config);
  }

  getHouseConfig(houseId: string): HouseConfig | undefined {
    return this.housesById.get(houseId);
  }

  removeHouseConfig(houseId: string): void {
    this.housesById.delete(houseId);
  }

  saveHouseStatus(status: HouseStatus): void {
    this.statusByHouseId.set(status.houseId, status);
  }

  getHouseStatus(houseId: string): HouseStatus | undefined {
    return this.statusByHouseId.get(houseId);
  }

  // --- Proposals Cache DAO ---
  observePendingProposals(houseId: string): Observable<BatteryProposal[]> {
    return this.proposalsStream.map((list) =>
      list.filter((p) => p.houseId === houseId && p.status === ProposalStatus.PENDING),
    );
  }

  setProposalsForHouse(houseId: string, proposals: BatteryProposal[]): void {
    const others = this.proposalsStream.value.filter((p) => p.houseId !== houseId);
    this.proposalsStream.value = [...others, ...proposals];
  }

  updateProposalStatus(proposalId: string, status: ProposalStatus): boolean {
    const exists = this.proposalsStream.value.some((p) => p.id === proposalId);
    if (!exists) return false;
    this.proposalsStream.value = this.proposalsStream.value.map((p) =>
      p.id === proposalId ? { ...p, status } : p,
    );
    return true;
  }
}

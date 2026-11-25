import { AppContainer } from '../di/AppContainer';
import {
  AcceptProposalUseCase,
  GetHouseStatusUseCase,
  LoginUseCase,
  LogoutUseCase,
  ObserveCurrentUserUseCase,
  ObservePendingProposalsUseCase,
  RejectProposalUseCase,
} from '../domain/usecase/UseCases';

export interface LoginDependencies {
  login: LoginUseCase;
  logout: LogoutUseCase;
  observeUser: ObserveCurrentUserUseCase;
}

export interface HomeDependencies {
  getStatus: GetHouseStatusUseCase;
  observeProposals: ObservePendingProposalsUseCase;
  accept: AcceptProposalUseCase;
  reject: RejectProposalUseCase;
}

export class ViewModelFactory {
  constructor(private readonly c: AppContainer) {}

  createLoginDependencies(): LoginDependencies {
    return {
      login: new LoginUseCase(this.c.authRepository),
      logout: new LogoutUseCase(this.c.authRepository),
      observeUser: new ObserveCurrentUserUseCase(this.c.authRepository),
    };
  }

  createHomeDependencies(): HomeDependencies {
    return {
      getStatus: new GetHouseStatusUseCase(this.c.houseRepository),
      observeProposals: new ObservePendingProposalsUseCase(this.c.proposalRepository),
      accept: new AcceptProposalUseCase(this.c.proposalRepository),
      reject: new RejectProposalUseCase(this.c.proposalRepository),
    };
  }
}

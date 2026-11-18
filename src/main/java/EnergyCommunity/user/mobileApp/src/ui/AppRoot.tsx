import React, { useMemo } from 'react';
import { AppContainer } from '../di/AppContainer';
import { ViewModelFactory } from './ViewModelFactory';
import { HomeScreen } from './home/HomeScreen';
import { useHomeViewModel } from './home/HomeViewModel';
import { LoginScreen } from './login/LoginScreen';
import { useLoginViewModel } from './login/LoginViewModel';

export interface AppRootProps {
  container: AppContainer;
}

export const AppRoot: React.FC<AppRootProps> = ({ container }) => {
  const factory = useMemo(() => new ViewModelFactory(container), [container]);
  const loginDeps = useMemo(() => factory.createLoginDependencies(), [factory]);
  const homeDeps = useMemo(() => factory.createHomeDependencies(), [factory]);

  const { state: loginState, user, onLogin, onLogout } = useLoginViewModel(loginDeps);
  const houseId = user?.houseId;
  const { state: homeState, onAccept, onReject } = useHomeViewModel(homeDeps, houseId);

  if (!user) {
    return <LoginScreen state={loginState} onLogin={onLogin} />;
  }

  return (
    <HomeScreen
      state={homeState}
      onAccept={onAccept}
      onReject={onReject}
      onLogout={onLogout}
    />
  );
};

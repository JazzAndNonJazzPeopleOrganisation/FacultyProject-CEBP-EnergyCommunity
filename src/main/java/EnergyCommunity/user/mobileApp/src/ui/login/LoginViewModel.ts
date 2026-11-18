import { useCallback, useEffect, useState } from 'react';
import { User } from '../../domain/model/Models';
import { LoginDependencies } from '../ViewModelFactory';

export interface LoginUiState {
  loading: boolean;
  error: string | null;
}

export function useLoginViewModel(deps: LoginDependencies) {
  const [state, setState] = useState<LoginUiState>({ loading: false, error: null });
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const unsubscribe = deps.observeUser.execute().subscribe(setUser);
    return unsubscribe;
  }, [deps]);

  const onLogin = useCallback(
    async (email: string, password: string) => {
      setState({ loading: true, error: null });
      const result = await deps.login.execute(email, password);
      if (result.ok) {
        setState({ loading: false, error: null });
      } else {
        setState({ loading: false, error: result.error.message || 'Eroare' });
      }
    },
    [deps],
  );

  const onLogout = useCallback(async () => {
    await deps.logout.execute();
  }, [deps]);

  return { state, user, onLogin, onLogout };
}

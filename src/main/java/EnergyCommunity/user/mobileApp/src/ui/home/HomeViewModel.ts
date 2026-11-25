import { useCallback, useEffect, useState } from 'react';
import { BatteryProposal, HouseStatus } from '../../domain/model/Models';
import { HomeDependencies } from '../ViewModelFactory';

export interface HomeUiState {
  loading: boolean;
  status: HouseStatus | null;
  proposals: BatteryProposal[];
  error: string | null;
}

export function useHomeViewModel(deps: HomeDependencies, houseId: string | null | undefined) {
  const [state, setState] = useState<HomeUiState>({
    loading: false,
    status: null,
    proposals: [],
    error: null,
  });

  useEffect(() => {
    if (!houseId) {
      setState({
        loading: false,
        status: null,
        proposals: [],
        error: null,
      });
      return;
    }
    let active = true;

    setState((prev) => ({ ...prev, loading: true, error: null }));
    deps.getStatus.execute(houseId).then((res) => {
      if (!active) return;
      if (res.ok) {
        setState((prev) => ({ ...prev, loading: false, status: res.value, error: null }));
      } else {
        setState((prev) => ({ ...prev, loading: false, error: res.error.message }));
      }
    });

    const unsubscribe = deps.observeProposals.execute(houseId).subscribe((list) => {
      if (!active) return;
      setState((prev) => ({ ...prev, proposals: list }));
    });

    return () => {
      active = false;
      unsubscribe();
    };
  }, [deps, houseId]);

  const onAccept = useCallback(
    async (id: string) => {
      const res = await deps.accept.execute(id);
      if (!res.ok) {
        setState((prev) => ({ ...prev, error: res.error.message }));
      }
    },
    [deps],
  );

  const onReject = useCallback(
    async (id: string) => {
      const res = await deps.reject.execute(id);
      if (!res.ok) {
        setState((prev) => ({ ...prev, error: res.error.message }));
      }
    },
    [deps],
  );

  return { state, onAccept, onReject };
}

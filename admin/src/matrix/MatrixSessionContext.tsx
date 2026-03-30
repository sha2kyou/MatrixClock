import React, {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
} from 'react';
import {
  authReset,
  clearStoredToken,
  getStoredToken,
  setStoredToken,
} from './api';

type MatrixSession = {
  token: string | null;
  setToken: (t: string | null) => void;
  toast: string | null;
  showToast: (msg: string) => void;
  logout: () => Promise<void>;
  onForbidden: () => void;
};

const Ctx = createContext<MatrixSession | null>(null);

export function MatrixSessionProvider({ children }: { children: React.ReactNode }) {
  const [token, setTokenState] = useState<string | null>(() => getStoredToken());
  const [toast, setToast] = useState<string | null>(null);

  const showToast = useCallback((msg: string) => {
    setToast(msg);
    window.setTimeout(() => setToast(null), 2800);
  }, []);

  const setToken = useCallback((t: string | null) => {
    if (t) setStoredToken(t);
    else clearStoredToken();
    setTokenState(t);
  }, []);

  const logout = useCallback(async () => {
    const tk = getStoredToken();
    if (tk) {
      try {
        await authReset(tk);
      } catch {
        /* ignore */
      }
    }
    clearStoredToken();
    setTokenState(null);
  }, []);

  const onForbidden = useCallback(() => {
    clearStoredToken();
    setTokenState(null);
    showToast('Session expired. Please authorize again.');
  }, [showToast]);

  const value = useMemo(
    () => ({
      token,
      setToken,
      toast,
      showToast,
      logout,
      onForbidden,
    }),
    [token, setToken, toast, showToast, logout, onForbidden],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useMatrixSession() {
  const v = useContext(Ctx);
  if (!v) throw new Error('useMatrixSession outside provider');
  return v;
}

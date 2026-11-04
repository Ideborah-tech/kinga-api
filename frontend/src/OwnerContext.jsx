import { createContext, useContext, useEffect, useState } from 'react';
import { api, setAuthToken } from './api.js';

// Signed-in developer. The session ({ token, user }) is remembered in the browser.
const STORAGE_KEY = 'kinga.session';
const AuthContext = createContext(null);

function readSession() {
  try {
    const session = JSON.parse(localStorage.getItem(STORAGE_KEY));
    setAuthToken(session?.token || null);
    return session?.token ? session : null;
  } catch {
    return null;
  }
}

export function OwnerProvider({ children }) {
  const [session, setSession] = useState(readSession);
  const [checking, setChecking] = useState(Boolean(session));

  const save = (next) => {
    setAuthToken(next?.token || null);
    setSession(next);
    try {
      if (next) localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      else localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Storage can be blocked; the session then lasts for this tab only.
    }
  };

  // A stored token may be stale (the backend's demo database resets on restart).
  useEffect(() => {
    if (!session) return;
    api.me()
      .then((user) => save({ token: session.token, user }))
      .catch(() => save(null))
      .finally(() => setChecking(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const value = {
    session,
    user: session?.user || null,
    checking,
    login: async (email, password) => save(await api.login(email, password)),
    register: async (name, email, password) => save(await api.register(name, email, password)),
    demoLogin: async () => save(await api.demoLogin()),
    logout: () => {
      api.logout().catch(() => {});
      save(null);
    },
  };
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);

/** The signed-in developer's handle, used as the owner of their monitors. */
export const useOwner = () => {
  const { user } = useAuth();
  return { owner: user?.handle };
};

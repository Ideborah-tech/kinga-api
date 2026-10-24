import { createContext, useContext, useState } from 'react';

// No login for the hackathon: the developer is picked from a list and remembered in the browser.
const OwnerContext = createContext({ owner: 'you', setOwner: () => {} });

function readOwner() {
  try {
    return localStorage.getItem('kinga.owner') || 'you';
  } catch {
    return 'you';
  }
}

export function OwnerProvider({ children }) {
  const [owner, setOwnerState] = useState(readOwner);
  const setOwner = (value) => {
    setOwnerState(value);
    try {
      localStorage.setItem('kinga.owner', value);
    } catch {
      // Storage can be blocked; the choice then lasts for this tab only.
    }
  };
  return <OwnerContext.Provider value={{ owner, setOwner }}>{children}</OwnerContext.Provider>;
}

export const useOwner = () => useContext(OwnerContext);

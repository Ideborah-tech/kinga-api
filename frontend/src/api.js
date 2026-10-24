// All calls to the Spring Boot backend live here.
export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8085';

async function request(path, { method = 'GET', body } = {}) {
  const res = await fetch(API_URL + path, {
    method,
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return null;
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const fieldErrors = data?.errors?.map((e) => `${e.field}: ${e.defaultMessage}`).join(', ');
    throw new Error(fieldErrors || data?.message || `Request failed (${res.status})`);
  }
  return data;
}

const q = encodeURIComponent;

export const api = {
  dashboard: (owner) => request(`/api/dashboard?owner=${q(owner)}`),

  monitors: (owner) => request(owner ? `/api/monitors?owner=${q(owner)}` : '/api/monitors'),
  monitor: (id) => request(`/api/monitors/${id}`),
  checks: (id, limit = 60) => request(`/api/monitors/${id}/checks?limit=${limit}`),
  checkNow: (id) => request(`/api/monitors/${id}/check`, { method: 'POST' }),
  report: (id) => request(`/api/monitors/${id}/report`),
  createMonitor: (monitor) => request('/api/monitors', { method: 'POST', body: monitor }),
  updateMonitor: (id, monitor) => request(`/api/monitors/${id}`, { method: 'PUT', body: monitor }),
  deleteMonitor: (id) => request(`/api/monitors/${id}`, { method: 'DELETE' }),
  shareMonitor: (id, description) => request(`/api/monitors/${id}/share`, { method: 'POST', body: { description } }),

  providers: () => request('/api/providers'),
  provider: (code) => request(`/api/providers/${code}`),
  checkProvider: (code) => request(`/api/providers/${code}/check-now`, { method: 'POST' }),
  incidents: (params = {}) => {
    const qs = new URLSearchParams(params).toString();
    return request(`/api/incidents${qs ? `?${qs}` : ''}`);
  },

  templates: (provider) => request(provider ? `/api/templates?provider=${q(provider)}` : '/api/templates'),
  template: (id) => request(`/api/templates/${id}`),

  demoModes: () => request('/api/demo/modes'),
  setDemoMode: (provider, mode) => request(`/api/demo/modes/${provider}`, { method: 'PUT', body: { mode } }),
  resetDemo: (owner) => request(`/api/demo/reset?owner=${q(owner)}`, { method: 'POST' }),
  setMyKey: (owner, provider, broken) =>
    request(`/api/demo/my-key?owner=${q(owner)}&provider=${provider}&broken=${broken}`, { method: 'POST' }),
};

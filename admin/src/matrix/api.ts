import type {
  AuthRecord,
  DeviceInfo,
  KeySettings,
  OpLogPage,
  PomodoroConfig,
  PomodoroSession,
} from './types';
import { TOKEN_STORAGE_KEY } from './constants';

export function getStoredToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

export function setStoredToken(token: string) {
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
}

export function clearStoredToken() {
  localStorage.removeItem(TOKEN_STORAGE_KEY);
}

function q(token: string, params: Record<string, string | number | undefined> = {}) {
  const u = new URLSearchParams();
  u.set('token', token);
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== '') u.set(k, String(v));
  }
  return u.toString();
}

/** POST /api/auth/bind — blocks until approved on device; body is token or DENIED */
export async function bindAuth(): Promise<string | 'DENIED' | null> {
  const r = await fetch('/api/auth/bind', { method: 'POST' });
  const t = (await r.text()).trim();
  if (t === 'DENIED') return 'DENIED';
  if (!t) return null;
  return t;
}

export async function postDeviceInfo(
  token: string,
  body: {
    deviceName?: string;
    deviceModel?: string;
    systemVersion?: string;
    batteryLevel?: number | null;
  },
) {
  await fetch(`/api/auth/device-info?${q(token)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      deviceName: body.deviceName ?? '',
      deviceModel: body.deviceModel ?? '',
      systemVersion: body.systemVersion ?? '',
      batteryLevel: body.batteryLevel ?? undefined,
    }),
  });
}

export async function authReset(token: string) {
  await fetch(`/api/auth/reset?${q(token)}`, { method: 'POST' });
}

export async function fetchAuthList(token: string): Promise<AuthRecord[]> {
  const r = await fetch(`/api/auth/list?${q(token)}`, { cache: 'no-store' });
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.json();
}

export async function revokeAuthToken(token: string, revokeToken: string) {
  const r = await fetch(
    `/api/auth/revoke?${q(token, { revokeToken })}`,
    { method: 'POST' },
  );
  if (r.status === 403) throw new Error('FORBIDDEN');
  if (!r.ok) throw new Error('FAILED');
}

export async function setMode(token: string, mode: 'CLOCK' | 'TEXT') {
  const r = await fetch(`/api/mode?${q(token, { mode })}`, { method: 'POST' });
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.ok;
}

export async function getClockTemplate(token: string): Promise<number> {
  const r = await fetch(`/api/clock/template?${q(token)}`, { cache: 'no-store' });
  if (r.status === 403) throw new Error('FORBIDDEN');
  const o = await r.json();
  return Math.max(1, Math.min(3, Number(o.template) || 1));
}

export async function setClockTemplate(token: string, template: number) {
  const r = await fetch(`/api/clock/template?${q(token, { template })}`, {
    method: 'POST',
  });
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.ok;
}

export async function postDisplayRollingText(
  token: string,
  args: { text: string; color: string; durationSec: number; style: number },
) {
  const r = await fetch(
    `/api/display?${q(token, {
      text: args.text,
      color: args.color,
      duration: args.durationSec,
      style: args.style,
    })}`,
    { method: 'POST' },
  );
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.ok;
}

export async function postDisplayStatus(
  token: string,
  args: { text: string; color: string; style: number; icon: string },
) {
  const r = await fetch(
    `/api/display?${q(token, {
      text: args.text,
      color: args.color,
      duration: 0,
      style: args.style,
      icon: args.icon,
    })}`,
    { method: 'POST' },
  );
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.ok;
}

export async function fetchPomodoroConfigs(token: string): Promise<PomodoroConfig[]> {
  const r = await fetch(`/api/pomodoro/configs?${q(token)}`, { cache: 'no-store' });
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.json();
}

export async function savePomodoroConfigs(token: string, configs: PomodoroConfig[]) {
  const r = await fetch(`/api/pomodoro/configs?${q(token)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(configs),
  });
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.ok;
}

export async function fetchPomodoroSessions(token: string): Promise<PomodoroSession[]> {
  const r = await fetch(`/api/pomodoro/sessions?${q(token)}`, { cache: 'no-store' });
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.json();
}

export async function fetchKeySettings(token: string): Promise<KeySettings> {
  const r = await fetch(`/api/keys/settings?${q(token)}`, { cache: 'no-store' });
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.json();
}

export async function saveKeySettings(token: string, s: KeySettings) {
  const r = await fetch(
    `/api/keys/settings?${q(token, {
      volUpShort: s.volUpShort,
      volUpLong: s.volUpLong,
      volUpDouble: s.volUpDouble,
      volDownShort: s.volDownShort,
      volDownLong: s.volDownLong,
      volDownDouble: s.volDownDouble,
    })}`,
    { method: 'POST' },
  );
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.ok;
}

export async function fetchDeviceInfo(token: string): Promise<DeviceInfo> {
  const r = await fetch(`/api/device/info?${q(token)}`, { cache: 'no-store' });
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.json();
}

export async function fetchOplogPage(
  token: string,
  page: number,
  pageSize: number,
): Promise<OpLogPage> {
  const r = await fetch(
    `/api/oplog?${q(token, { page, pageSize })}`,
    { cache: 'no-store' },
  );
  if (r.status === 403) throw new Error('FORBIDDEN');
  return r.json();
}

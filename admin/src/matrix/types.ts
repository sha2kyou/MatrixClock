/**
 * Types aligned with MatrixClock Kotlin @Serializable models (JSON field names).
 */

export type DisplayMode = 'CLOCK' | 'TEXT';

export interface PomodoroConfig {
  id: string;
  text: string;
  durationSec: number;
  colorHex: string;
  isPrimary?: boolean;
  countdownStyle?: number;
  cycleTotal?: number;
}

export interface AuthRecord {
  token: string;
  ip: string;
  deviceName?: string;
  deviceModel?: string;
  systemVersion?: string;
  batteryLevel?: number;
  createdAt?: number;
}

export interface DeviceInfo {
  model: string;
  manufacturer: string;
  device: string;
  androidVersion: string;
  sdkInt: number;
  batteryLevel: number;
  batteryStatus: string;
  screenWidthPx: number;
  screenHeightPx: number;
  screenDensity: number;
  isCharging: boolean;
}

export interface PomodoroSession {
  configId: string;
  configText: string;
  startTimeMillis: number;
  plannedDurationSec: number;
  completed: boolean;
  actualDurationSec: number;
}

export interface KeySettings {
  volUpShort: string;
  volUpLong: string;
  volUpDouble: string;
  volDownShort: string;
  volDownLong: string;
  volDownDouble: string;
}

export interface OpLogEntry {
  timeMillis: number;
  action: string;
  detail?: string;
  ip?: string;
}

export interface OpLogPage {
  items: OpLogEntry[];
  hasMore: boolean;
}

export const TOKEN_STORAGE_KEY = 'matrix_tk';

export const PRESET_COLORS = [
  '#FF0000',
  '#FF7F00',
  '#FFFF00',
  '#00FF00',
  '#00FFFF',
  '#0000FF',
  '#8B00FF',
] as const;

export const STATUS_ICON_OPTIONS = [
  { value: '', label: 'None' },
  { value: 'star', label: 'Star' },
  { value: 'moon', label: 'Moon' },
  { value: 'phone', label: 'Phone' },
  { value: 'alarm', label: 'Alarm' },
  { value: 'clock', label: 'Clock' },
  { value: 'stopwatch', label: 'Stopwatch' },
] as const;

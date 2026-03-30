import React, { useEffect, useState } from 'react';
import { Gamepad2, Zap } from 'lucide-react';
import type { KeySettings } from '../matrix/types';
import { fetchKeySettings, saveKeySettings } from '../matrix/api';
import { useMatrixSession } from '../matrix/MatrixSessionContext';

const keyOptions = [
  { value: 'none', label: 'None' },
  { value: 'pomodoro', label: 'Pomodoro Timer' },
  { value: 'switch_pomodoro', label: 'Switch Pomodoro Preset' },
  { value: 'menu', label: 'Menu' },
];

const rows: {
  key: keyof KeySettings;
  title: string;
  sub: string;
  options: readonly { value: string; label: string }[];
}[] = [
  { key: 'volUpShort', title: 'Volume +', sub: 'Short press', options: keyOptions },
  { key: 'volDownShort', title: 'Volume -', sub: 'Short press', options: keyOptions },
  { key: 'volUpLong', title: 'Volume +', sub: 'Long press', options: keyOptions },
  { key: 'volDownLong', title: 'Volume -', sub: 'Long press', options: keyOptions },
  { key: 'volUpDouble', title: 'Volume +', sub: 'Double press', options: keyOptions },
  { key: 'volDownDouble', title: 'Volume -', sub: 'Double press', options: keyOptions },
];

const VALID_KEY_VALUES = new Set(['menu', 'pomodoro', 'none', 'switch_pomodoro']);

function normalizeKeyValue(value: string | undefined, fallback: KeySettings[keyof KeySettings]) {
  if (!value) return fallback;
  return VALID_KEY_VALUES.has(value) ? value : fallback;
}

export function KeysSection() {
  const { token, showToast, onForbidden } = useMatrixSession();
  const [s, setS] = useState<KeySettings>({
    volUpShort: 'pomodoro',
    volUpLong: 'menu',
    volUpDouble: 'switch_pomodoro',
    volDownShort: 'pomodoro',
    volDownLong: 'menu',
    volDownDouble: 'switch_pomodoro',
  });

  useEffect(() => {
    if (!token) return;
    (async () => {
      try {
        const o = await fetchKeySettings(token);
        setS({
          volUpShort: normalizeKeyValue(o.volUpShort, 'pomodoro'),
          volUpLong: normalizeKeyValue(o.volUpLong, 'menu'),
          volUpDouble: normalizeKeyValue(o.volUpDouble, 'switch_pomodoro'),
          volDownShort: normalizeKeyValue(o.volDownShort, 'pomodoro'),
          volDownLong: normalizeKeyValue(o.volDownLong, 'menu'),
          volDownDouble: normalizeKeyValue(o.volDownDouble, 'switch_pomodoro'),
        });
      } catch (e) {
        if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
      }
    })();
  }, [token, onForbidden]);

  const save = async () => {
    if (!token) return;
    try {
      await saveKeySettings(token, s);
      showToast('Key mapping saved');
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl sm:text-2xl font-semibold">Volume Key Mapping</h2>
        <p className="text-xs sm:text-sm text-hw-muted mt-1">Short / Long (~0.5s) / Double press</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {rows.map((row) => (
          <div key={row.key} className="hw-panel p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <div className="text-[10px] font-bold uppercase text-hw-accent">{row.sub}</div>
                <div className="text-sm font-bold">{row.title}</div>
              </div>
              <div className="w-10 h-10 rounded-xl bg-hw-surface flex items-center justify-center text-hw-muted">
                <Gamepad2 size={20} />
              </div>
            </div>
            <select
              className="w-full hw-input"
              value={s[row.key]}
              onChange={(e) => setS((prev) => ({ ...prev, [row.key]: e.target.value }))}
            >
              {row.options.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </div>
        ))}
      </div>

      <button type="button" className="w-full sm:w-auto hw-button px-8 py-3" onClick={save}>
        Save to device
      </button>

      <div className="hw-panel p-4 bg-blue-500/5 border-blue-500/20 flex items-start gap-3">
        <div className="w-8 h-8 rounded-full bg-blue-500/10 flex items-center justify-center text-blue-400 shrink-0 mt-0.5">
          <Zap size={16} />
        </div>
        <p className="text-[11px] text-hw-muted leading-relaxed">
          Matches MatrixClock Android behavior. Changes take effect on physical buttons immediately.
        </p>
      </div>
    </div>
  );
}

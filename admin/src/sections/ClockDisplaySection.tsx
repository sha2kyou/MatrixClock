import React, { useCallback, useEffect, useState } from 'react';
import { Check, Zap } from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { PRESET_COLORS, STATUS_ICON_OPTIONS } from '../matrix/constants';
import {
  getClockTemplate,
  postDisplayRollingText,
  postDisplayStatus,
  setClockTemplate,
  setMode,
} from '../matrix/api';
import { useMatrixSession } from '../matrix/MatrixSessionContext';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

function pickRollingColor(selected: string) {
  if (selected === 'random') {
    return PRESET_COLORS[Math.floor(Math.random() * PRESET_COLORS.length)]!;
  }
  return selected;
}

const templates = [
  { id: 1, name: 'Template 1', description: 'Large clock + top-left date' },
  { id: 2, name: 'Template 2', description: 'Compact single line' },
  { id: 3, name: 'Template 3', description: 'Left hour + right minute scroll' },
];

export function ClockTemplateSection() {
  const { token, showToast, onForbidden } = useMatrixSession();
  const [tpl, setTpl] = useState(1);

  const handleErr = useCallback(
    (e: unknown) => {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    },
    [onForbidden],
  );

  useEffect(() => {
    if (!token) return;
    (async () => {
      try {
        const t = await getClockTemplate(token);
        setTpl(t);
      } catch (e) {
        handleErr(e);
      }
    })();
  }, [token, handleErr]);

  const applyTemplate = async (id: number) => {
    if (!token) return;
    try {
      await setClockTemplate(token, id);
      setTpl(id);
      showToast(`Applied template ${id}`);
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-xl sm:text-2xl font-semibold">Clock Templates</h2>
        <p className="text-xs sm:text-sm text-hw-muted mt-1">
          Current template: <span className="text-hw-text font-mono">{tpl}</span>
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mt-4">
          {templates.map((x) => (
            <button
              key={x.id}
              type="button"
              onClick={() => applyTemplate(x.id)}
              className={cn(
                'hw-panel cursor-pointer p-6 transition-all text-left relative',
                tpl === x.id
                  ? 'border-hw-accent ring-1 ring-hw-accent/50'
                  : 'hover:border-hw-border',
              )}
            >
              {tpl === x.id && (
                <div className="absolute top-3 right-3 w-6 h-6 bg-hw-accent rounded-full flex items-center justify-center">
                  <Check size={14} className="text-white" />
                </div>
              )}
              <h3 className="font-bold">{x.name}</h3>
              <p className="text-xs text-hw-muted mt-1">{x.description}</p>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}

export function CountdownSection() {
  const { token, showToast, onForbidden } = useMatrixSession();
  const [rollColor, setRollColor] = useState('#FF0000');
  const [rollText, setRollText] = useState('');
  const [rollDur, setRollDur] = useState(60);
  const [rollStyle, setRollStyle] = useState(1);

  const backToClock = async () => {
    if (!token) return;
    try {
      await setMode(token, 'CLOCK');
      showToast('Switched back to clock mode');
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  };

  const sendRolling = async () => {
    if (!token) return;
    try {
      await setMode(token, 'TEXT');
      const color = pickRollingColor(rollColor);
      await postDisplayRollingText(token, {
        text: rollText.slice(0, 20),
        color,
        durationSec: rollDur,
        style: rollStyle,
      });
      showToast('Countdown text sent');
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  };

  return (
    <div className="space-y-6">
      <h2 className="text-xl sm:text-2xl font-semibold">Countdown Text</h2>
      <div className="hw-panel p-5 sm:p-6 space-y-4">
        <div className="flex items-center gap-2 text-hw-accent">
          <Zap size={16} />
          <h3 className="font-bold">Scrolling / Countdown Text</h3>
        </div>
        <div className="flex flex-wrap gap-2">
          {PRESET_COLORS.map((c) => (
            <button
              key={c}
              type="button"
              onClick={() => setRollColor(c)}
              className={cn(
                'w-8 h-8 rounded-full border-2 transition-all',
                rollColor === c ? 'border-white scale-110' : 'border-transparent',
              )}
              style={{ backgroundColor: c }}
            />
          ))}
          <button
            type="button"
            onClick={() => setRollColor('random')}
            className={cn(
              'w-8 h-8 rounded-full p-[3px] transition-all',
              rollColor === 'random' ? 'ring-2 ring-white scale-110' : '',
            )}
            style={{ background: 'conic-gradient(#FF0000, #FF7F00, #FFFF00, #00FF00, #00FFFF, #0000FF, #8B00FF, #FF0000)' }}
            title="Random color"
          >
            <span className="block w-full h-full rounded-full bg-hw-bg/80" />
          </button>
        </div>
        <label className="block text-[10px] font-bold uppercase text-hw-muted">Text (≤20)</label>
        <input
          className="w-full hw-input"
          value={rollText}
          maxLength={20}
          onChange={(e) => setRollText(e.target.value)}
          placeholder="Enter text..."
        />
        <label className="block text-[10px] font-bold uppercase text-hw-muted">Duration (seconds)</label>
        <input
          type="number"
          className="w-full hw-input"
          min={5}
          max={3600}
          value={rollDur || ''}
          onChange={(e) => setRollDur(e.target.value === '' ? 0 : Number(e.target.value))}
        />
        <label className="block text-[10px] font-bold uppercase text-hw-muted">Countdown style</label>
        <select
          className="w-full hw-input"
          value={rollStyle}
          onChange={(e) => setRollStyle(Number(e.target.value))}
        >
          <option value={1}>Style 1 · Dual-line progress</option>
          <option value={2}>Style 2 · Full-screen progress bar</option>
          <option value={3}>Style 3 · Left text + right matrix</option>
        </select>
        <div className="flex flex-col sm:flex-row gap-2">
          <button type="button" className="flex-1 hw-button py-3" onClick={sendRolling}>
            Send to device
          </button>
          <button type="button" className="flex-1 hw-button-secondary py-3" onClick={backToClock}>
            Switch to clock
          </button>
        </div>
      </div>
    </div>
  );
}

export function ResidentStatusSection() {
  const { token, showToast, onForbidden } = useMatrixSession();
  const [statColor, setStatColor] = useState('#FF0000');
  const [statText, setStatText] = useState('');
  const [statStyle, setStatStyle] = useState(1);
  const [statIcon, setStatIcon] = useState('');

  const backToClock = async () => {
    if (!token) return;
    try {
      await setMode(token, 'CLOCK');
      showToast('Switched back to clock mode');
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  };

  const sendStatus = async () => {
    if (!token) return;
    try {
      await setMode(token, 'TEXT');
      const color = pickRollingColor(statColor);
      await postDisplayStatus(token, {
        text: statText.slice(0, 20),
        color,
        style: statStyle,
        icon: statIcon,
      });
      showToast('Resident status updated');
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  };

  return (
    <div className="space-y-6">
      <h2 className="text-xl sm:text-2xl font-semibold">Resident Status</h2>
      <div className="hw-panel p-5 sm:p-6 space-y-4">
        <h3 className="font-bold">Resident Display</h3>
        <p className="text-xs text-hw-muted">
          No countdown, persistent display (same behavior as official web admin page)
        </p>
        <div className="flex flex-wrap gap-2">
          {PRESET_COLORS.map((c) => (
            <button
              key={c}
              type="button"
              onClick={() => setStatColor(c)}
              className={cn(
                'w-8 h-8 rounded-full border-2 transition-all',
                statColor === c ? 'border-white scale-110' : 'border-transparent',
              )}
              style={{ backgroundColor: c }}
            />
          ))}
          <button
            type="button"
            onClick={() => setStatColor('random')}
            className={cn(
              'w-8 h-8 rounded-full p-[3px] transition-all',
              statColor === 'random' ? 'ring-2 ring-white scale-110' : '',
            )}
            style={{ background: 'conic-gradient(#FF0000, #FF7F00, #FFFF00, #00FF00, #00FFFF, #0000FF, #8B00FF, #FF0000)' }}
            title="Random color"
          >
            <span className="block w-full h-full rounded-full bg-hw-bg/80" />
          </button>
        </div>
        <input
          className="w-full hw-input"
          value={statText}
          maxLength={20}
          onChange={(e) => setStatText(e.target.value)}
          placeholder="Status text..."
        />
        <label className="block text-[10px] font-bold uppercase text-hw-muted">Badge icon</label>
        <select
          className="w-full hw-input"
          value={statIcon}
          onChange={(e) => setStatIcon(e.target.value)}
        >
          {STATUS_ICON_OPTIONS.map((o) => (
            <option key={o.value || 'none'} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        <select
          className="w-full hw-input"
          value={statStyle}
          onChange={(e) => setStatStyle(Number(e.target.value))}
        >
          <option value={1}>Style 1</option>
          <option value={2}>Style 2</option>
          <option value={3}>Style 3</option>
        </select>
        <div className="flex flex-col sm:flex-row gap-2">
          <button type="button" className="flex-1 hw-button py-3" onClick={sendStatus}>
            Update status
          </button>
          <button type="button" className="flex-1 hw-button-secondary py-3" onClick={backToClock}>
            Switch to clock
          </button>
        </div>
      </div>
    </div>
  );
}

export function ClockDisplaySection() {
  return (
    <div className="space-y-10">
      <ClockTemplateSection />
      <CountdownSection />
      <ResidentStatusSection />
    </div>
  );
}

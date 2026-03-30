import React, { useCallback, useEffect, useRef, useState } from 'react';
import { ChevronRight, Plus, Timer, Trash2, X } from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { PomodoroConfig } from '../matrix/types';
import { PRESET_COLORS } from '../matrix/constants';
import { fetchPomodoroConfigs, savePomodoroConfigs } from '../matrix/api';
import { useMatrixSession } from '../matrix/MatrixSessionContext';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

function useDebouncedSave(fn: (configs: PomodoroConfig[]) => void, ms: number) {
  const t = useRef<ReturnType<typeof setTimeout> | null>(null);
  return useCallback(
    (next: PomodoroConfig[]) => {
      if (t.current) clearTimeout(t.current);
      t.current = setTimeout(() => fn(next), ms);
    },
    [fn, ms],
  );
}

function PomoModal({
  open,
  onClose,
  initial,
  onSave,
  onDelete,
}: {
  open: boolean;
  onClose: () => void;
  initial: PomodoroConfig | null;
  onSave: (c: PomodoroConfig) => void;
  onDelete?: () => void;
}) {
  const [text, setText] = useState('');
  const [durationMin, setDurationMin] = useState(25);
  const [colorHex, setColorHex] = useState('#FF0000');
  const [countdownStyle, setCountdownStyle] = useState(1);
  const [cycleTotal, setCycleTotal] = useState(12);
  const [isPrimary, setIsPrimary] = useState(false);

  useEffect(() => {
    if (!open) return;
    if (initial) {
      setText(initial.text);
      setDurationMin(Math.max(1, Math.round((initial.durationSec || 60) / 60)));
      setColorHex(
        String(initial.colorHex).toLowerCase() === 'random'
          ? 'random'
          : initial.colorHex || '#FF0000',
      );
      setCountdownStyle(initial.countdownStyle ?? 1);
      setCycleTotal(initial.cycleTotal ?? 12);
      setIsPrimary(!!initial.isPrimary);
    } else {
      setText('Focus');
      setDurationMin(25);
      setColorHex('#FF0000');
      setCountdownStyle(1);
      setCycleTotal(12);
      setIsPrimary(false);
    }
  }, [initial, open]);

  if (!open) return null;

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const id = initial?.id ?? `p_${Date.now()}`;
    onSave({
      id,
      text: text.slice(0, 20),
      durationSec: Math.min(7200, Math.max(60, durationMin * 60)),
      colorHex: colorHex === 'random' ? 'random' : colorHex,
      isPrimary,
      countdownStyle,
      cycleTotal: Math.max(1, Math.min(99, cycleTotal)),
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-md">
      <div
        className={cn(
          'hw-panel w-full max-w-md flex flex-col max-h-[90vh]',
          'shadow-2xl',
        )}
      >
        <div className="px-6 py-4 border-b border-hw-border flex justify-between items-center bg-hw-surface shrink-0">
          <h3 className="text-lg font-bold">{initial ? 'Edit Preset' : 'New Preset'}</h3>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-hw-surface-hover flex items-center justify-center text-hw-muted"
          >
            <X size={18} />
          </button>
        </div>
        <form onSubmit={submit} className="p-6 space-y-4 overflow-y-auto">
          <div>
            <label className="text-[10px] font-bold uppercase text-hw-muted">Label</label>
            <input
              className="w-full hw-input mt-1"
              value={text}
              maxLength={20}
              onChange={(e) => setText(e.target.value)}
              required
            />
          </div>
          <div>
            <label className="text-[10px] font-bold uppercase text-hw-muted">Duration (minutes)</label>
            <input
              type="number"
              className="w-full hw-input mt-1"
              min={1}
              max={120}
              value={durationMin || ''}
              onChange={(e) => setDurationMin(e.target.value === '' ? 0 : Number(e.target.value))}
            />
          </div>
          <div>
            <label className="text-[10px] font-bold uppercase text-hw-muted">Style</label>
            <select
              className="w-full hw-input mt-1"
              value={countdownStyle}
              onChange={(e) => setCountdownStyle(Number(e.target.value))}
            >
              <option value={1}>Style 1 · Dual line</option>
              <option value={2}>Style 2 · Full-screen bar</option>
              <option value={3}>Style 3 · Left text + right matrix</option>
            </select>
          </div>
          <div>
            <label className="text-[10px] font-bold uppercase text-hw-muted">Daily target (x/N)</label>
            <input
              type="number"
              className="w-full hw-input mt-1"
              min={1}
              max={99}
              value={cycleTotal || ''}
              onChange={(e) => setCycleTotal(e.target.value === '' ? 0 : Number(e.target.value))}
            />
          </div>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="checkbox"
              checked={isPrimary}
              onChange={(e) => setIsPrimary(e.target.checked)}
            />
            Set as primary preset (default for shortcuts)
          </label>
          <div>
            <label className="text-[10px] font-bold uppercase text-hw-muted">Color</label>
            <div className="flex flex-wrap gap-2 mt-2">
              {PRESET_COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setColorHex(c)}
                  className={cn(
                    'w-9 h-9 rounded-full border-2',
                    colorHex === c ? 'border-white scale-110' : 'border-transparent',
                  )}
                  style={{ backgroundColor: c }}
                />
              ))}
              <button
                type="button"
                onClick={() => setColorHex('random')}
                className={cn(
                  'w-9 h-9 rounded-full p-[3px] transition-all',
                  colorHex === 'random' ? 'ring-2 ring-white scale-110' : '',
                )}
                style={{
                  background:
                    'conic-gradient(#FF0000, #FF7F00, #FFFF00, #00FF00, #00FFFF, #0000FF, #8B00FF, #FF0000)',
                }}
                title="Random color"
              >
                <span className="block w-full h-full rounded-full bg-hw-bg/80" />
              </button>
            </div>
          </div>
          <div className="flex gap-2 pt-2">
            {initial && onDelete && (
              <button
                type="button"
                onClick={onDelete}
                className="w-12 h-12 rounded-full bg-red-500/15 border border-red-500/30 text-red-400 flex items-center justify-center"
              >
                <Trash2 size={18} />
              </button>
            )}
            <button type="button" onClick={onClose} className="flex-1 hw-button-secondary py-3">
              Cancel
            </button>
            <button type="submit" className="flex-1 hw-button py-3">
              Save
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export function PomodoroSection() {
  const { token, showToast, onForbidden } = useMatrixSession();
  const [configs, setConfigs] = useState<PomodoroConfig[]>([]);
  const [modal, setModal] = useState<PomodoroConfig | 'new' | null>(null);

  const persist = useCallback(
    async (next: PomodoroConfig[]) => {
      if (!token) return;
      try {
        const ok = await savePomodoroConfigs(token, next);
        if (ok) showToast('Saved');
      } catch (e) {
        if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
      }
    },
    [token, showToast, onForbidden],
  );

  const debouncedPersist = useDebouncedSave(persist, 500);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const list = await fetchPomodoroConfigs(token);
      setConfigs(list);
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  }, [token, onForbidden]);

  useEffect(() => {
    load();
  }, [load]);

  const pushAndSave = (next: PomodoroConfig[]) => {
    setConfigs(next);
    debouncedPersist(next);
  };

  const handleSaveModal = (c: PomodoroConfig) => {
    let next: PomodoroConfig[];
    const exists = configs.some((x) => x.id === c.id);
    if (exists) {
      next = configs.map((x) => (x.id === c.id ? c : { ...x, isPrimary: c.isPrimary ? false : x.isPrimary }));
    } else {
      next = [...configs.map((x) => ({ ...x, isPrimary: c.isPrimary ? false : x.isPrimary })), c];
    }
    if (!next.some((x) => x.isPrimary) && next.length > 0) {
      next = next.map((x, i) => ({ ...x, isPrimary: i === 0 }));
    }
    setConfigs(next);
    persist(next);
  };

  const handleDelete = (id: string) => {
    const was = configs.find((x) => x.id === id);
    let next = configs.filter((x) => x.id !== id);
    if (was?.isPrimary && next.length > 0) next = next.map((x, i) => ({ ...x, isPrimary: i === 0 }));
    setModal(null);
    pushAndSave(next);
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-start gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-semibold">Pomodoro Presets</h2>
          <p className="text-xs sm:text-sm text-hw-muted mt-1">
            Same behavior as the app. Changes are saved to the device.
          </p>
        </div>
        <button type="button" onClick={load} className="hw-button-secondary text-xs py-2 px-3 shrink-0">
          Refresh
        </button>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
        {configs.map((config) => (
          <button
            key={config.id}
            type="button"
            onClick={() => setModal(config)}
            className={cn(
              'hw-panel group relative text-left p-4 aspect-square flex flex-col transition-transform hover:scale-[1.02]',
              config.isPrimary && 'ring-1 ring-hw-accent',
            )}
          >
            {config.isPrimary && (
              <span className="absolute bottom-2.5 right-2.5 px-1.5 py-0.5 text-[9px] font-bold uppercase rounded-md bg-hw-accent text-white leading-none">
                Primary
              </span>
            )}
            <div
              className="w-10 h-10 rounded-full flex items-center justify-center text-white mb-auto shadow-lg"
              style={{
                backgroundColor:
                  String(config.colorHex).toLowerCase() === 'random' ? '#888' : config.colorHex,
              }}
            >
              <Timer size={20} />
            </div>
            <div className="mt-3">
              <div className="font-bold text-sm line-clamp-1">{config.text}</div>
              <div className="text-lg font-bold text-hw-accent">
                {Math.round((config.durationSec || 0) / 60)}
                <span className="text-[9px] font-bold text-hw-muted ml-0.5">min</span>
              </div>
            </div>
            <ChevronRight
              size={14}
              className="absolute top-3 right-3 opacity-0 group-hover:opacity-100 text-hw-muted"
            />
          </button>
        ))}
        <button
          type="button"
          onClick={() => setModal('new')}
          className="hw-panel border-dashed border-2 flex flex-col items-center justify-center aspect-square text-hw-muted hover:text-hw-accent hover:border-hw-accent"
        >
          <Plus size={24} className="mb-2" />
          <span className="text-[10px] font-bold uppercase">Add</span>
        </button>
      </div>

      <PomoModal
        open={modal !== null}
        onClose={() => setModal(null)}
        initial={modal === 'new' || modal === null ? null : modal}
        onSave={handleSaveModal}
        onDelete={
          modal && modal !== 'new'
            ? () => handleDelete((modal as PomodoroConfig).id)
            : undefined
        }
      />
    </div>
  );
}

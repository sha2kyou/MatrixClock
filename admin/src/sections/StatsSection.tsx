import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  Chart,
  BarController,
  BarElement,
  CategoryScale,
  LinearScale,
  Legend,
  Tooltip,
} from 'chart.js';
import type { PomodoroSession } from '../matrix/types';
import { fetchPomodoroSessions } from '../matrix/api';
import { useMatrixSession } from '../matrix/MatrixSessionContext';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Legend, Tooltip);

function dayKey(ms: number) {
  const d = new Date(ms);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function todayKey() {
  return dayKey(Date.now());
}

function formatFocusMin(sec: number) {
  const m = Math.round(sec / 60);
  if (m >= 60) return `${Math.floor(m / 60)}h ${m % 60}m`;
  return `${m} min`;
}

export function StatsSection() {
  const { token, onForbidden } = useMatrixSession();
  const [sessions, setSessions] = useState<PomodoroSession[]>([]);
  const c1 = useRef<HTMLCanvasElement>(null);
  const c2 = useRef<HTMLCanvasElement>(null);
  const chart1 = useRef<Chart | null>(null);
  const chart2 = useRef<Chart | null>(null);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const list = await fetchPomodoroSessions(token);
      setSessions(list);
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  }, [token, onForbidden]);

  useEffect(() => {
    load();
  }, [load]);

  const byDay: Record<string, { completed: number; interrupts: number; focusSec: number }> = {};
  const byPreset: Record<
    string,
    { text: string; completed: number; total: number; focusSec: number }
  > = {};
  let totalCompleted = 0;
  let totalSessions = 0;
  let totalFocusSec = 0;
  const today = todayKey();
  let todayCompleted = 0;
  let todayFocusSec = 0;

  for (const s of sessions) {
    const d = dayKey(s.startTimeMillis);
    if (!byDay[d]) byDay[d] = { completed: 0, interrupts: 0, focusSec: 0 };
    byDay[d].completed += s.completed ? 1 : 0;
    byDay[d].interrupts += s.completed ? 0 : 1;
    byDay[d].focusSec += s.actualDurationSec || 0;
    totalSessions += 1;
    if (s.completed) totalCompleted += 1;
    totalFocusSec += s.actualDurationSec || 0;
    if (d === today) {
      if (s.completed) todayCompleted += 1;
      todayFocusSec += s.actualDurationSec || 0;
    }
    const key = s.configId || s.configText || '?';
    if (!byPreset[key])
      byPreset[key] = { text: s.configText || key, completed: 0, total: 0, focusSec: 0 };
    byPreset[key].total += 1;
    if (s.completed) byPreset[key].completed += 1;
    byPreset[key].focusSec += s.actualDurationSec || 0;
  }

  const completionRate = totalSessions > 0 ? Math.round((100 * totalCompleted) / totalSessions) : 0;
  const days = Object.keys(byDay)
    .sort()
    .reverse()
    .slice(0, 14)
    .reverse();

  useEffect(() => {
    if (!days.length || !c1.current || !c2.current) {
      chart1.current?.destroy();
      chart2.current?.destroy();
      chart1.current = null;
      chart2.current = null;
      return;
    }
    chart1.current?.destroy();
    chart2.current?.destroy();
    chart1.current = new Chart(c1.current, {
      type: 'bar',
      data: {
        labels: days.map((d) => d.slice(5)),
        datasets: [
          {
            label: 'Completed',
            data: days.map((d) => byDay[d]?.completed ?? 0),
            backgroundColor: 'rgba(10,132,255,0.85)',
          },
          {
            label: 'Interrupted',
            data: days.map((d) => byDay[d]?.interrupts ?? 0),
            backgroundColor: 'rgba(248,113,113,0.75)',
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {},
          y: {
            beginAtZero: true,
            ticks: { precision: 0 },
          },
        },
        plugins: {
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const v = ctx.parsed.y;
                const n = typeof v === 'number' ? Math.round(v) : v;
                return `${ctx.dataset.label}: ${n}`;
              },
            },
          },
        },
      },
    });
    chart2.current = new Chart(c2.current, {
      type: 'bar',
      data: {
        labels: days.map((d) => d.slice(5)),
        datasets: [
          {
            label: 'Focus (min)',
            data: days.map((d) =>
              byDay[d]?.focusSec ? Math.round(byDay[d].focusSec / 60) : 0,
            ),
            backgroundColor: 'rgba(52,199,89,0.75)',
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {},
          y: {
            beginAtZero: true,
            ticks: { precision: 0 },
          },
        },
        plugins: {
          tooltip: {
            callbacks: {
              label: (ctx) => {
                const v = ctx.parsed.y;
                const n = typeof v === 'number' ? Math.round(v) : v;
                return `${ctx.dataset.label}: ${n}`;
              },
            },
          },
        },
      },
    });
    return () => {
      chart1.current?.destroy();
      chart2.current?.destroy();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- charts sync to derived byDay/days
  }, [sessions]);

  const presetKeys = Object.keys(byPreset);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h2 className="text-xl sm:text-2xl font-semibold">Pomodoro Stats</h2>
        <button type="button" onClick={load} className="hw-button-secondary text-xs py-2 px-3">
          Refresh
        </button>
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <div className="hw-panel p-4">
          <div className="text-hw-muted text-xs">Completed Today</div>
          <div className="text-2xl font-bold mt-1">{todayCompleted}</div>
        </div>
        <div className="hw-panel p-4">
          <div className="text-hw-muted text-xs">Focus Today</div>
          <div className="text-2xl font-bold mt-1">{Math.round(todayFocusSec / 60)} min</div>
        </div>
        <div className="hw-panel p-4">
          <div className="text-hw-muted text-xs">Completion Rate</div>
          <div className="text-2xl font-bold mt-1">{completionRate}%</div>
        </div>
        <div className="hw-panel p-4">
          <div className="text-hw-muted text-xs">Total Focus</div>
          <div className="text-xl font-bold mt-1">{formatFocusMin(totalFocusSec)}</div>
        </div>
      </div>

      <div className="hw-panel p-4">
        <h3 className="text-sm font-semibold mb-3">Last 14 Days · Completed vs Interrupted</h3>
        <div className="relative h-48">
          <canvas ref={c1} />
        </div>
      </div>
      <div className="hw-panel p-4">
        <h3 className="text-sm font-semibold mb-3">Last 14 Days · Focus Time</h3>
        <div className="relative h-48">
          <canvas ref={c2} />
        </div>
      </div>

      <div className="hw-panel p-4">
        <h3 className="text-sm font-semibold mb-3">By Preset</h3>
        {presetKeys.length === 0 ? (
          <p className="text-hw-muted text-sm">No data</p>
        ) : (
          <div className="flex flex-col gap-2">
            {presetKeys.map((k) => {
              const p = byPreset[k]!;
              const rate = p.total > 0 ? Math.round((100 * p.completed) / p.total) : 0;
              return (
                <div
                  key={k}
                  className="flex flex-wrap justify-between gap-2 py-2 px-3 rounded-xl bg-hw-surface border border-hw-border"
                >
                  <span className="font-semibold text-sm">{p.text}</span>
                  <span className="text-hw-muted text-xs">
                    {p.completed} completed · {formatFocusMin(p.focusSec)} · {rate}%
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

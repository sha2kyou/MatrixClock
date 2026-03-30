import React, { useCallback, useEffect, useMemo, useState } from 'react';
import type { OpLogEntry } from '../matrix/types';
import { fetchOplogPage } from '../matrix/api';
import { useMatrixSession } from '../matrix/MatrixSessionContext';

const LABELS: Record<string, string> = {
  auth_bind: 'Authorization granted',
  auth_revoke: 'Authorization revoked',
  auth_reset: 'Authorization reset',
  auth_device_info: 'Device info updated',
  mode: 'Mode switched',
  clock_template: 'Clock template updated',
  display_text: 'Display text sent',
  pomodoro_configs: 'Pomodoro configs saved',
  keys_settings: 'Key mapping saved',
};

const PAGE_SIZE = 50;

function oplogType(action: string): 'auth' | 'control' | 'config' | 'other' {
  if (action.startsWith('auth_')) return 'auth';
  if (action === 'display_text' || action === 'mode' || action === 'clock_template')
    return 'control';
  if (action === 'pomodoro_configs' || action === 'keys_settings') return 'config';
  return 'other';
}

function formatTime(ms: number) {
  const d = new Date(ms);
  const now = new Date();
  return d.toDateString() === now.toDateString()
    ? d.toLocaleTimeString()
    : d.toLocaleString();
}

function parseDetailToKV(detail: string): { key: string; value: string }[] | null {
  if (!detail.includes('=')) return null;
  const parts = detail.split(/,(?=\w+=)/);
  const result: { key: string; value: string }[] = [];
  for (const part of parts) {
    const idx = part.indexOf('=');
    if (idx > 0) {
      result.push({ key: part.slice(0, idx).trim(), value: part.slice(idx + 1).trim() });
    }
  }
  return result.length > 0 ? result : null;
}

function typeLabel(type: ReturnType<typeof oplogType>) {
  if (type === 'auth') return 'AUTH';
  if (type === 'control') return 'CONTROL';
  if (type === 'config') return 'CONFIG';
  return 'OTHER';
}

export function OplogSection() {
  const { token, onForbidden } = useMatrixSession();
  const [items, setItems] = useState<OpLogEntry[]>([]);
  const [nextPageIdx, setNextPageIdx] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [filter, setFilter] = useState<'all' | 'auth' | 'control' | 'config'>('all');

  const refresh = useCallback(async () => {
    if (!token) return;
    try {
      const res = await fetchOplogPage(token, 0, PAGE_SIZE);
      setItems(res.items);
      setHasMore(res.hasMore);
      setNextPageIdx(res.hasMore ? 1 : 0);
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  }, [token, onForbidden]);

  useEffect(() => {
    if (token) refresh();
  }, [token, refresh]);

  const loadMore = useCallback(async () => {
    if (!token || !hasMore) return;
    try {
      const res = await fetchOplogPage(token, nextPageIdx, PAGE_SIZE);
      setItems((prev) => [...prev, ...res.items]);
      setHasMore(res.hasMore);
      if (res.hasMore) setNextPageIdx((p) => p + 1);
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  }, [token, hasMore, nextPageIdx, onForbidden]);

  const shown = useMemo(
    () =>
      filter === 'all' ? items : items.filter((e) => oplogType(e.action) === filter),
    [items, filter],
  );

  const stats = useMemo(() => {
    const today = new Date().toDateString();
    const todayCount = items.filter(
      (e) => new Date(e.timeMillis).toDateString() === today,
    ).length;
    return {
      loaded: items.length,
      todayCount,
      auth: items.filter((e) => oplogType(e.action) === 'auth').length,
      config: items.filter((e) => oplogType(e.action) === 'config').length,
    };
  }, [items]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-semibold">Operation Logs</h2>
          <p className="text-xs sm:text-sm text-hw-muted mt-1">
            Recent device operations with action, source, and details
          </p>
        </div>
        <div className="flex gap-2">
          <select
            className="hw-input text-xs py-2 max-w-[180px]"
            value={filter}
            onChange={(e) => setFilter(e.target.value as typeof filter)}
          >
            <option value="all">All</option>
            <option value="auth">Auth</option>
            <option value="control">Display/Control</option>
            <option value="config">Settings</option>
          </select>
          <button type="button" onClick={refresh} className="hw-button-secondary text-xs py-2 px-3">
            Refresh
          </button>
        </div>
      </div>

      <div className="flex flex-wrap gap-2 text-[11px]">
        <span className="px-2 py-1 rounded-full bg-hw-surface border border-hw-border">
          Loaded {stats.loaded}
        </span>
        <span className="px-2 py-1 rounded-full bg-hw-surface border border-hw-border">
          Today {stats.todayCount}
        </span>
        <span className="px-2 py-1 rounded-full bg-hw-surface border border-hw-border">
          Auth {stats.auth}
        </span>
        <span className="px-2 py-1 rounded-full bg-hw-surface border border-hw-border">
          Config {stats.config}
        </span>
      </div>

      <div className="grid gap-3">
        {shown.length === 0 ? (
          <div className="hw-panel p-12 text-center text-hw-muted">No records</div>
        ) : (
          shown.map((entry, i) => {
            const t = oplogType(entry.action);
            const rawDetail = (entry.detail || '').trim();
            const detailBlocks = rawDetail
              .split('\n')
              .map((x) => x.trim())
              .filter(Boolean);
            return (
              <div
                key={`${entry.timeMillis}-${i}`}
                className="hw-panel p-5"
              >
                <div className="flex justify-between gap-2 text-xs text-hw-muted mb-1">
                  <span
                    className={
                      t === 'auth'
                        ? 'text-blue-300'
                        : t === 'control'
                          ? 'text-emerald-300'
                          : t === 'config'
                            ? 'text-violet-300'
                            : ''
                    }
                  >
                    {typeLabel(t)}
                  </span>
                  <span>
                    {formatTime(entry.timeMillis)}
                    {entry.ip ? ` · ${entry.ip}` : ''}
                  </span>
                </div>
                <div className="font-semibold text-sm">
                  {LABELS[entry.action] || entry.action}
                </div>
                {detailBlocks.length > 0 && (
                  <div className="mt-1 text-xs text-hw-muted space-y-0.5">
                    {detailBlocks.map((line, lineIdx) => {
                      const kv = parseDetailToKV(line);
                      if (kv) {
                        return (
                          <div
                            key={`${lineIdx}-${line}`}
                            className="grid gap-x-3 gap-y-0.5 mt-1"
                            style={{ gridTemplateColumns: 'auto 1fr' }}
                          >
                            {kv.map(({ key, value }) => (
                              <React.Fragment key={key}>
                                <span className="text-hw-muted/70 select-none text-right">{key}</span>
                                <span className="text-hw-fg/80 break-all">{value}</span>
                              </React.Fragment>
                            ))}
                          </div>
                        );
                      }
                      return <div key={`${lineIdx}-${line}`}>{line}</div>;
                    })}
                  </div>
                )}
              </div>
            );
          })
        )}
      </div>

      {hasMore && (
        <button type="button" className="hw-button-secondary text-xs py-2 px-3" onClick={loadMore}>
          Load more
        </button>
      )}
    </div>
  );
}

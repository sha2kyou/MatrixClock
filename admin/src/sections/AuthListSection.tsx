import React, { useCallback, useEffect, useState } from 'react';
import { HardDrive, Trash2 } from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { AuthRecord } from '../matrix/types';
import { fetchAuthList, revokeAuthToken } from '../matrix/api';
import { useMatrixSession } from '../matrix/MatrixSessionContext';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function AuthListSection() {
  const { token, setToken, showToast, onForbidden } = useMatrixSession();
  const [list, setList] = useState<AuthRecord[]>([]);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const rows = await fetchAuthList(token);
      setList(rows);
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  }, [token, onForbidden]);

  useEffect(() => {
    load();
  }, [load]);

  const revoke = async (revokeToken: string) => {
    if (!token) return;
    if (!window.confirm('Revoke this device access? It must re-authorize afterward.')) return;
    try {
      await revokeAuthToken(token, revokeToken);
      showToast('Revoked');
      if (revokeToken === token) setToken(null);
      else load();
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
    }
  };

  const revokeOthers = async () => {
    if (!token) return;
    if (!window.confirm('Revoke all authorizations except this device?')) return;
    for (const r of list) {
      if (r.token !== token) {
        try {
          await revokeAuthToken(token, r.token);
        } catch {
          /* continue */
        }
      }
    }
    showToast('Done');
    load();
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-semibold">Authorized Devices</h2>
          <p className="text-xs sm:text-sm text-hw-muted mt-1">
            Name and battery are reported by each browser client
          </p>
        </div>
        <div className="flex gap-2">
          <button type="button" onClick={load} className="hw-button-secondary text-xs py-2 px-3">
            Refresh
          </button>
          {list.filter((r) => r.token !== token).length > 0 && (
            <button
              type="button"
              onClick={revokeOthers}
              className="text-xs font-bold uppercase tracking-wider text-red-400 border border-red-400/20 rounded-xl px-3 py-2 bg-red-400/5"
            >
              Revoke all others
            </button>
          )}
        </div>
      </div>

      {list.length === 0 ? (
        <div className="hw-panel p-12 text-center text-hw-muted">No records</div>
      ) : (
        <div className="grid gap-3">
          {list.map((rec) => {
            const isCurrent = rec.token === token;
            const name = rec.deviceName || rec.deviceModel || 'Device';
            const meta = [rec.deviceModel, rec.systemVersion, rec.batteryLevel != null && rec.batteryLevel >= 0 ? `${rec.batteryLevel}%` : null, rec.ip]
              .filter(Boolean)
              .join(' · ');
            return (
              <div key={rec.token} className="hw-panel p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-2xl bg-hw-surface flex items-center justify-center text-hw-muted shrink-0">
                    <HardDrive size={24} />
                  </div>
                  <div>
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-bold">{name}</span>
                      {isCurrent && (
                        <span className="text-[8px] font-black uppercase tracking-widest px-1.5 py-0.5 rounded border text-hw-accent border-hw-accent/30">
                          Current browser
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-hw-muted mt-1 break-all">{meta || '—'}</div>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => revoke(rec.token)}
                  className={cn(
                    'hw-button-secondary py-2 px-4 text-xs shrink-0',
                    'border-red-500/20 hover:bg-red-500/10 hover:text-red-400',
                  )}
                >
                  <Trash2 size={14} className="inline mr-1" />
                  Revoke
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

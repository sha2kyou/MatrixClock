import React, { useCallback, useEffect, useState } from 'react';
import { Battery, Cpu, Smartphone } from 'lucide-react';
import { fetchDeviceInfo } from '../matrix/api';
import { useMatrixSession } from '../matrix/MatrixSessionContext';

export function DeviceInfoSection() {
  const { token, onForbidden } = useMatrixSession();
  const [info, setInfo] = useState<Record<string, string> | null>(null);

  const load = useCallback(async () => {
    if (!token) return;
    try {
      const d = await fetchDeviceInfo(token);
      const statusMap: Record<string, string> = {
        charging: 'Charging',
        not_charging: 'Not charging',
        full: 'Full',
        discharging: 'Discharging',
        unknown: 'Unknown',
      };
      const statusLabel = statusMap[d.batteryStatus?.toLowerCase()] ?? d.batteryStatus;
      const batteryStr =
        d.batteryLevel >= 0 ? `${d.batteryLevel}% (${statusLabel})` : '—';
      setInfo({
        Model: d.model,
        Manufacturer: d.manufacturer,
        Device: d.device,
        Android: d.androidVersion,
        'SDK Level': String(d.sdkInt),
        Battery: batteryStr,
        Screen: d.screenWidthPx && d.screenHeightPx ? `${d.screenWidthPx} × ${d.screenHeightPx}` : '—',
        Density: d.screenDensity != null ? String(d.screenDensity) : '—',
      });
    } catch (e) {
      if (e instanceof Error && e.message === 'FORBIDDEN') onForbidden();
      setInfo(null);
    }
  }, [token, onForbidden]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center gap-4">
        <h2 className="text-xl sm:text-2xl font-semibold">Device Info</h2>
        <button type="button" onClick={load} className="hw-button-secondary text-xs py-2 px-3">
          Refresh
        </button>
      </div>
      <p className="text-xs text-hw-muted -mt-2">
        Data from the Android device running MatrixClock (not your browser)
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
        <div className="hw-panel p-4 flex items-center gap-3">
          <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400">
            <Smartphone size={20} />
          </div>
          <div>
            <div className="text-[10px] font-bold uppercase text-hw-muted">Host</div>
            <div className="text-sm font-bold">Android</div>
          </div>
        </div>
        <div className="hw-panel p-4 flex items-center gap-3">
          <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400">
            <Battery size={20} />
          </div>
          <div>
            <div className="text-[10px] font-bold uppercase text-hw-muted">Battery</div>
            <div className="text-sm font-bold">{info?.Battery ?? '—'}</div>
          </div>
        </div>
        <div className="hw-panel p-4 flex items-center gap-3">
          <div className="p-2 rounded-xl bg-violet-500/10 text-violet-400">
            <Cpu size={20} />
          </div>
          <div>
            <div className="text-[10px] font-bold uppercase text-hw-muted">SDK</div>
            <div className="text-sm font-bold">{info?.['SDK Level'] ?? '—'}</div>
          </div>
        </div>
      </div>

      <div className="hw-panel p-5">
        {!info ? (
          <p className="text-hw-muted text-sm">Failed to load or no data</p>
        ) : (
          Object.entries(info).map(([k, v]) => (
            <div
              key={k}
              className="flex justify-between gap-4 py-3 border-b border-hw-border last:border-0"
            >
              <span className="text-hw-muted text-sm">{k}</span>
              <span className="font-medium text-sm text-right break-all">{String(v)}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

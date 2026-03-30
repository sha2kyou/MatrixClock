import React, { useState } from 'react';
import { LayoutDashboard } from 'lucide-react';
import {
  bindAuth,
  postDeviceInfo,
} from '../matrix/api';
import { useMatrixSession } from '../matrix/MatrixSessionContext';

export function DeviceAuthorization() {
  const { setToken, showToast } = useMatrixSession();
  const [status, setStatus] = useState<'idle' | 'requesting' | 'error'>('idle');
  const [errMsg, setErrMsg] = useState('');

  const handleRequest = async () => {
    setStatus('requesting');
    setErrMsg('');
    try {
      const t = await bindAuth();
      if (t === 'DENIED' || t === null) {
        setErrMsg(t === 'DENIED' ? 'Authorization was denied by device' : 'Token was not received');
        setStatus('error');
        return;
      }
      setToken(t);
      showToast('Authorized');

      let batteryLevel: number | null = null;
      const getBattery = (
        navigator as Navigator & { getBattery?: () => Promise<{ level: number }> }
      ).getBattery;
      if (getBattery) {
        try {
          const b = await getBattery.call(navigator);
          batteryLevel = Math.round(b.level * 100);
        } catch {
          /* ignore */
        }
      }
      try {
        await postDeviceInfo(t, {
          deviceName: navigator.platform || '',
          deviceModel: navigator.userAgent.split(/[()]+/)[1] || navigator.userAgent.slice(0, 80),
          systemVersion: navigator.userAgent,
          batteryLevel,
        });
      } catch {
        /* optional */
      }
    } catch {
      setErrMsg('Connection failed. Ensure the device is on the same LAN and service is running.');
      setStatus('error');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-hw-bg p-4 relative overflow-hidden">
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-hw-accent/20 blur-[120px] rounded-full pointer-events-none" />

      <div className="hw-panel w-full max-w-[360px] p-6 sm:p-8 text-center relative z-10">
        <div className="flex flex-col items-center mb-6 sm:mb-8">
          <div className="w-16 h-16 sm:w-20 sm:h-20 bg-hw-accent rounded-[20px] sm:rounded-[24px] flex items-center justify-center mb-5 sm:mb-6 shadow-[0_8px_32px_rgba(10,132,255,0.3)]">
            <LayoutDashboard className="text-white w-8 h-8 sm:w-10 sm:h-10" />
          </div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight">MatrixClock</h1>
          <p className="text-hw-muted text-xs sm:text-sm mt-2 sm:mt-3 leading-relaxed px-2">
            {status === 'idle' && 'Confirm authorization on your phone to start managing the matrix display.'}
            {status === 'requesting' && 'Please confirm authorization in the dialog on the device screen...'}
            {status === 'error' && (errMsg || 'Please try again')}
          </p>
        </div>

        {status !== 'requesting' && (
          <button
            type="button"
            onClick={handleRequest}
            className="w-full hw-button py-3 sm:py-3.5 text-sm"
          >
            Request authorization and connect
          </button>
        )}

        {status === 'requesting' && (
          <div className="w-full py-3.5 flex flex-col items-center gap-4">
            <div className="w-6 h-6 border-2 border-hw-accent border-t-transparent rounded-full animate-spin" />
            <span className="text-xs font-semibold text-hw-muted uppercase tracking-widest">
              Waiting for device
            </span>
          </div>
        )}

        {status === 'error' && (
          <button
            type="button"
            onClick={() => setStatus('idle')}
            className="mt-4 w-full hw-button-secondary py-2.5 text-xs"
          >
            Retry
          </button>
        )}
      </div>
    </div>
  );
}

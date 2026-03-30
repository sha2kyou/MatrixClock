/**
 * MatrixClock web dashboard for NanoHTTPD API (build output goes to Android assets/web).
 */
import React, { useEffect, useState } from 'react';
import {
  ChevronDown,
  Clock,
  Cpu,
  Gamepad2,
  Hourglass,
  LayoutDashboard,

  Menu,
  Monitor as MonitorIcon,
  Moon,
  Pin,
  ScrollText,
  Settings as SettingsIcon,
  Sun,
  Timer,
  X,
} from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import { useMatrixSession } from './matrix/MatrixSessionContext';
import { DeviceAuthorization } from './sections/DeviceAuthorization';
import {
  ClockTemplateSection,
  CountdownSection,
  ResidentStatusSection,
} from './sections/ClockDisplaySection';
import { PomodoroSection } from './sections/PomodoroSection';
import { KeysSection } from './sections/KeysSection';
import { AuthListSection } from './sections/AuthListSection';
import { DeviceInfoSection } from './sections/DeviceInfoSection';
import { StatsSection } from './sections/StatsSection';
import { OplogSection } from './sections/OplogSection';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

type Tab =
  | 'clock-template'
  | 'countdown'
  | 'resident'
  | 'pomodoro'
  | 'stats'
  | 'keys'
  | 'device'
  | 'access'
  | 'oplog'
  | 'settings';

type ThemePref = 'light' | 'dark' | 'system';

interface MenuItem {
  id: Tab;
  icon: typeof Timer;
  label: string;
}

interface MenuGroup {
  id: string;
  icon: typeof Timer;
  label: string;
  children: MenuItem[];
}

function SettingsPanel({
  theme,
  setTheme,
}: {
  theme: ThemePref;
  setTheme: (t: ThemePref) => void;
}) {
  const options: { id: ThemePref; label: string; icon: typeof Sun }[] = [
    { id: 'light', label: 'Light', icon: Sun },
    { id: 'dark', label: 'Dark', icon: Moon },
    { id: 'system', label: 'System', icon: MonitorIcon },
  ];

  return (
    <div className="space-y-6">
      <h2 className="text-xl sm:text-2xl font-semibold">Settings</h2>
      <div className="hw-panel overflow-hidden">
        <div className="p-4 border-b border-hw-border bg-hw-surface">
          <h3 className="text-sm font-semibold">Appearance</h3>
        </div>
        <div className="p-4 grid grid-cols-3 gap-2">
          {options.map((o) => (
            <button
              key={o.id}
              type="button"
              onClick={() => setTheme(o.id)}
              className={cn(
                'flex flex-col items-center gap-2 p-4 rounded-2xl border transition-all',
                theme === o.id
                  ? 'bg-hw-accent/10 border-hw-accent text-hw-accent'
                  : 'bg-hw-surface border-hw-border text-hw-muted hover:text-hw-text',
              )}
            >
              <o.icon size={20} />
              <span className="text-[10px] font-bold uppercase">{o.label}</span>
            </button>
          ))}
        </div>
      </div>
      <p className="text-[11px] text-hw-muted leading-relaxed">
        Dashboard and device must be on the same LAN. Example:{' '}
        <code className="text-hw-accent">http://device-ip:6574</code>
      </p>
    </div>
  );
}

function MatrixShell() {
  const { token, toast } = useMatrixSession();
  const [activeTab, setActiveTab] = useState<Tab>(() => {
    const hash = window.location.hash.replace('#', '') as Tab;
    const valid: Tab[] = [
      'clock-template', 'countdown', 'resident', 'pomodoro',
      'stats', 'keys', 'device', 'access', 'oplog', 'settings',
    ];
    return valid.includes(hash) ? hash : 'clock-template';
  });
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(() => {
    const saved = localStorage.getItem('mc_expanded');
    if (saved) {
      try {
        const arr = JSON.parse(saved) as string[];
        if (Array.isArray(arr)) return new Set(arr);
      } catch { /* ignore */ }
    }
    return new Set(['display']);
  });
  const [theme, setTheme] = useState<ThemePref>(() => {
    if (typeof window === 'undefined') return 'system';
    return (localStorage.getItem('mc_theme') as ThemePref) || 'system';
  });

  useEffect(() => {
    const root = document.documentElement;
    const apply = (t: ThemePref) => {
      if (t === 'system') {
        const dark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        root.classList.toggle('light', !dark);
      } else {
        root.classList.toggle('light', t === 'light');
      }
    };
    apply(theme);
    localStorage.setItem('mc_theme', theme);
    if (theme === 'system') {
      const mq = window.matchMedia('(prefers-color-scheme: dark)');
      const fn = () => apply('system');
      mq.addEventListener('change', fn);
      return () => mq.removeEventListener('change', fn);
    }
    return undefined;
  }, [theme]);

  if (!token) {
    return <DeviceAuthorization />;
  }

  const menu: MenuGroup[] = [
    {
      id: 'display',
      icon: Clock,
      label: 'Display',
      children: [
        { id: 'clock-template', icon: Clock, label: 'Clock Templates' },
        { id: 'countdown', icon: Hourglass, label: 'Countdown' },
        { id: 'resident', icon: Pin, label: 'Resident Status' },
      ],
    },
    {
      id: 'pomodoro',
      icon: Timer,
      label: 'Pomodoro',
      children: [
        { id: 'pomodoro', icon: Timer, label: 'Pomodoro Presets' },
        { id: 'stats', icon: LayoutDashboard, label: 'Pomodoro Stats' },
      ],
    },
    {
      id: 'device',
      icon: Cpu,
      label: 'Device',
      children: [
        { id: 'keys', icon: Gamepad2, label: 'Key Mapping' },
        { id: 'device', icon: Cpu, label: 'Device Info' },
      ],
    },
    {
      id: 'system',
      icon: SettingsIcon,
      label: 'System',
      children: [
        { id: 'access', icon: LayoutDashboard, label: 'Authorized Devices' },
        { id: 'oplog', icon: ScrollText, label: 'Operation Logs' },
        { id: 'settings', icon: SettingsIcon, label: 'Settings' },
      ],
    },
  ];

  const toggleGroup = (groupId: string) => {
    setExpandedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(groupId)) {
        next.delete(groupId);
      } else {
        next.add(groupId);
      }
      localStorage.setItem('mc_expanded', JSON.stringify([...next]));
      return next;
    });
  };

  const isChildActive = (group: MenuGroup) =>
    group.children.some((child) => child.id === activeTab);

  const handleTabClick = (tab: Tab) => {
    setActiveTab(tab);
    window.location.hash = tab;
    setSidebarOpen(false);
  };

  const renderMenuItem = (item: MenuItem) => (
    <button
      key={item.id}
      type="button"
      onClick={() => handleTabClick(item.id)}
      className={cn(
        'w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-sm font-semibold transition-all',
        activeTab === item.id
          ? 'bg-hw-accent text-white shadow-lg shadow-hw-accent/20'
          : 'text-hw-muted hover:bg-hw-surface hover:text-hw-text',
      )}
    >
      <item.icon size={18} />
      {item.label}
    </button>
  );

  const renderMenuGroup = (group: MenuGroup) => {
    const expanded = expandedGroups.has(group.id);
    const childActive = isChildActive(group);

    return (
      <div key={group.id}>
        <button
          type="button"
          onClick={() => toggleGroup(group.id)}
          className={cn(
            'w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-semibold transition-all',
            childActive
              ? 'text-hw-accent bg-hw-accent/5'
              : 'text-hw-muted hover:bg-hw-surface hover:text-hw-text',
          )}
        >
          <group.icon size={20} />
          <span className="flex-1 text-left">{group.label}</span>
          <ChevronDown
            size={16}
            className={cn(
              'transition-transform duration-200',
              expanded ? 'rotate-180' : '',
            )}
          />
        </button>
        <div
          className={cn(
            'overflow-hidden transition-all duration-200',
            expanded ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0',
          )}
        >
          <div className="ml-4 pl-3 border-l border-hw-border/50 mt-1 space-y-0.5">
            {group.children.map(renderMenuItem)}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="flex min-h-screen bg-hw-bg text-hw-text font-sans relative overflow-x-hidden">
      {toast && (
        <div className="fixed bottom-6 left-1/2 -translate-x-1/2 z-[100] px-4 py-2 rounded-xl bg-emerald-600 text-white text-sm shadow-lg">
          {toast}
        </div>
      )}

      <div className="lg:hidden fixed top-0 left-0 right-0 h-16 bg-hw-bg/80 backdrop-blur-xl border-b border-hw-border z-40 flex items-center justify-between px-4">
        <span className="font-bold text-lg">MatrixClock</span>
        <button
          type="button"
          onClick={() => setSidebarOpen(true)}
          className="w-10 h-10 rounded-xl bg-hw-surface flex items-center justify-center text-hw-muted"
        >
          <Menu size={20} />
        </button>
      </div>

      {sidebarOpen && (
        <button
          type="button"
          className="lg:hidden fixed inset-0 bg-black/60 z-50 border-0 p-0 cursor-pointer"
          aria-label="Close menu"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-[60] w-72 bg-hw-bg border-r border-hw-border flex flex-col p-6 transition-transform duration-300 lg:translate-x-0 lg:static',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex items-center justify-between mb-8 px-1">
          <div className="font-bold text-xl tracking-tight">MatrixClock</div>
          <button
            type="button"
            className="lg:hidden w-8 h-8 rounded-full bg-hw-surface flex items-center justify-center"
            onClick={() => setSidebarOpen(false)}
          >
            <X size={18} />
          </button>
        </div>
        <nav className="flex-1 space-y-1">
          {menu.map((entry) => renderMenuGroup(entry))}
        </nav>
      </aside>

      <main className="flex-1 p-4 sm:p-8 pt-20 lg:pt-8 overflow-y-auto">
        <div className="max-w-5xl mx-auto">
          {activeTab === 'clock-template' && <ClockTemplateSection />}
          {activeTab === 'countdown' && <CountdownSection />}
          {activeTab === 'resident' && <ResidentStatusSection />}
          {activeTab === 'pomodoro' && <PomodoroSection />}
          {activeTab === 'stats' && <StatsSection />}
          {activeTab === 'keys' && <KeysSection />}
          {activeTab === 'device' && <DeviceInfoSection />}
          {activeTab === 'access' && <AuthListSection />}
          {activeTab === 'oplog' && <OplogSection />}
          {activeTab === 'settings' && <SettingsPanel theme={theme} setTheme={setTheme} />}
        </div>
      </main>
    </div>
  );
}

export default function App() {
  return <MatrixShell />;
}

import { Users, Map, Shield, Activity, TrendingUp, AlertTriangle, UserCheck } from 'lucide-react';
import { adminApi } from '@/api/admin';
import { useAsync } from '@/hooks/useAsync';
import { StatCard } from '@/components/StatCard';

function fmtArea(m2: number): string {
  if (m2 >= 1_000_000) return `${(m2 / 1_000_000).toFixed(2)} km²`;
  if (m2 >= 10_000) return `${(m2 / 10_000).toFixed(1)} ha`;
  return `${Math.round(m2)} m²`;
}

function MiniBarChart({ data, color }: { data: { date: string; count: number }[]; color: string }) {
  const max = Math.max(...data.map(d => d.count), 1);
  return (
    <div className="flex items-end gap-1 h-12 mt-2">
      {data.map(d => (
        <div key={d.date} className="flex-1 flex flex-col items-center gap-0.5">
          <div
            className={`w-full rounded-sm ${color}`}
            style={{ height: `${Math.max((d.count / max) * 44, 2)}px` }}
            title={`${d.date}: ${d.count}`}
          />
          <span className="text-[9px] text-gray-600">{d.date.slice(5)}</span>
        </div>
      ))}
    </div>
  );
}

export function Dashboard() {
  const { data, loading, error } = useAsync(() => adminApi.getStats().then(r => r.data), []);

  if (loading) return <div className="text-gray-400 text-sm">Loading stats...</div>;
  if (error) return <div className="text-red-400 text-sm">Error: {error}</div>;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold">Dashboard</h1>

      {/* Основные метрики */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Аккаунтов" value={data.users_count.toLocaleString()} icon={Users} color="text-blue-400" />
        <StatCard label="Активны сегодня" value={data.dau_today?.toLocaleString() ?? '—'} icon={UserCheck} color="text-emerald-400" />
        <StatCard label="Ошибок сегодня" value={data.errors_today?.toLocaleString() ?? '0'} icon={AlertTriangle} color="text-red-400" />
        <StatCard label="Территорий" value={data.territories_count.toLocaleString()} icon={Map} color="text-orange-400" />
        <StatCard label="Кланов" value={data.clans_count.toLocaleString()} icon={Shield} color="text-purple-400" />
        <StatCard label="Активных сессий" value={data.active_sessions.toLocaleString()} icon={Activity} color="text-yellow-400" />
      </div>

      {/* Захваченная площадь */}
      <div className="card">
        <p className="text-xs text-gray-500 uppercase tracking-wider font-medium mb-1">Общая захваченная площадь</p>
        <p className="text-3xl font-bold text-emerald-400">{fmtArea(data.total_area_m2)}</p>
      </div>

      {/* 7-дневные графики */}
      {(data.dau_7days || data.errors_7days) && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {data.dau_7days && (
            <div className="card">
              <div className="flex items-center gap-2 mb-1">
                <UserCheck size={14} className="text-emerald-400" />
                <h2 className="font-semibold text-sm">Активных пользователей (7 дней)</h2>
              </div>
              <MiniBarChart data={data.dau_7days} color="bg-emerald-500" />
            </div>
          )}
          {data.errors_7days && (
            <div className="card">
              <div className="flex items-center gap-2 mb-1">
                <AlertTriangle size={14} className="text-red-400" />
                <h2 className="font-semibold text-sm">Ошибок сервера (7 дней)</h2>
              </div>
              <MiniBarChart data={data.errors_7days} color="bg-red-500" />
            </div>
          )}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Top Users */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp size={16} className="text-blue-400" />
            <h2 className="font-semibold text-sm">Топ игроков по площади</h2>
          </div>
          <div className="space-y-2">
            {data.top_users.map((u: any, i: number) => (
              <div key={u.id} className="flex items-center gap-3 py-1.5">
                <span className="text-xs text-gray-500 w-4">{i + 1}</span>
                <span className="w-3 h-3 rounded-full shrink-0" style={{ backgroundColor: u.color }} />
                <span className="text-sm font-medium flex-1 truncate">{u.username}</span>
                <span className="text-xs text-gray-400">{fmtArea(u.total_area_m2)}</span>
                <span className="text-xs text-gray-600">{u.territories_count} terr.</span>
              </div>
            ))}
          </div>
        </div>

        {/* Top Clans */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp size={16} className="text-purple-400" />
            <h2 className="font-semibold text-sm">Топ кланов по площади</h2>
          </div>
          <div className="space-y-2">
            {data.top_clans.map((c: any, i: number) => (
              <div key={c.id} className="flex items-center gap-3 py-1.5">
                <span className="text-xs text-gray-500 w-4">{i + 1}</span>
                <span className="w-3 h-3 rounded-full shrink-0" style={{ backgroundColor: c.color }} />
                <span className="text-sm font-medium flex-1 truncate">{c.name}</span>
                <span className="badge bg-gray-800 text-gray-400 mr-1">[{c.tag}]</span>
                <span className="text-xs text-gray-400">{fmtArea(c.total_area_m2)}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

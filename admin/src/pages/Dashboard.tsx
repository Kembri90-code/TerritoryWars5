import { Users, Map, Shield, Activity, TrendingUp } from 'lucide-react';
import { adminApi } from '@/api/admin';
import { useAsync } from '@/hooks/useAsync';
import { StatCard } from '@/components/StatCard';

function fmtArea(m2: number): string {
  if (m2 >= 1_000_000) return `${(m2 / 1_000_000).toFixed(2)} km²`;
  if (m2 >= 10_000) return `${(m2 / 10_000).toFixed(1)} ha`;
  return `${Math.round(m2)} m²`;
}

export function Dashboard() {
  const { data, loading, error } = useAsync(() => adminApi.getStats().then(r => r.data), []);

  if (loading) return <div className="text-gray-400 text-sm">Loading stats...</div>;
  if (error) return <div className="text-red-400 text-sm">Error: {error}</div>;
  if (!data) return null;

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold">Dashboard</h1>

      {/* Stats Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total Users" value={data.users_count.toLocaleString()} icon={Users} color="text-blue-400" />
        <StatCard label="Territories" value={data.territories_count.toLocaleString()} icon={Map} color="text-emerald-400" />
        <StatCard label="Clans" value={data.clans_count.toLocaleString()} icon={Shield} color="text-purple-400" />
        <StatCard label="Active Sessions" value={data.active_sessions.toLocaleString()} icon={Activity} color="text-orange-400" />
      </div>

      <div className="card">
        <p className="text-xs text-gray-500 uppercase tracking-wider font-medium mb-1">Total Captured Area</p>
        <p className="text-3xl font-bold text-emerald-400">{fmtArea(data.total_area_m2)}</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Top Users */}
        <div className="card">
          <div className="flex items-center gap-2 mb-4">
            <TrendingUp size={16} className="text-blue-400" />
            <h2 className="font-semibold text-sm">Top Users by Area</h2>
          </div>
          <div className="space-y-2">
            {data.top_users.map((u, i) => (
              <div key={u.id} className="flex items-center gap-3 py-1.5">
                <span className="text-xs text-gray-500 w-4">{i + 1}</span>
                <span
                  className="w-3 h-3 rounded-full shrink-0"
                  style={{ backgroundColor: u.color }}
                />
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
            <h2 className="font-semibold text-sm">Top Clans by Area</h2>
          </div>
          <div className="space-y-2">
            {data.top_clans.map((c, i) => (
              <div key={c.id} className="flex items-center gap-3 py-1.5">
                <span className="text-xs text-gray-500 w-4">{i + 1}</span>
                <span
                  className="w-3 h-3 rounded-full shrink-0"
                  style={{ backgroundColor: c.color }}
                />
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

import { useState, useCallback } from 'react';
import { Trash2, RefreshCw } from 'lucide-react';
import { adminApi, Territory } from '@/api/admin';
import { useAsync } from '@/hooks/useAsync';
import { Pagination } from '@/components/Pagination';
import { ConfirmDialog } from '@/components/ConfirmDialog';

function fmtArea(m2: number): string {
  if (m2 >= 1_000_000) return `${(m2 / 1_000_000).toFixed(2)} km²`;
  if (m2 >= 10_000) return `${(m2 / 10_000).toFixed(1)} ha`;
  return `${Math.round(m2)} m²`;
}

export function TerritoriesPage() {
  const [page, setPage] = useState(1);
  const [confirmDelete, setConfirmDelete] = useState<Territory | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [actionMsg, setActionMsg] = useState('');

  const fetchTerritories = useCallback(
    () => adminApi.getTerritories({ page, limit: 20 }).then(r => r.data),
    [page]
  );
  const { data, loading, error, reload } = useAsync(fetchTerritories, [page]);

  async function deleteTerritory() {
    if (!confirmDelete) return;
    setDeleting(true);
    try {
      await adminApi.deleteTerritory(confirmDelete.id);
      setConfirmDelete(null);
      setActionMsg(`Territory deleted.`);
      reload();
    } catch {
      setActionMsg('Failed to delete territory.');
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Territories</h1>
        <button onClick={reload} className="btn-ghost text-xs gap-1.5">
          <RefreshCw size={13} /> Refresh
        </button>
      </div>

      {actionMsg && (
        <div className="text-sm bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-200">
          {actionMsg}
        </div>
      )}

      <div className="card p-0 overflow-hidden">
        {loading && <div className="p-6 text-sm text-gray-400">Loading...</div>}
        {error && <div className="p-6 text-sm text-red-400">Error: {error}</div>}
        {data && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-800 text-left text-xs text-gray-500 uppercase tracking-wider">
                    <th className="px-4 py-3 font-medium">Owner</th>
                    <th className="px-4 py-3 font-medium">Clan</th>
                    <th className="px-4 py-3 font-medium">Area</th>
                    <th className="px-4 py-3 font-medium">Perimeter</th>
                    <th className="px-4 py-3 font-medium">Captured</th>
                    <th className="px-4 py-3 font-medium">ID</th>
                    <th className="px-4 py-3 font-medium"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-800">
                  {data.territories.map(t => (
                    <tr key={t.id} className="hover:bg-gray-800/50 transition-colors">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: t.owner_color }} />
                          <span className="font-medium">{t.owner_username}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        {t.clan_name ? (
                          <span className="badge bg-purple-900/40 text-purple-300">[{t.clan_tag}] {t.clan_name}</span>
                        ) : (
                          <span className="text-gray-600">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-emerald-400 font-medium">{fmtArea(t.area_m2)}</td>
                      <td className="px-4 py-3 text-gray-400">{Math.round(t.perimeter_m)} m</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">
                        {new Date(t.captured_at).toLocaleString()}
                      </td>
                      <td className="px-4 py-3 text-gray-600 text-xs font-mono">
                        {t.id.slice(0, 8)}…
                      </td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => setConfirmDelete(t)}
                          className="p-1.5 rounded-lg hover:bg-gray-700 text-gray-400 hover:text-red-400 transition-colors"
                          title="Delete territory"
                        >
                          <Trash2 size={13} />
                        </button>
                      </td>
                    </tr>
                  ))}
                  {data.territories.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-4 py-8 text-center text-gray-500 text-sm">No territories found</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            <div className="px-4 py-3 border-t border-gray-800">
              <Pagination {...data.pagination} onChange={p => setPage(p)} />
            </div>
          </>
        )}
      </div>

      {confirmDelete && (
        <ConfirmDialog
          title="Delete Territory"
          message={`Delete this territory (${fmtArea(confirmDelete.area_m2)}) owned by "${confirmDelete.owner_username}"?`}
          onConfirm={deleteTerritory}
          onCancel={() => setConfirmDelete(null)}
          loading={deleting}
        />
      )}
    </div>
  );
}

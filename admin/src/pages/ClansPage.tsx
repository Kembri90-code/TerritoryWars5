import { useState, useCallback } from 'react';
import { Search, Trash2, RefreshCw } from 'lucide-react';
import { adminApi, Clan } from '@/api/admin';
import { useAsync } from '@/hooks/useAsync';
import { Pagination } from '@/components/Pagination';
import { ConfirmDialog } from '@/components/ConfirmDialog';

function fmtArea(m2: number): string {
  if (m2 >= 1_000_000) return `${(m2 / 1_000_000).toFixed(2)} km²`;
  if (m2 >= 10_000) return `${(m2 / 10_000).toFixed(1)} ha`;
  return `${Math.round(m2)} m²`;
}

export function ClansPage() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [confirmDelete, setConfirmDelete] = useState<Clan | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [actionMsg, setActionMsg] = useState('');

  const fetchClans = useCallback(
    () => adminApi.getClans({ page, limit: 20, search }).then(r => r.data),
    [page, search]
  );
  const { data, loading, error, reload } = useAsync(fetchClans, [page, search]);

  function applySearch() {
    setPage(1);
    setSearch(searchInput);
  }

  async function deleteClan() {
    if (!confirmDelete) return;
    setDeleting(true);
    try {
      await adminApi.deleteClan(confirmDelete.id);
      setConfirmDelete(null);
      setActionMsg(`Clan "${confirmDelete.name}" deleted.`);
      reload();
    } catch {
      setActionMsg('Failed to delete clan.');
    } finally {
      setDeleting(false);
    }
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold">Clans</h1>
        <button onClick={reload} className="btn-ghost text-xs gap-1.5">
          <RefreshCw size={13} /> Refresh
        </button>
      </div>

      {actionMsg && (
        <div className="text-sm bg-gray-800 border border-gray-700 rounded-lg px-4 py-2 text-gray-200">
          {actionMsg}
        </div>
      )}

      <div className="flex gap-2">
        <div className="relative flex-1 max-w-xs">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
          <input
            type="text"
            className="input pl-8"
            placeholder="Search name or tag..."
            value={searchInput}
            onChange={e => setSearchInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && applySearch()}
          />
        </div>
        <button onClick={applySearch} className="btn-primary text-xs">Search</button>
      </div>

      <div className="card p-0 overflow-hidden">
        {loading && <div className="p-6 text-sm text-gray-400">Loading...</div>}
        {error && <div className="p-6 text-sm text-red-400">Error: {error}</div>}
        {data && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-gray-800 text-left text-xs text-gray-500 uppercase tracking-wider">
                    <th className="px-4 py-3 font-medium">Clan</th>
                    <th className="px-4 py-3 font-medium">Leader</th>
                    <th className="px-4 py-3 font-medium">Members</th>
                    <th className="px-4 py-3 font-medium">Area</th>
                    <th className="px-4 py-3 font-medium">Territories</th>
                    <th className="px-4 py-3 font-medium">Created</th>
                    <th className="px-4 py-3 font-medium"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-800">
                  {data.clans.map(c => (
                    <tr key={c.id} className="hover:bg-gray-800/50 transition-colors">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ backgroundColor: c.color }} />
                          <span className="font-medium">{c.name}</span>
                          <span className="badge bg-gray-800 text-gray-400">[{c.tag}]</span>
                        </div>
                        {c.description && (
                          <div className="text-xs text-gray-500 mt-0.5 ml-4 truncate max-w-xs">{c.description}</div>
                        )}
                      </td>
                      <td className="px-4 py-3 text-gray-400">{c.leader_username}</td>
                      <td className="px-4 py-3 text-gray-300">
                        {c.members_count} / {c.max_members}
                      </td>
                      <td className="px-4 py-3 text-gray-300">{fmtArea(c.total_area_m2)}</td>
                      <td className="px-4 py-3 text-gray-400">{c.territories_count}</td>
                      <td className="px-4 py-3 text-gray-500 text-xs">
                        {new Date(c.created_at).toLocaleDateString()}
                      </td>
                      <td className="px-4 py-3">
                        <button
                          onClick={() => setConfirmDelete(c)}
                          className="p-1.5 rounded-lg hover:bg-gray-700 text-gray-400 hover:text-red-400 transition-colors"
                          title="Delete clan"
                        >
                          <Trash2 size={13} />
                        </button>
                      </td>
                    </tr>
                  ))}
                  {data.clans.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-4 py-8 text-center text-gray-500 text-sm">No clans found</td>
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
          title="Delete Clan"
          message={`Delete clan "${confirmDelete.name}"? All members will be removed from the clan.`}
          onConfirm={deleteClan}
          onCancel={() => setConfirmDelete(null)}
          loading={deleting}
        />
      )}
    </div>
  );
}

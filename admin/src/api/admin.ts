import { api } from './client';

export interface DayCount { date: string; count: number; }

export interface Stats {
  users_count: number;
  territories_count: number;
  clans_count: number;
  active_sessions: number;
  total_area_m2: number;
  dau_today: number;
  errors_today: number;
  dau_7days: DayCount[];
  errors_7days: DayCount[];
  top_users: { id: string; username: string; total_area_m2: number; territories_count: number; color: string }[];
  top_clans: { id: string; name: string; tag: string; total_area_m2: number; territories_count: number; color: string }[];
}

export interface User {
  id: string;
  username: string;
  email: string;
  color: string;
  avatar_url: string | null;
  total_area_m2: number;
  territories_count: number;
  captures_count: number;
  created_at: string;
  city: string | null;
  clan: { id: string; name: string; tag: string } | null;
}

export interface Clan {
  id: string;
  name: string;
  tag: string;
  color: string;
  description: string | null;
  leader_username: string;
  members_count: number;
  max_members: number;
  total_area_m2: number;
  territories_count: number;
  created_at: string;
}

export interface Territory {
  id: string;
  owner_id: string;
  owner_username: string;
  owner_color: string;
  clan_id: string | null;
  clan_name: string | null;
  clan_tag: string | null;
  area_m2: number;
  perimeter_m: number;
  captured_at: string;
}

export interface Pagination {
  page: number;
  limit: number;
  total: number;
  pages: number;
}

export const adminApi = {
  login: (username: string, password: string) =>
    api.post<{ token: string; admin: { id: string; username: string } }>('/admin/login', { username, password }),

  getStats: () => api.get<Stats>('/admin/stats'),

  getUsers: (params: { page?: number; limit?: number; search?: string }) =>
    api.get<{ users: User[]; pagination: Pagination }>('/admin/users', { params }),

  deleteUser: (id: string) => api.delete(`/admin/users/${id}`),

  resetUserSessions: (id: string) => api.post(`/admin/users/${id}/reset-sessions`),

  getClans: (params: { page?: number; limit?: number; search?: string }) =>
    api.get<{ clans: Clan[]; pagination: Pagination }>('/admin/clans', { params }),

  deleteClan: (id: string) => api.delete(`/admin/clans/${id}`),

  getTerritories: (params: { page?: number; limit?: number }) =>
    api.get<{ territories: Territory[]; pagination: Pagination }>('/admin/territories', { params }),

  deleteTerritory: (id: string) => api.delete(`/admin/territories/${id}`),
};

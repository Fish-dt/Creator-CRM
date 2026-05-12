import { createClient } from "./supabase/client";

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

async function getAuthHeader(): Promise<HeadersInit> {
  const supabase = createClient();
  const { data } = await supabase.auth.getSession();
  const token = data.session?.access_token;
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const authHeaders = await getAuthHeader();
  const res = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...authHeaders,
      ...init?.headers,
    },
  });
  if (!res.ok) {
    const err = await res.text();
    throw new Error(`API error ${res.status}: ${err}`);
  }
  return res.json() as Promise<T>;
}

// ── Deal API ──────────────────────────────────────────────────────────────────

export type DealStatus =
  | "DRAFT" | "NEGOTIATING" | "ACTIVE"
  | "DELIVERED" | "INVOICED" | "COMPLETE" | "CANCELLED";

export interface Deal {
  id:                string;
  creatorId:         string;
  brandName:         string;
  title:             string;
  description?:      string;
  value:             number;
  currency:          string;
  status:            DealStatus;
  campaignStartDate?: string;
  campaignEndDate?:  string;
  createdAt:         string;
  updatedAt:         string;
}

export interface CreateDealInput {
  creatorId:        string;
  brandName:        string;
  title:            string;
  description?:     string;
  value:            number;
  currency?:        string;
  campaignStartDate?: string;
  campaignEndDate?: string;
}

export const dealsApi = {
  list:       (creatorId: string) =>
    apiFetch<Deal[]>(`/api/v1/deals?creatorId=${creatorId}`),
  get:        (id: string)        => apiFetch<Deal>(`/api/v1/deals/${id}`),
  create:     (data: CreateDealInput) =>
    apiFetch<Deal>("/api/v1/deals", { method: "POST", body: JSON.stringify(data) }),
  transition: (id: string, status: DealStatus) =>
    apiFetch<Deal>(`/api/v1/deals/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    }),
  earnings:   (creatorId: string) =>
    apiFetch<{ total: number }>(`/api/v1/deals/earnings?creatorId=${creatorId}`),
};

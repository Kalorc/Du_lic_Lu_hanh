/**
 * Lop goi API duy nhat cho toan bo giao dien.
 * Tang giao dien (React) -> Auth(JWT) -> Controller -> Service -> Repository -> DB.
 */

const BASE = import.meta.env.VITE_API_BASE ?? '/api/v1';
const TOKEN_KEY = 'tour_discovery_token';
const USER_KEY = 'tour_discovery_user';

export const auth = {
  getToken: () => localStorage.getItem(TOKEN_KEY),
  getUser: () => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  save: (token, user) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

/** Loi API co mang theo ma loi cua backend (INVALID_BUDGET, FORBIDDEN, ...). */
export class ApiError extends Error {
  constructor(code, message, status, details) {
    super(message || code);
    this.code = code;
    this.status = status;
    this.details = details;
  }
}

async function request(path, { method = 'GET', body, authenticated = false } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (authenticated) {
    const token = auth.getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;
  }

  let res;
  try {
    res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (networkError) {
    throw new ApiError(
      'NETWORK_ERROR',
      'Không kết nối được tới server. Hãy kiểm tra backend đã chạy ở cổng 8080 chưa.',
      0,
    );
  }

  const text = await res.text();
  const payload = text ? JSON.parse(text) : {};

  if (!res.ok || payload.success === false) {
    const err = payload.error ?? {};
    throw new ApiError(err.code ?? 'INTERNAL_ERROR', err.message, res.status, err.details);
  }

  return payload;
}

function toQuery(params) {
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    if (Array.isArray(value)) {
      value.forEach((v) => sp.append(key, v));
    } else {
      sp.append(key, value);
    }
  });
  const q = sp.toString();
  return q ? `?${q}` : '';
}

export const api = {
  // --- API 1, 2: danh muc cho bo loc
  listInterests: () => request('/interests').then((r) => r.data),
  listDestinations: (type) => request(`/destinations${toQuery({ type })}`).then((r) => r.data),

  // --- API 3: tim kiem va loc
  searchTours: (params) => request(`/tours${toQuery(params)}`),

  // --- API 4: goi y va xep hang theo so thich
  recommendTours: (params) => request(`/tour-recommendations${toQuery(params)}`),

  // --- API 5: chi tiet tour cong khai
  getTour: (tourId) => request(`/tours/${tourId}`).then((r) => r.data),

  // --- Dang nhap
  login: (username, password) =>
    request('/auth/login', { method: 'POST', body: { username, password } }).then((r) => r.data),

  // --- API 6, 7: so thich cua phien ban Tour (noi bo)
  getVersionInterests: (versionId) =>
    request(`/internal/tour-versions/${versionId}/interests`, { authenticated: true })
      .then((r) => r.data),
  replaceVersionInterests: (versionId, interestCodes) =>
    request(`/internal/tour-versions/${versionId}/interests`, {
      method: 'PUT',
      body: { interestCodes },
      authenticated: true,
    }).then((r) => r.data),

  // --- API 8, 9: danh muc so thich (noi bo)
  listAllInterests: () =>
    request('/internal/interests', { authenticated: true }).then((r) => r.data),
  createInterest: (payload) =>
    request('/internal/interests', { method: 'POST', body: payload, authenticated: true })
      .then((r) => r.data),
  updateInterest: (interestId, payload) =>
    request(`/internal/interests/${interestId}`, {
      method: 'PATCH',
      body: payload,
      authenticated: true,
    }).then((r) => r.data),
};

export function formatVnd(value) {
  if (value === null || value === undefined) return '—';
  return new Intl.NumberFormat('vi-VN').format(Number(value)) + ' ₫';
}

export function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
}

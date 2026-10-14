import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if ([401, 403].includes(error.response?.status)) {
      localStorage.removeItem('admin_token');
      localStorage.removeItem('admin_name');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export const authAPI = {
  login: (email, password) => api.post('/auth/admin/login', { email, password }),
};

export const profileAPI = {
  get: () => api.get('/admin/profile'),
  update: (data) => api.put('/admin/profile', data),
};

export const dashboardAPI = {
  getStats: () => api.get('/admin/dashboard'),
};

export const adminAPI = {
  getAll: () => api.get('/admin/manage'),
  create: (data) => api.post('/admin/manage', data),
  update: (id, data) => api.put(`/admin/manage/${id}`, data),
  delete: (id) => api.delete(`/admin/manage/${id}`),
};

export const busAPI = {
  getAll: () => api.get('/admin/buses'),
  getPublic: () => api.get('/buses'),
  getById: (id) => api.get(`/buses/${id}`),
  create: (data) => api.post('/admin/buses', data),
  update: (id, data) => api.put(`/admin/buses/${id}`, data),
  delete: (id) => api.delete(`/admin/buses/${id}`),
  toggle: (id) => api.patch(`/admin/buses/${id}/toggle`),
  updateTrackerLink: (busId, data) => api.put(`/admin/buses/${busId}/tracker-link`, data),
};

export const scheduleAPI = {
  getAll: () => api.get('/admin/schedules'),
  create: (data) => api.post('/admin/schedules', data),
  update: (id, data) => api.put(`/admin/schedules/${id}`, data),
  delete: (id) => api.delete(`/admin/schedules/${id}`),
  toggle: (id) => api.patch(`/admin/schedules/${id}/toggle`),
};

export const noticeAPI = {
  getAll: () => api.get('/admin/notices'),
  create: (data) => api.post('/admin/notices', data),
  delete: (id) => api.delete(`/admin/notices/${id}`),
};

export const studentAPI = {
  getAll: () => api.get('/admin/students'),
  getPending: () => api.get('/admin/students/pending'),
  verify: (id) => api.put(`/admin/students/${id}/verify`),
  toggleActive: (id) => api.put(`/admin/students/${id}/toggle-active`),
  delete: (id) => api.delete(`/admin/students/${id}`),
};

export const teacherAPI = {
  getAll: () => api.get('/admin/teachers'),
  getPending: () => api.get('/admin/teachers/pending'),
  verify: (id) => api.put(`/admin/teachers/${id}/verify`),
  toggleActive: (id) => api.put(`/admin/teachers/${id}/toggle-active`),
  delete: (id) => api.delete(`/admin/teachers/${id}`),
};

export default api;

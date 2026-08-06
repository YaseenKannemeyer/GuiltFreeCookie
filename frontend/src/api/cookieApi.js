import api from "./index"

const COOKIE_BASE = "/guiltFreeCookie/cookie";

export default {
  async getAll() {
    const response = await api.get(`${COOKIE_BASE}/getAll`);
    return response.data;
  },

  async create(cookie) {
    const response = await api.post(`${COOKIE_BASE}/create`, cookie);
    return response.data;
  },

  async update(cookie) {
    const response = await api.put(
      `${COOKIE_BASE}/update/${cookie.cookieId}`,
      cookie,
    );
    return response.data;
  },

  async delete(cookieId) {
    await api.delete(`${COOKIE_BASE}/delete/${cookieId}`);
  },

  async read(cookieId) {
    const response = await api.get(`${COOKIE_BASE}/read/${cookieId}`);
    return response.data;
  },

  async getById(cookieId) {
    return this.read(cookieId);
  },
};

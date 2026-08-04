import axios from "axios";

const API_BASE = "http://localhost:8080/guiltFreeCookie/cookie";

export default {
  async getAll() {
    const response = await axios.get(`${API_BASE}/getAll`);
    return response.data;
  },

  async create(cookie) {
    const response = await axios.post(`${API_BASE}/create`, cookie);
    return response.data;
  },

  async update(cookie) {
    const response = await axios.put(
      `${API_BASE}/update/${cookie.cookieId}`,
      cookie,
    );
    return response.data;
  },

  async delete(cookieId) {
    await axios.delete(`${API_BASE}/delete/${cookieId}`);
  },

  async read(cookieId) {
    const response = await axios.get(`${API_BASE}/read/${cookieId}`);
    return response.data;
  },
  async getById(cookieId) {
    return this.read(cookieId);
  },
};

import axios from "axios";

const API_BASE = "http://localhost:8080/guiltFreeCookie/cookie";

const cookieApi = {
  // Get all cookies
  async getAll() {
    const response = await axios.get(`${API_BASE}/getAll`);
    return response.data;
  },

  // Get single cookie by ID
  async getById(id) {
    const response = await axios.get(`${API_BASE}/read/${id}`);
    return response.data;
  },

  // Get cookies by category
  async getByCategory(category) {
    const response = await axios.get(`${API_BASE}/category/${category}`);
    return response.data;
  },

  // Search cookies
  async search(keyword) {
    const response = await axios.get(`${API_BASE}/search`, {
      params: { keyword },
    });
    return response.data;
  },

  // Filter cookies
  async filter(params) {
    const response = await axios.get(`${API_BASE}/filter`, { params });
    return response.data;
  },

  // Get by price range
  async getByPriceRange(min, max) {
    const response = await axios.get(`${API_BASE}/price-range`, {
      params: { min, max },
    });
    return response.data;
  },

  // Get allergy-free cookies
  async getAllergyFree(allergy) {
    const response = await axios.get(`${API_BASE}/allergy-free`, {
      params: { allergy },
    });
    return response.data;
  },

  // Create cookie
  async create(cookie) {
    const response = await axios.post(`${API_BASE}/create`, cookie);
    return response.data;
  },

  // Update cookie
  async update(cookie) {
    const response = await axios.put(`${API_BASE}/update`, cookie);
    return response.data;
  },

  // Delete cookie
  async delete(id) {
    const response = await axios.delete(`${API_BASE}/delete/${id}`);
    return response.data;
  },
};

export default cookieApi;

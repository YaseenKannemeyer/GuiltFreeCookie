import axios from "axios";
import { API_BASE } from "./http";
export const api = async (method, path, data, config = {}) =>
  (await axios({ method, url: `${API_BASE}/api/v1${path}`, data, ...config }))
    .data;
export const errorMessage = (e) =>
  e.response?.status === 429
    ? `Too many attempts. Try again in ${e.response.headers?.["retry-after"] || 60} seconds.`
    : e.response?.data?.message ||
      (typeof e.response?.data === "string"
        ? e.response.data
        : "We couldn't connect. Please try again.");
export const catalogApi = {
  search: (params, signal) =>
    api("get", "/cookies", undefined, { params, signal }),
  detail: (id) => api("get", `/cookies/${encodeURIComponent(id)}`),
  categories: () => api("get", "/cookies/categories"),
};

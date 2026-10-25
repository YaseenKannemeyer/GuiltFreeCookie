import { api } from "./storeApi";

export const analyticsApi = {
  summary: (from, to) => api("get", "/admin/analytics", undefined, { params: { from, to } }),
};

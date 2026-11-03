import { api } from "./storeApi";

export const discountApi = {
  list: () => api("get", "/admin/discounts"),
  create: (body) => api("post", "/admin/discounts", body),
  update: (id, body) => api("put", `/admin/discounts/${encodeURIComponent(id)}`, body),
  switchOff: (id) => api("delete", `/admin/discounts/${encodeURIComponent(id)}`),
};

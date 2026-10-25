import axios from "axios";
import { API_BASE } from "./http";
const base = `${API_BASE}/api/v1/me`;
const data = (response) => response.data;
export const accountApi = {
  orders: (page = 0, size = 10) =>
    axios.get(`${base}/orders`, { params: { page, size } }).then(data),
  order: (number) =>
    axios.get(`${base}/orders/${encodeURIComponent(number)}`).then(data),
  claimable: () => axios.get(`${base}/orders/claimable`).then(data),
  requestClaim: () => axios.post(`${base}/orders/claim`).then(data),
  confirmClaim: (token) =>
    axios.post(`${base}/orders/claim/confirm`, { token }).then(data),
  addresses: () => axios.get(`${base}/addresses`).then(data),
  createAddress: (address) =>
    axios.post(`${base}/addresses`, address).then(data),
  updateAddress: (id, address) =>
    axios
      .put(`${base}/addresses/${encodeURIComponent(id)}`, address)
      .then(data),
  deleteAddress: (id) =>
    axios.delete(`${base}/addresses/${encodeURIComponent(id)}`).then(data),
  makeDefault: (id) =>
    axios.put(`${base}/addresses/${encodeURIComponent(id)}/default`).then(data),
  async saveAtCheckout(address) {
    // A failed optional save must not trigger the global 401 sign-in redirect or delay payment indefinitely.
    const response = await axios.post(`${base}/addresses`, address, {
      timeout: 4000,
      validateStatus: () => true,
    });
    if (response.status >= 400)
      throw Object.assign(new Error("Address could not be saved"), {
        response,
      });
    return response.data;
  },
};

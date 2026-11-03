import { ref } from "vue";
import { api } from "../api/storeApi";
import { BUSINESS } from "../constants/business";

const fallback = {
  supportEmail: BUSINESS.email,
  phone: BUSINESS.phone,
  whatsapp: BUSINESS.phone.replace(/\D/g, ""),
  address: BUSINESS.address,
  hours: "",
  responseTime: "",
  social: BUSINESS.socials,
  stats: { averageRating: null, reviewCount: 0, customersServed: 0, cookiesSold: 0 },
};
const info = ref({ ...fallback }),
  error = ref(false);
let pending;
let loaded = false;
async function load() {
  if (loaded) return info.value;
  if (pending) return pending;
  pending = api("get", "/site/info")
    .then((data) => {
      info.value = { ...fallback, ...data,
        supportEmail: data.supportEmail || fallback.supportEmail,
        phone: data.phone || fallback.phone,
        address: data.address || fallback.address,
        stats: { ...fallback.stats, ...data.stats },
      };
      loaded = true;
      error.value = false;
      return info.value;
    })
    .catch(() => {
      error.value = true;
      return info.value;
    })
    .finally(() => (pending = null));
  return pending;
}
export const useSiteInfo = () => ({ info, error, load });

import { ref } from "vue";
import { api } from "../api/storeApi";
const info = ref(null),
  error = ref(false);
let pending;
async function load() {
  if (info.value) return info.value;
  if (pending) return pending;
  pending = api("get", "/site/info")
    .then((data) => {
      info.value = data;
      error.value = false;
      return data;
    })
    .catch(() => {
      error.value = true;
      return null;
    })
    .finally(() => (pending = null));
  return pending;
}
export const useSiteInfo = () => ({ info, error, load });

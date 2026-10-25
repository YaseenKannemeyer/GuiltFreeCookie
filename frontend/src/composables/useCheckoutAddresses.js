import { ref, onMounted, watch } from "vue";
import { accountApi } from "../api/accountApi";
import { useToast } from "./useToast";
export function useCheckoutAddresses(form, isLoggedIn) {
  const addresses = ref([]),
    unavailable = ref(false),
    loading = ref(false);
  const { showToast } = useToast();
  async function loadAddresses() {
    if (!isLoggedIn.value) return;
    loading.value = true;
    try {
      addresses.value = await accountApi.addresses();
      unavailable.value = false;
      const hasDraft = Object.entries(form.address).some(
        ([key, value]) => key !== "province" && value,
      );
      if (!hasDraft && !form.savedAddressId) {
        const preferred = addresses.value.find((a) => a.isDefault);
        if (preferred) chooseAddress(preferred.id);
      }
    } catch {
      unavailable.value = true;
    } finally {
      loading.value = false;
    }
  }
  function chooseAddress(id) {
    form.savedAddressId = id;
    form.saveAddress = false;
    const saved = addresses.value.find((a) => a.id === id);
    if (!saved) return;
    Object.assign(form.address, saved.address);
    form.phone = saved.phone;
    const [first, ...rest] = saved.recipientName.trim().split(/\s+/);
    form.firstName = first;
    form.lastName = rest.join(" ") || form.lastName;
  }
  watch(
    () => form.address,
    () => {
      const saved = addresses.value.find((a) => a.id === form.savedAddressId);
      if (
        saved &&
        Object.keys(form.address).some(
          (k) => (form.address[k] || "") !== (saved.address[k] || ""),
        )
      )
        form.savedAddressId = "";
    },
    { deep: true },
  );
  async function saveBeforeCheckout() {
    if (
      !isLoggedIn.value ||
      !form.saveAddress ||
      form.shippingMethod !== "COURIER"
    )
      return;
    try {
      await accountApi.saveAtCheckout({
        label: "Delivery",
        recipientName:
          `${form.firstName.trim()} ${form.lastName.trim()}`.trim(),
        phone: form.phone.trim(),
        isDefault: false,
        address: { ...form.address },
      });
      form.saveAddress = false;
    } catch {
      showToast(
        "Your address wasn't saved. Your order can still go ahead.",
        "info",
      );
    }
  }
  onMounted(loadAddresses);
  return {
    addresses,
    unavailable,
    loading,
    loadAddresses,
    chooseAddress,
    saveBeforeCheckout,
  };
}

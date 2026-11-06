import { shallowRef } from 'vue'

// Keep notifications inside the active dialog's top layer and inert boundary.
const dialogs = []
export const activeModal = shallowRef(null)

export function registerModal(dialog) {
  if (!dialogs.includes(dialog)) dialogs.push(dialog)
  activeModal.value = dialogs.at(-1) || null
}

export function unregisterModal(dialog) {
  const index = dialogs.indexOf(dialog)
  if (index !== -1) dialogs.splice(index, 1)
  activeModal.value = dialogs.at(-1) || null
}

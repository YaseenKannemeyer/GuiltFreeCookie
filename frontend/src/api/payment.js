import { DEMO_MODE } from "../constants/business.js";

// PayFast takes the shopper by form POST: build the form from the API's fields, in order, and submit it.
export function submitPaymentForm({ gateway, actionUrl, fields }, { demoMode = DEMO_MODE } = {}) {
  if (gateway === "DEMO" && demoMode && actionUrl === "/demo-payment" && fields?.order && fields?.token) {
    window.location.assign(`/demo-payment?${new URLSearchParams({ order: fields.order, token: fields.token })}`);
    return;
  }
  if (demoMode && actionUrl !== "https://sandbox.payfast.co.za/eng/process") {
    throw new Error("Live payments are disabled in this demo.");
  }
  if (![
    "https://www.payfast.co.za/eng/process",
    "https://sandbox.payfast.co.za/eng/process",
  ].includes(actionUrl) || !fields || typeof fields !== "object" || Array.isArray(fields)) {
    throw new Error("We couldn't open the payment page. Open your order to try paying again.");
  }
  const form = document.createElement("form");
  form.method = "POST";
  form.action = actionUrl;
  for (const [name, value] of Object.entries(fields)) {
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = name;
    input.value = value;
    form.append(input);
  }
  document.body.append(form);
  form.submit();
}

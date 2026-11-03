import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

// Dev server and `vite preview` forward API calls to Spring Boot
// (API_PROXY_TARGET overrides the default port).
const apiProxy = (env) => ({
  "/guiltFreeCookie": {
    target: env.API_PROXY_TARGET || "http://localhost:8080",
    changeOrigin: true,
  },
});

// The API's origin, when it lives on another domain (VITE_API_BASE_URL).
function apiOrigin(env) {
  try {
    return new URL(env.VITE_API_BASE_URL).origin;
  } catch {
    return ""; // relative path: same origin as the site
  }
}

/**
 * Production pages get a Content-Security-Policy: scripts only from this
 * site, so injected markup can't run code. Development is left alone (Vite's
 * dev server injects styles and HMR code). Inline styles stay allowed: Vue
 * renders style bindings into the prerendered HTML as style attributes.
 * Headers a <meta> tag can't set (frame-ancestors, HSTS) belong on the host;
 * see the README.
 */
function contentSecurityPolicy(env) {
  const api = apiOrigin(env);
  const policy = [
    "default-src 'self'",
    "script-src 'self'",
    "style-src 'self' 'unsafe-inline'",
    `img-src 'self' data: blob:${api ? ` ${api}` : ""}`,
    "font-src 'self'",
    `connect-src 'self'${api ? ` ${api}` : ""}`,
    "media-src 'self'",
    "manifest-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self' https://www.payfast.co.za https://sandbox.payfast.co.za",
  ].join("; ");

  return {
    name: "gfc-content-security-policy",
    apply: (_, { command, isSsrBuild }) => command === "build" && !isSsrBuild,
    // Straight after <meta charset>: a policy only covers what loads after it.
    transformIndexHtml: {
      order: "pre",
      handler: (html) =>
        html.replace(
          /<meta charset="UTF-8" \/>/,
          (charset) =>
            `${charset}\n    <meta http-equiv="Content-Security-Policy" content="${policy}" />` +
            `\n    <meta name="referrer" content="strict-origin-when-cross-origin" />`,
        ),
    },
  };
}

export default defineConfig(({ mode, isSsrBuild }) => {
  const env = loadEnv(mode, process.cwd(), "");

  return {
    plugins: [vue(), contentSecurityPolicy(env)],

    build: {
      // Never publish source maps: they hand anyone the original source.
      sourcemap: false,
      // The prerender step uses this to preload each page's own chunks.
      ssrManifest: !isSsrBuild,
    },

    server: {
      port: 5173,
      proxy: apiProxy(env),
    },

    preview: {
      port: 4173,
      proxy: apiProxy(env),
    },
  };
});

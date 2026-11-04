// Sentry error reporting, only when VITE_SENTRY_DSN is set. Loaded as a separate chunk so builds
// without a DSN don't ship it. Errors only: no personal data, replays or tracing (POPIA, free-plan quota).
export function startErrorReporting(app) {
  const dsn = import.meta.env.VITE_SENTRY_DSN;
  if (!dsn) return;
  import("@sentry/vue")
    .then((Sentry) =>
      Sentry.init({
        app,
        dsn,
        environment: import.meta.env.VITE_SENTRY_ENVIRONMENT || import.meta.env.MODE,
        sendDefaultPii: false,
      }),
    )
    .catch(() => {});
}

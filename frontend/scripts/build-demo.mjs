import { spawnSync } from "node:child_process";
const result = spawnSync("npm", ["run", "build"], {
  stdio: "inherit",
  env: { ...process.env, VITE_DEMO_MODE: "true", REQUIRE_CATALOG: "true",
    PRERENDER_API_URL: process.env.PRERENDER_API_URL || "http://127.0.0.1:8080/guiltFreeCookie" },
});
process.exit(result.status ?? 1);

/**
 * import-cookie-catalog — creates the generated cookie catalogue through the
 * same multipart endpoint the admin form uses.
 *
 *   node scripts/import-cookie-catalog.mjs --dry-run
 *   node scripts/import-cookie-catalog.mjs --price=45 --archive-existing
 *
 * Reads scripts/cookie-catalog.json, uploads each frontend/public/img/cookies/<slug>.webp
 * and records the returned cookieId/image in scripts/cookie-import-results.json so a
 * rerun skips what is already in. --archive-existing archives the products that were
 * active before the import, only once every catalogue cookie went in.
 *
 * Credentials come from ADMIN_EMAIL / ADMIN_PASSWORD, or are prompted for.
 */
import { readFile, writeFile } from "node:fs/promises";
import { existsSync } from "node:fs";
import { fileURLToPath } from "node:url";
import path from "node:path";
import readline from "node:readline";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const CATALOG_PATH = path.join(ROOT, "frontend/scripts/cookie-catalog.json");
const RESULTS_PATH = path.join(ROOT, "frontend/scripts/cookie-import-results.json");
const API = process.env.API_BASE || "http://localhost:8080/guiltFreeCookie";

const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const archiveExisting = args.includes("--archive-existing");
const price = Number(args.find((a) => a.startsWith("--price="))?.split("=")[1] ?? 45);

const sameName = (a, b) => a.trim().toLowerCase() === b.trim().toLowerCase();

function ask(question, { hidden = false } = {}) {
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout, terminal: true });
  if (hidden) {
    rl._writeToOutput = (text) => {
      if (text.startsWith(question)) rl.output.write(question);
    };
  }
  return new Promise((resolve) =>
    rl.question(question, (answer) => {
      rl.close();
      if (hidden) process.stdout.write("\n");
      resolve(answer.trim());
    }),
  );
}

async function request(method, url, { token, body, json } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (json) headers["Content-Type"] = "application/json";
  const response = await fetch(`${API}${url}`, { method, headers, body: json ? JSON.stringify(json) : body });
  if (!response.ok) {
    throw new Error(`${method} ${url} -> HTTP ${response.status} ${await response.text()}`);
  }
  return response.status === 204 ? null : response.json();
}

async function login() {
  const email = process.env.ADMIN_EMAIL || (await ask("Admin email: "));
  const password = process.env.ADMIN_PASSWORD || (await ask("Admin password: ", { hidden: true }));
  const { token } = await request("POST", "/admin/login", { json: { email, password } });
  return token;
}

async function main() {
  if (!(price > 0)) throw new Error("--price must be a number above 0");

  const { products } = JSON.parse(await readFile(CATALOG_PATH, "utf8"));
  const results = existsSync(RESULTS_PATH) ? JSON.parse(await readFile(RESULTS_PATH, "utf8")) : {};

  for (const p of products) {
    const file = path.join(ROOT, p.webPath);
    if (!existsSync(file)) throw new Error(`Missing image for ${p.slug}: ${p.webPath}`);
  }

  const token = dryRun ? null : await login();
  const existing = dryRun
    ? await request("GET", "/cookie/getAll")
    : await request("GET", "/cookie/getAllForAdmin", { token });

  // A backend started before the longDescription column existed silently drops it.
  if (existing.length && !("longDescription" in existing[0])) {
    const message = "The running backend doesn't return longDescription yet. Restart it, then rerun.";
    if (!dryRun) throw new Error(message);
    console.warn(`warning: ${message}\n`);
  }

  const isCatalogCookie = (c) => products.some((p) => sameName(p.name, c.description || ""));
  const toArchive = existing.filter((c) => !c.archived && !isCatalogCookie(c));

  let failures = 0;
  for (const p of products) {
    if (results[p.slug]) {
      console.log(`skip    ${p.name} (already imported as ${results[p.slug].cookieId})`);
      continue;
    }
    if (existing.some((c) => sameName(c.description || "", p.name))) {
      console.log(`skip    ${p.name} (a product with this name already exists)`);
      continue;
    }

    const cookie = {
      category: p.category,
      description: p.name,
      longDescription: p.description,
      ingredients: p.proposedIngredients,
      allergies: p.proposedAllergens,
      price,
    };
    if (dryRun) {
      console.log(`create  ${p.name} [${p.category}] R${price} <- ${p.webPath}`);
      continue;
    }

    const form = new FormData();
    form.append("cookie", new Blob([JSON.stringify(cookie)], { type: "application/json" }));
    form.append("image", new Blob([await readFile(path.join(ROOT, p.webPath))], { type: "image/webp" }), `${p.slug}.webp`);

    try {
      const created = await request("POST", "/cookie/create", { token, body: form });
      results[p.slug] = {
        cookieId: created.cookieId,
        image: created.image,
        name: p.name,
        category: p.category,
        importedAt: new Date().toISOString(),
      };
      await writeFile(RESULTS_PATH, `${JSON.stringify(results, null, 2)}\n`);
      console.log(`created ${p.name} -> ${created.cookieId} (${created.image})`);
    } catch (error) {
      failures++;
      console.error(`FAILED  ${p.name}: ${error.message}`);
    }
  }

  if (!archiveExisting) return;
  if (failures) {
    console.log(`\n${failures} import(s) failed, so nothing was archived. Fix and rerun.`);
    process.exitCode = 1;
    return;
  }
  for (const c of toArchive) {
    if (dryRun) {
      console.log(`archive ${c.description} (${c.cookieId})`);
      continue;
    }
    await request("PUT", `/cookie/archive/${c.cookieId}`, { token });
    console.log(`archived ${c.description} (${c.cookieId})`);
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});

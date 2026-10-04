const express = require("express");
const fs = require("fs");
const os = require("os");
const path = require("path");
const crypto = require("crypto");
const { execFileSync } = require("child_process");

const app = express();
app.disable("x-powered-by");
app.use(express.json({ limit: "60mb" }));

const PORT = Number(process.env.PORT || 8080);
const REPO = process.env.GITHUB_REPOSITORY || "huter413/Musab-dosya-yoneticisi";
const TOKEN = process.env.GITHUB_TOKEN;
const MAX_SOURCE_BYTES = 45 * 1024 * 1024;
const BUILD_TIMEOUT_MS = 20 * 60 * 1000;

if (!TOKEN) console.warn("GITHUB_TOKEN is not configured; build requests will be rejected.");

app.get("/health", (_req, res) => res.json({ state: "ok", service: "musab-build-service", repository: REPO }));

app.post("/build", async (req, res) => {
  if (!TOKEN) return res.status(503).json({ state: "failed", error: "Build service GitHub token is not configured." });

  const { type, sign, javaVersion, target, sourceName, sourceBase64 } = req.body || {};
  if (!sourceBase64 || !["APK", "JAR"].includes(type)) {
    return res.status(400).json({ state: "failed", error: "Geçersiz derleme isteği." });
  }

  let sourceBytes;
  try {
    sourceBytes = Buffer.from(sourceBase64, "base64");
  } catch (_) {
    return res.status(400).json({ state: "failed", error: "Kaynak Base64 verisi geçersiz." });
  }
  if (!sourceBytes.length || sourceBytes.length > MAX_SOURCE_BYTES) {
    return res.status(413).json({ state: "failed", error: "Kaynak dosyası 45 MB sınırını aşamaz." });
  }

  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "musab-build-"));
  const safeName = String(sourceName || "source.zip").toLowerCase().endsWith(".jar") ? "source.jar" : "source.zip";
  const source = path.join(dir, safeName);
  fs.writeFileSync(source, sourceBytes);
  const branch = "musab-build-service-" + Date.now() + "-" + crypto.randomBytes(4).toString("hex");

  try {
    const base = await gh(`/repos/${REPO}/git/ref/heads/main`);
    const baseCommit = await gh(`/repos/${REPO}/git/commits/${base.object.sha}`);
    const configJson = JSON.stringify({
      type,
      sign: !!sign,
      javaVersion: javaVersion || "Android",
      target: target || "Android",
      sourceName: safeName
    });

    // Create both input files as blobs and publish them in ONE commit.
    // This prevents Actions from starting on a half-written build request.
    const configBlob = await gh(`/repos/${REPO}/git/blobs`, "POST", {
      encoding: "base64",
      content: Buffer.from(configJson).toString("base64")
    });
    const sourceBlob = await gh(`/repos/${REPO}/git/blobs`, "POST", {
      encoding: "base64",
      content: sourceBytes.toString("base64")
    });
    const tree = await gh(`/repos/${REPO}/git/trees`, "POST", {
      base_tree: baseCommit.tree.sha,
      tree: [
        { path: `build-input/${branch}/config.json`, mode: "100644", type: "blob", sha: configBlob.sha },
        { path: `build-input/${branch}/${safeName}`, mode: "100644", type: "blob", sha: sourceBlob.sha }
      ]
    });
    const commit = await gh(`/repos/${REPO}/git/commits`, "POST", {
      message: `Musab remote build ${branch}`,
      tree: tree.sha,
      parents: [baseCommit.sha]
    });
    await gh(`/repos/${REPO}/git/refs`, "POST", { ref: `refs/heads/${branch}`, sha: commit.sha });

    const run = await waitRun(branch);
    if (run.conclusion !== "success") {
      return res.status(422).json({ state: "failed", error: `GitHub Actions derlemesi başarısız: ${run.conclusion || "unknown"}.`, runId: run.id });
    }

    const arts = await gh(`/repos/${REPO}/actions/runs/${run.id}/artifacts`);
    const art = (arts.artifacts || []).find(x => x.name === "musab-build-output" && !x.expired);
    if (!art) throw new Error("Derleme çıktısı bulunamadı.");

    const zip = await raw(art.archive_download_url);
    const z = path.join(dir, "out.zip");
    fs.writeFileSync(z, zip);
    execFileSync("unzip", ["-o", z, "-d", path.join(dir, "out")]);

    const wanted = type === "APK" ? [".apk"] : [".jar"];
    const files = walk(path.join(dir, "out")).filter(x => wanted.some(ext => x.toLowerCase().endsWith(ext)));
    if (!files.length) throw new Error("APK/JAR çıktısı arşivde yok.");

    const selected = type === "APK"
      ? (files.find(x => x.toLowerCase().endsWith("musab-dosya-yoneticisi.apk")) || files.find(x => !x.toLowerCase().includes("skk")) || files[0])
      : files[0];
    const out = fs.readFileSync(selected);

    return res.json({ state: "success", outputName: path.basename(selected), outputBase64: out.toString("base64"), runId: run.id });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ state: "failed", error: e.message || String(e) });
  } finally {
    // Build branches contain only temporary source input; remove them after the run.
    try { await gh(`/repos/${REPO}/git/refs/heads/${branch}`, "DELETE"); } catch (cleanupError) { console.warn("Build branch cleanup failed:", cleanupError.message); }
    fs.rmSync(dir, { recursive: true, force: true });
  }
});

async function gh(p, method = "GET", body) {
  const response = await fetch("https://api.github.com" + p, {
    method,
    headers: {
      Authorization: "Bearer " + TOKEN,
      Accept: "application/vnd.github+json",
      "X-GitHub-Api-Version": "2022-11-28",
      "Content-Type": "application/json"
    },
    body: body ? JSON.stringify(body) : undefined
  });
  const text = await response.text();
  if (!response.ok) throw new Error(`GitHub API ${response.status}: ${text}`);
  return text ? JSON.parse(text) : {};
}

async function waitRun(branch) {
  const deadline = Date.now() + BUILD_TIMEOUT_MS;
  while (Date.now() < deadline) {
    const x = await gh(`/repos/${REPO}/actions/runs?branch=${encodeURIComponent(branch)}&event=push&per_page=20`);
    const r = (x.workflow_runs || []).find(y => y.head_branch === branch);
    if (r && r.status === "completed") return r;
    await new Promise(resolve => setTimeout(resolve, 5000));
  }
  throw new Error("Actions zaman aşımı (20 dakika).");
}

async function raw(url) {
  const r = await fetch(url, { headers: { Authorization: "Bearer " + TOKEN, Accept: "application/vnd.github+json" } });
  if (!r.ok) throw new Error("Artifact HTTP " + r.status);
  return Buffer.from(await r.arrayBuffer());
}

function walk(root) {
  if (!fs.existsSync(root)) return [];
  const out = [];
  for (const entry of fs.readdirSync(root, { withFileTypes: true })) {
    const p = path.join(root, entry.name);
    if (entry.isDirectory()) out.push(...walk(p)); else out.push(p);
  }
  return out;
}

app.listen(PORT, () => console.log(`Musab build service listening on ${PORT}`));

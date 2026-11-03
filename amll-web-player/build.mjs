import { build } from "vite";
import { cp, mkdir, rm, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const project = dirname(fileURLToPath(import.meta.url));
const repository = resolve(project, "..");
const target = resolve(repository, "app/src/main/assets/amll");
const generatedAssets = resolve(target, "assets");
if (dirname(target) !== resolve(repository, "app/src/main/assets") || dirname(generatedAssets) !== target) {
  throw new Error("Generated web assets must stay inside app/src/main/assets/amll");
}
await build({ root: project });
await mkdir(target, { recursive: true });
await rm(generatedAssets, { recursive: true, force: true });
await cp(resolve(project, "dist"), target, { recursive: true });
const indexFile = resolve(target, "index.html");
await writeFile(indexFile, (await readFile(indexFile, "utf8")).replace(/\r\n?/g, "\n"));
console.log("AMLL assets updated:", target);

#!/usr/bin/env node
// Generates src/data/release.json from the ACTUAL release APK, so the website never shows
// invented version, size or checksum values.
//
// Usage:
//   node scripts/release-meta.mjs --apk <path/to/ShifaDaily-1.0.0.apk> --url <https download URL> [--date YYYY-MM-DD]
//   node scripts/release-meta.mjs --none        # mark "no release available"
//
// Requires ANDROID_HOME (aapt2 + apksigner from build-tools) for package/SDK/certificate data.
import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, statSync, readdirSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { join, basename, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const out = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'data', 'release.json');
const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, arr) => {
  if (a.startsWith('--')) acc.push([a.slice(2), arr[i + 1] && !arr[i + 1].startsWith('--') ? arr[i + 1] : true]);
  return acc;
}, []));

if (args.none) {
  writeFileSync(out, JSON.stringify({ available: false }, null, 2) + '\n');
  console.log('release.json: no release available');
  process.exit(0);
}
if (!args.apk || !args.url) {
  console.error('Missing --apk or --url'); process.exit(1);
}
if (!/^https:\/\//.test(args.url)) { console.error('The download URL must use HTTPS.'); process.exit(1); }

const apk = args.apk;
const bytes = readFileSync(apk);
const sha256 = createHash('sha256').update(bytes).digest('hex');
const sizeBytes = statSync(apk).size;

const sdk = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
if (!sdk) { console.error('Set ANDROID_HOME to read package metadata with aapt2/apksigner.'); process.exit(1); }
const btDir = join(sdk, 'build-tools');
const bt = readdirSync(btDir).sort().reverse().find((v) => existsSync(join(btDir, v, process.platform === 'win32' ? 'aapt2.exe' : 'aapt2')));
const tool = (name) => join(btDir, bt, process.platform === 'win32' ? `${name}.exe` : name);

const badging = execFileSync(tool('aapt2'), ['dump', 'badging', apk], { encoding: 'utf8' });
const pkg = /package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'/.exec(badging);
const minSdk = /minSdkVersion:'(\d+)'/.exec(badging) || /sdkVersion:'(\d+)'/.exec(badging);
const targetSdk = /targetSdkVersion:'(\d+)'/.exec(badging);
if (!pkg || !minSdk || !targetSdk) { console.error('Could not parse aapt2 output'); process.exit(1); }

// Run apksigner's JAR directly (the Windows .bat wrapper breaks on paths containing spaces).
const java = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin', 'java') : 'java';
const certs = execFileSync(java, ['-jar', join(btDir, bt, 'lib', 'apksigner.jar'), 'verify', '--print-certs', apk], { encoding: 'utf8' });
const cert = /Signer #1 certificate SHA-256 digest: ([0-9a-f]+)/.exec(certs);
if (!cert) { console.error('APK is not signed / signature could not be verified'); process.exit(1); }

const release = {
  available: true,
  version: pkg[3],
  versionCode: Number(pkg[2]),
  packageName: pkg[1],
  minSdk: Number(minSdk[1]),
  targetSdk: Number(targetSdk[1]),
  sizeBytes,
  sha256,
  certSha256: cert[1],
  releaseDate: args.date || new Date().toISOString().slice(0, 10),
  apkUrl: args.url,
  apkFileName: basename(apk),
};
writeFileSync(out, JSON.stringify(release, null, 2) + '\n');
console.log(release);

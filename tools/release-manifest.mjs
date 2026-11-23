import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { createReadStream } from 'node:fs';
import { readFile, stat, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';

const [mode, tag, number] = process.argv.slice(2);
const match = /^v([1-9]\d{0,3})\.(\d)\.(0|[1-9]\d?)$/.exec(tag ?? '');
const versionCode = Number(number);
if (!match || versionCode !== Number(match[1]) * 1000 + Number(match[2]) * 100 + Number(match[3])) {
  throw new Error('Supply a release tag and its matching version code.');
}
const versionName = tag.slice(1);
const packageName = 'com.mohamedzayed.nocturne';
const baseUrl = `https://github.com/MZAYED2/Nocturne/releases/download/${tag}`;

async function fingerprint(file) {
  const hash = createHash('sha256');
  for await (const chunk of createReadStream(file)) hash.update(chunk);
  return { size_bytes: (await stat(file)).size, sha256: hash.digest('hex') };
}

if (mode === 'catalog') {
  const catalog = JSON.parse(await readFile('app/src/main/assets/catalog.json', 'utf8'));
  catalog.version = versionCode;
  catalog.updated_at = new Date().toISOString();
  catalog.download_base_url = baseUrl;
  for (const face of catalog.watchfaces) {
    const file = resolve('distribution', face.apk_filename);
    const validation = execFileSync('java', [
      '-jar', 'tools/validator-push-cli.jar', '--apk_path', file, '--package_name', packageName,
    ], { encoding: 'utf8' });
    const token = /generated token:\s*(\S+)/.exec(validation)?.[1];
    if (!token) throw new Error(`Watch Face Push validation failed for ${face.apk_filename}`);
    Object.assign(face, await fingerprint(file), {
      validation_token: token,
      download_url: `${baseUrl}/${face.apk_filename}`,
      version_code: versionCode,
      version_name: versionName,
    });
  }
  await writeFile('distribution/catalog.json', JSON.stringify(catalog, null, 2) + '\n');
} else if (mode === 'manifest') {
  const dir = 'release-apks';
  const catalog = JSON.parse(await readFile(join(dir, 'catalog.json'), 'utf8'));
  const artifacts = [];
  for (const [target, minSdk, formFactor] of [['Phone', 24, 'android'], ['WearBridge', 33, 'wear_os']]) {
    const filename = `Nocturne-${target}-${tag}.apk`;
    artifacts.push({
      filename, package_name: packageName, version_code: versionCode, version_name: versionName,
      url: `${baseUrl}/${filename}`, min_sdk: minSdk, form_factor: formFactor,
      transfer_protocol: 2, ...(await fingerprint(join(dir, filename))),
    });
  }
  for (const face of catalog.watchfaces) {
    const fingerprintOnDisk = await fingerprint(join(dir, face.apk_filename));
    if (fingerprintOnDisk.sha256 !== face.sha256 || fingerprintOnDisk.size_bytes !== face.size_bytes) {
      throw new Error(`Catalog fingerprint differs from release package ${face.apk_filename}`);
    }
    artifacts.push({
      filename: face.apk_filename, package_name: face.package_name,
      version_code: face.version_code, version_name: face.version_name,
      url: face.download_url, min_sdk: 34, form_factor: 'wear_os_watch_face',
      requires_watch_face_push: true, validation_token: face.validation_token,
      ...fingerprintOnDisk,
    });
  }
  const notes = process.env.NOCTURNE_RELEASE_NOTES || `Nocturne ${versionName}`;
  const manifest = { schema_version: 1, version_code: versionCode, version_name: versionName, tag,
    release_notes: notes, artifacts };
  await writeFile(join(dir, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
} else {
  throw new Error('Choose catalog or manifest mode.');
}

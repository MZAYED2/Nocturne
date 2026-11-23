import { createHash } from 'node:crypto';
import { createReadStream, statSync } from 'node:fs';
import { createServer } from 'node:http';

const [tag, phoneApk, bridgeApk] = process.argv.slice(2);
if (!/^v\d+\.\d\.\d{1,2}$/.test(tag ?? '') || !phoneApk) {
  throw new Error('Usage: node tools/local-update-server.mjs v1.0.8 <phone.apk> [bridge.apk]');
}

const base = 'http://127.0.0.1:8765';
const packages = new Map();
const assets = [];
for (const [type, file] of [['Phone', phoneApk], ['WearBridge', bridgeApk]]) {
  if (!file) continue;
  const name = `Nocturne-${type}-${tag}.apk`;
  const path = `/${name}`;
  const hash = createHash('sha256');
  for await (const chunk of createReadStream(file)) hash.update(chunk);
  const size = statSync(file).size;
  packages.set(path, { file, size });
  assets.push({ name, size, digest: `sha256:${hash.digest('hex')}`, browser_download_url: base + path });
}

createServer((request, response) => {
  if (request.url === '/latest') {
    response.writeHead(200, { 'Content-Type': 'application/json' });
    response.end(JSON.stringify({
      tag_name: tag,
      name: `Nocturne ${tag} local test`,
      body: 'Local update fixture with real signed APKs.',
      published_at: new Date().toISOString(),
      assets,
    }));
    return;
  }
  const apk = packages.get(request.url);
  if (!apk) {
    response.writeHead(404).end();
    return;
  }
  response.writeHead(200, {
    'Content-Type': 'application/vnd.android.package-archive',
    'Content-Length': apk.size,
  });
  createReadStream(apk.file).pipe(response);
}).listen(8765, '127.0.0.1', () => console.log(`Serving ${tag} at ${base}/latest`));

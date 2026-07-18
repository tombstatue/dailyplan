#!/usr/bin/env node
// 通过 GitHub Git Data API 把当前 HEAD 的全部文件推送到远程 main 分支。
// 用途：本机 git push 通道被网络干扰时的替代推送方式（api.github.com 通道可用）。
// 用法：node tools/api-push.mjs
import { execSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const OWNER = 'tombstatue';
const REPO = 'dailyplan';
const BRANCH = 'main';
const ROOT = fileURLToPath(new URL('..', import.meta.url)); // 脚本在 tools/ 下，上一级即项目根
const TOKEN = execSync('gh auth token', { encoding: 'utf8' }).trim();
const API = 'https://api.github.com';

async function gh(method, path, body) {
  const res = await fetch(API + path, {
    method,
    headers: {
      Authorization: `Bearer ${TOKEN}`,
      Accept: 'application/vnd.github+json',
      'User-Agent': 'api-push-script',
      'Content-Type': 'application/json',
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 404 && method === 'GET') return null; // 仅 GET 的 404 表示"不存在"
  if (!res.ok) throw new Error(`${method} ${path} → ${res.status}: ${await res.text()}`);
  return res.json();
}

// 1. 列出 git 跟踪的全部文件（自动排除 .gitignore 内容，如签名密钥）
const files = execSync('git ls-files -z', { cwd: ROOT, encoding: 'utf8' })
  .split('\0')
  .filter(Boolean);
console.log(`共 ${files.length} 个文件，开始上传...`);

// 2. 逐个上传为 blob
const tree = [];
let n = 0;
for (const f of files) {
  const content = readFileSync(join(ROOT, f)).toString('base64');
  const blob = await gh('POST', `/repos/${OWNER}/${REPO}/git/blobs`, { content, encoding: 'base64' });
  tree.push({ path: f, mode: '100644', type: 'blob', sha: blob.sha });
  n += 1;
  if (n % 5 === 0 || n === files.length) console.log(`  已上传 ${n}/${files.length}`);
}

// 3. 组树 → 建提交 → 更新分支引用
const treeObj = await gh('POST', `/repos/${OWNER}/${REPO}/git/trees`, { tree });
const ref = await gh('GET', `/repos/${OWNER}/${REPO}/git/ref/heads/${BRANCH}`);
const parents = ref ? [ref.object.sha] : [];
const msg = execSync('git log -1 --pretty=%s', { cwd: ROOT, encoding: 'utf8' }).trim() + ' [api-sync]';
const commit = await gh('POST', `/repos/${OWNER}/${REPO}/git/commits`, {
  message: msg,
  tree: treeObj.sha,
  parents,
});
if (ref) {
  await gh('PATCH', `/repos/${OWNER}/${REPO}/git/refs/heads/${BRANCH}`, { sha: commit.sha, force: true });
} else {
  await gh('POST', `/repos/${OWNER}/${REPO}/git/refs`, { ref: `refs/heads/${BRANCH}`, sha: commit.sha });
}
console.log(`完成：远程 ${BRANCH} 已更新为 ${commit.sha.slice(0, 7)}  "${msg}"`);

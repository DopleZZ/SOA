'use strict';

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const ejs = require('ejs');

const root = path.join(__dirname, '..');
const distDir = path.join(root, 'dist');
const staticDir = path.join(root, 'static');

function collect(dir, ext, acc = []) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const p = path.join(dir, entry.name);
        if (entry.isDirectory()) collect(p, ext, acc);
        else if (entry.name.endsWith(ext)) acc.push(p);
    }
    return acc;
}

let failed = false;

for (const file of [path.join(root, 'server.js'), ...collect(path.join(root, 'src'), '.js'), path.join(staticDir, 'app.js')]) {
    try {
        execFileSync(process.execPath, ['--check', file], { stdio: 'pipe' });
        console.log(`ok   ${path.relative(root, file)}`);
    } catch (e) {
        failed = true;
        console.error(`fail ${path.relative(root, file)}\n${e.stderr}`);
    }
}

for (const file of collect(path.join(root, 'views'), '.ejs')) {
    try {
        ejs.compile(fs.readFileSync(file, 'utf8'), { filename: file });
        console.log(`ok   ${path.relative(root, file)}`);
    } catch (e) {
        failed = true;
        console.error(`fail ${path.relative(root, file)}\n${e.message}`);
    }
}

if (failed) {
    console.error('build failed');
    process.exit(1);
}

fs.rmSync(distDir, { recursive: true, force: true });
fs.mkdirSync(distDir, { recursive: true });
for (const file of [...collect(staticDir, '.html'), ...collect(staticDir, '.js')]) {
    fs.copyFileSync(file, path.join(distDir, path.basename(file)));
}
fs.copyFileSync(path.join(staticDir, 'style.css'), path.join(distDir, 'style.css'));
for (const entry of fs.readdirSync(distDir)) {
    console.log(`dist ${entry}`);
}
console.log(`build ok: ${path.relative(root, distDir)}`);

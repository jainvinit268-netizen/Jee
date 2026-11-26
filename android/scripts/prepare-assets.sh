#!/usr/bin/env bash
set -euo pipefail
ZIP_URL="https://raw.githubusercontent.com/jainvinit268-netizen/Jee/main/eduniti_website_full_complete.zip"
mkdir -p /tmp/eduniti-source app/src/main/assets
curl -L --fail "$ZIP_URL" -o /tmp/eduniti.zip
unzip -q -o /tmp/eduniti.zip -d /tmp/eduniti-source
node - <<'NODE'
const fs=require('fs');
const source=fs.readFileSync('/tmp/eduniti-source/bank.js','utf8').trim();
const json=source.replace(/^window\.EDUNITI_BANK\s*=\s*/, '').replace(/;\s*$/, '');
const bank=JSON.parse(json);
const questions=bank.reduce((n,p)=>n+p.questions.length,0);
if(bank.length!==28 || questions!==2403) throw new Error(`Unexpected bank size: ${bank.length} papers / ${questions} questions`);
fs.writeFileSync('app/src/main/assets/questions.json',json);
NODE
cp -R /tmp/eduniti-source/images app/src/main/assets/images
#!/usr/bin/env python3
"""Create a RESOURCE-ONLY verification APK from an installed debug baseline.

Not a Gradle/source build. Leaves the standard APK untouched. Requires the existing
compatible local signing configuration. Never generates or copies signing keys.
Usage: python3 tools/package_artwork_update.py INSTALLED_BASE.apk OUTPUT.apk
"""
import hashlib
import json
import os
import re
import subprocess
import sys
import tempfile
import zipfile
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CERT = 'fc1828f63432530e788baa4d69c70594157dfad0312a977e710158393e4110f6'
BT = Path.home()/'Android/Sdk/build-tools/35.0.0'

def run(*args, **kwargs):
    return subprocess.check_output(list(map(str,args)), text=True, **kwargs)

def certificate(path):
    output=run(BT/'apksigner','verify','--print-certs',path)
    assert f'certificate SHA-256 digest: {CERT}' in output
    return output

def signature(name):
    return bool(re.match(r'META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|DSA|EC))$', name, re.I))

def main():
    baseline, output=map(Path,sys.argv[1:])
    baseline=baseline.resolve(); output=output.resolve()
    assert baseline != output
    baseline_record=json.loads((ROOT/'docs/codex47/baseline-apk.json').read_text())
    assert hashlib.sha256(baseline.read_bytes()).hexdigest()==baseline_record['sha256']
    certificate(baseline)
    manifest=json.loads((ROOT/'docs/codex47/art-manifest.json').read_text())
    replacements={f'res/drawable-nodpi-v4/{e["resource"]}': e for e in manifest}
    assert len(replacements)==23
    # Local properties are read, never printed or added to the evidence report.
    props={}
    for line in (ROOT/'signing.local.properties').read_text().splitlines():
        if line.strip() and not line.lstrip().startswith('#') and '=' in line:
            k,v=line.split('=',1); props[k.strip()]=v.strip()
    signing_env=os.environ.copy()
    signing_env['TILEBLOOM47_STORE_PASS']=props['storePassword']
    signing_env['TILEBLOOM47_KEY_PASS']=props['keyPassword']
    output.parent.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='tilebloom47-') as temp:
        unsigned=Path(temp)/'unsigned.apk'; aligned=Path(temp)/'aligned.apk'
        with zipfile.ZipFile(baseline) as source, zipfile.ZipFile(unsigned,'w') as target:
            assert len(source.namelist())==len(set(source.namelist()))
            assert set(replacements) == {n for n in source.namelist() if '/botanical_' in n}
            for entry in source.infolist():
                if signature(entry.filename):
                    continue
                data=source.read(entry.filename)
                if entry.filename in replacements:
                    e=replacements[entry.filename]
                    assert hashlib.sha256(data).hexdigest()==e['previousSha256']
                    data=(ROOT/'app/src/main/res/drawable-nodpi'/e['resource']).read_bytes()
                    assert hashlib.sha256(data).hexdigest()==e['sha256']
                target.writestr(entry,data)
        run(BT/'zipalign','-f','-p','4',unsigned,aligned)
        run(BT/'apksigner','sign','--ks',ROOT/props['storeFile'],
            '--ks-key-alias',props['keyAlias'],
            '--ks-pass','env:TILEBLOOM47_STORE_PASS','--key-pass','env:TILEBLOOM47_KEY_PASS',
            '--out',output,aligned,env=signing_env)
    cert_output=certificate(output)
    run(BT/'zipalign','-c','-p','4',output)
    with zipfile.ZipFile(baseline) as before, zipfile.ZipFile(output) as after:
        changed=[]; preserved=[]
        assert len(after.namelist())==len(set(after.namelist()))
        assert {n for n in before.namelist() if not signature(n)} == {n for n in after.namelist() if not signature(n)}
        for name in before.namelist():
            if signature(name):
                continue
            a,b=before.read(name),after.read(name)
            if name in replacements:
                assert a!=b
                assert hashlib.sha256(b).hexdigest()==replacements[name]['sha256']
                changed.append(name)
            else:
                assert a==b, f'Unexpected non-artwork modification: {name}'
                preserved.append(name)
    report=dict(buildType='RESOURCE-ONLY verification APK; NOT a Gradle/source build',
                baselineSha256=baseline_record['sha256'],apk=str(output.relative_to(ROOT)),
                bytes=output.stat().st_size,sha256=hashlib.sha256(output.read_bytes()).hexdigest(),
                buildTimestampUtc=datetime.fromtimestamp(output.stat().st_mtime,timezone.utc).isoformat(),
                certificateSha256=CERT,replaced=changed,preservedEntryCount=len(preserved),
                dexManifestAndResourceTableUnchanged=True)
    (ROOT/'docs/codex47/artwork-apk.json').write_text(json.dumps(report,indent=2)+'\n')
    (ROOT/'docs/codex47/artwork-apk-certificate.txt').write_text(cert_output)
    print(json.dumps(report,indent=2))

if __name__ == '__main__':
    main()

"""Run previously compiled original/optimized host matrices sequentially, fixed JVM heap.
Compile each first with benchmark32.py [--original]. Never represents Android results.
"""
from pathlib import Path
import subprocess, glob
root=Path('docs/codex32')
stdlib=glob.glob(str(Path.home()/'.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/1.9.24/*/*.jar'))[0]
for label, classes in [('original','/tmp/tilebloom32-original'),('optimized','/tmp/tilebloom32-benchmark')]:
    with (root/f'isolated-{label}-jvm.txt').open('w') as output:
        subprocess.run(['/usr/lib/jvm/java-17-openjdk-amd64/bin/java','-Xms256m','-Xmx512m','-cp',f'{classes}:{stdlib}','BaselineKt'],stdout=output,stderr=subprocess.STDOUT,check=True)

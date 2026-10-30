#!/usr/bin/env python3
"""Shared cached Kotlin/JUnit tools for independent diagnostics; never builds an APK."""
from pathlib import Path
import glob, os, subprocess, sys
ROOT = Path.cwd()
os.chdir(ROOT)
SDK = Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or Path.home() / 'Android/Sdk')
CACHE = Path.home() / '.gradle/caches/modules-2/files-2.1'
def jar(group, artifact, version):
    return next(p for p in glob.glob(str(CACHE / group / artifact / version / '*' / '*.jar')) if not p.endswith(('-sources.jar', '-javadoc.jar')))
def run(*args):
    subprocess.run(([str(args[0]), '-XX:-UsePerfData', '-Xmx512m', '-XX:ActiveProcessorCount=2'] + [str(a) for a in args[1:]]) if str(args[0]) == 'java' else [str(a) for a in args], check=True)
stdlib = jar('org.jetbrains.kotlin','kotlin-stdlib','1.9.24')
annotations = jar('org.jetbrains','annotations','13.0')
compiler = ':'.join([jar('org.jetbrains.kotlin','kotlin-compiler-embeddable','1.9.24'), stdlib,
    jar('org.jetbrains.kotlin','kotlin-reflect','1.6.10'), jar('org.jetbrains.intellij.deps','trove4j','1.0.20200330'), annotations])
junit = jar('junit','junit','4.13.2')
hamcrest = jar('org.hamcrest','hamcrest-core','1.3')
android = SDK / 'platforms/android-36/android.jar'
bt = SDK / 'build-tools/35.0.0'
def kotlin(sources, destination, cp):
    run('java','-cp',compiler,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect',
        '-jvm-target','17','-classpath',cp,'-d',destination,*sources)

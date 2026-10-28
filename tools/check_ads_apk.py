#!/usr/bin/env python3
"""Verify actual SDK class descriptors are present in a standard debug APK."""
import sys
import zipfile
with zipfile.ZipFile(sys.argv[1]) as apk:
    dex = b"\n".join(apk.read(n) for n in apk.namelist() if n.endswith('.dex'))
for descriptor in (b'Lcom/google/android/gms/ads/rewarded/RewardedAd;',
                   b'Lcom/google/android/ump/UserMessagingPlatform;'):
    if descriptor not in dex:
        sys.exit(f'Missing SDK class: {descriptor.decode()}')
    print(f'Found {descriptor.decode()}')

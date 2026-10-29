TileBloom sound effects — original procedural synthesis

Ten short mono 22.05 kHz 16-bit PCM effects in app/src/main/res/raw are
synthesized specifically for TileBloom by tools/generate_audio.py (Python
standard library only). No external samples or recordings were used.
The synthesis source and generated audio assets are dedicated under CC0 1.0
Universal: https://creativecommons.org/publicdomain/zero/1.0/.

Effects: ceramic-inspired selection, reveal, hide, crystalline match, hint,
undo, restart, victory, defeat, and a short button click. These are procedural
timbres, not recordings of acoustic instruments. There are no music or ambient
assets. Regenerate with: python3 tools/generate_audio.py.

SoundPool preloads all effects and plays at 30% normalized gain, with at most
three streams. Real listening and artistic quality verification remain pending.

TileBloom garden audio — original procedural composition

All nine effects and the 48-second Zen Garden loop in app/src/main/res/raw
were synthesized specifically for TileBloom with tools/generate_audio.py.
No samples, recordings, melodies, or music from external works were used.
The score, synthesis source, and generated audio assets are dedicated under
CC0 1.0 Universal (https://creativecommons.org/publicdomain/zero/1.0/).

The track uses slow pentatonic arpeggios, decaying piano/harp-like harmonic
partials and a quiet ambient bass texture. It has no vocals or percussion.
Note tails wrap around the phrase boundary before encoding as Ogg Vorbis.
Effects are mono 22.05 kHz 16-bit PCM for inexpensive SoundPool preloading;
the music is streamed from the bundled Ogg resource by MediaPlayer.
Regenerate with: python3 tools/generate_audio.py (Python stdlib + ffmpeg).

Real listening, device loop continuity and artistic quality are pending;
procedural timbres should not be represented as recorded acoustic instruments.

"""Audit actual artwork bytes and unchanged gameplay/geometry against the CODEX 47 commit."""
import hashlib
import json
import subprocess
from pathlib import Path

root = Path(__file__).resolve().parents[2]
manifest = json.loads((root / 'docs/codex47/art-manifest.json').read_text())
for item in manifest:
    asset = root / 'app/src/main/res/drawable-nodpi' / item['resource']
    assert hashlib.sha256(asset.read_bytes()).hexdigest() == item['sha256'], asset

def baseline(path):
    return subprocess.check_output(['git', '--git-dir=.tilebloom-git', '--work-tree=.',
                                    'show', '5fbea42:' + str(path)], cwd=root)

paths = []
for folder in ['domain', 'storage', 'audio', 'ads', 'presentation/board']:
    paths += list((root / 'app/src/main/java/com/tilebloom/game' / folder).rglob('*.kt'))
paths += [root / 'app/src/main/java/com/tilebloom/game/presentation/TileTrayView.kt',
          root / 'app/src/main/AndroidManifest.xml']
for path in paths:
    assert path.read_bytes() == baseline(path.relative_to(root)), path

board = root / 'app/src/main/java/com/tilebloom/game/presentation/MahjongBoardView.kt'
def section(text, start, end=None):
    tail = text.split(start, 1)[1]
    return tail.split(end, 1)[0] if end else tail
before = baseline(board.relative_to(root)).decode()
after = board.read_text()
assert section(before, 'object BotanicalSymbols') == section(after, 'object BotanicalSymbols')
assert section(before, '    private fun drawMystery(', '    private fun drawHint(').replace('surface(goldTrim,', 'surface(mysteryGoldTrim,') == section(after, '    private fun drawMystery(', '    private fun drawHint(')
main = (root / 'app/src/main/java/com/tilebloom/game/presentation/MainActivity.kt').read_text()
header = section(main, '    private fun createGameplay()', '        tray = TileTrayView')
assert 'R.string.app_name' not in header
assert 'R.id.counter' not in main and 'R.plurals.pairs_remaining' not in main
assert 'getString(R.string.level_number, session.level.id)' in main
assert 'setText(R.string.app_name)' in section(main, '    private fun createStartScreen()', '    private fun createGameplay()')
print(f'PASS: {len(manifest)} CODEX 47 botanical hashes; {len(paths)} gameplay/storage/audio/ad/tray/geometry files unchanged.')
print('PASS: botanical mapping/rendering and mystery emblem unchanged; gameplay title/counter removed; Home title and localized level retained.')

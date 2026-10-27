# TileBloom — first Android playtest

Status: NOT PERFORMED. Leave feedback empty until a person plays on Android.
Install and launch using HOST_VERIFICATION.md. This is a debug prototype with one fixed level; choices can lead to no more matches. No hints, undo or shuffle are available.

Tester: __________  Date: __________
Device / Android version: __________  Font size: __________
APK / commit: __________  Session length: __________

Mark each item after trying it; record unexpected behavior below. Let the player
try unaided first. The explanations and winning walkthrough are tester aids;
record any help given so it is not mistaken for immediate understanding.

- [ ] 1. Open TileBloom. It stays open without an error.
- [ ] 2. Observe the initial board: 24 tiles, two overlapping layers, 12 remaining pairs.
- [ ] 3. Identify available tiles: uncovered, with an open left or right side. Blocked tiles have corner bars.
- [ ] 4. Tap one available tile. A gold outline appears.
- [ ] 5. Tap it again. The outline disappears.
- [ ] 6. Tap two available tiles with different symbols. Selection moves to the second; neither disappears.
- [ ] 7. Tap two available identical symbols.
- [ ] 8. Observe both tiles disappear immediately. Check that no other tile disappears.
- [ ] 9. Check Remaining Pairs decreased by one.
- [ ] 10. Tap a blocked tile. Brief border feedback appears; selection and count remain unchanged.
- [ ] 11. Continue playing. Try tile centers, edges, overlap regions and adjacent boundaries. A visible upper tile should receive the touch.
- [ ] 12. Complete the level. If stuck, Restart and use the walkthrough below.
- [ ] 13. Observe Garden Cleared and Play Again, with zero pairs remaining.
- [ ] 14. Tap Play Again / Restart.
- [ ] 15. Check the exact original board and 12 pairs return, with no selection.
- [ ] 16. Try rapid touches, dragging away and back, and two fingers. Check there are no accidental or duplicate matches. Restart during blocked feedback, then match again.
- [ ] 17. Make a match, select a tile and leave using the Home button.
- [ ] 18. Return using Recents.
- [ ] 19. Check removed tiles, selected outline, symbols and counter remain consistent.

Winning walkthrough: remove upper-layer pairs left to right, pairing the two rows in each column. Then remove lower-layer columns left to right; in each column pair rows 1/2, then rows 3/4. Restart first if needed. There are twelve matches.

Repeat on compact, typical and large portrait phones when available. At larger system font size, check all tiles, counters, instructions and Restart remain visible and usable. Record device details separately for each run. Do not mark these checks from mathematical viewport tests alone.

## Human feedback

Objective understood without explanation? __________
Available versus blocked tiles understood? __________
Symbols easy to distinguish? __________
Tiles comfortable to touch? __________
Selected tile obvious? __________
Matching satisfying? __________
Board easy to understand? __________
First level requires meaningful decisions? __________
Game feels relaxing? __________
Would you willingly play another level? __________
Deadlock message understood? __________
Restart intuitive? __________
Help / walkthrough used? __________

## Objective technical observations

Unexpected behavior / reproduction steps: __________
Screenshots / device logs: __________

## Subjective player impressions

Most confusing moment: __________
Suggested improvement: __________

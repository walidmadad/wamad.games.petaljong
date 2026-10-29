# CODEX 34 human playtest — awaiting observations

No human playtest has been performed for v9. Do not treat simulated wins as player feedback.
The current APK predates this work. Run this protocol after a fresh compatible Gradle APK has passed host verification.

Use a separate test profile/device with existing test storage instrumentation; never replace the player's real progress, uninstall, or clear app data. In the shipping forward-only profile, observe each tier as it is naturally reached. Do not add level skipping to production.

Compare Normal 4/7, Hard 8/12/15, Very Hard 16/23/30, and Expert 31/40/50/75/101. Include a level whose qualityTargetMet is false so the limitation can be assessed. Record the generator version, layout, seed, selections, tray sizes before each decision, elapsed active play time, losses, Hint/Undo usage, and whether identities were visible before the choice. Keep a separate familiarization round. Alternate tier order across testers where test storage permits it; do not always show Expert last.

Ask after every board, using 1–5 ratings and a short explanation:

1. Did the puzzle require planning? Describe a choice where you looked ahead.
2. Were matching pairs too obvious?
3. Did tile order matter? Which move changed the outcome?
4. Was the four-slot tray challenging to manage?
5. Were losses understandable? Could you explain the cause before retrying?
6. Did the puzzle feel fair? Did a choice require guessing a covered symbol?
7. Was the level enjoyable?
8. Would you play another level?

Ask the player to think aloud at two- and three-symbol tray states without coaching a move. Distinguish a visible dependency mistake from an unavoidable unknown reveal. Record actual comments verbatim with consent; leave response fields empty until observed.

| Level/version | Planning | Obvious pairs | Order mattered | Tray challenge | Understandable losses | Fairness | Enjoyment | Another level | Observed choice / hidden information |
|---|---|---|---|---|---|---|---|---|---|
| Awaiting playtest | | | | | | | | | |

Tune against observations: if players still repeatedly clear obvious pairs, inspect those exact action traces and rerun shallow probes. If losses hinge on an unseen identity, reduce that dependency even when the automated score is high. If all choices feel punishing, prefer candidates with several proved safe continuations rather than rewarding losses. Preserve this record, including negative feedback.

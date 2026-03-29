# PLAN.md — Task 6.3: "Quick Glance" Map UI

## Objective
Make map pins visually distinguish Verified vs. Unverified events at a glance.

---

## Key Constraint: SymbolManager Bitmap Architecture

The app renders pins as **pre-rasterized Bitmaps** added to the MapLibre style (`style.addImage()`)
and drawn via `SymbolManager`. Per-symbol opacity is NOT natively supported through the annotations
plugin — it only provides `withIconSize()`, `withIconImage()`, etc.

**Solution:**
For each event type, pre-render **two** bitmap variants at style setup time:
- **`pin-{type}`** — Full opacity (verified / high trust)
- **`pin-{type}-faded`** — `alpha = 0.4` (unverified / low trust)

At symbol creation time in `updateSymbols()`, pick the correct image key based on the event's
current `TrustScore.isVerified` flag. When trust data updates (DB → Flow → ViewModel), trigger
`updateSymbols()` again, which redraws all pins.

---

## Architecture: Trust Data Flow to Map

### Problem
`updateSymbols()` runs outside Compose — it's a private Kotlin function called from a
`LaunchedEffect`. It cannot directly consume a `StateFlow` inside `LaunchedEffect` easily when
mixing with `uiState`.

### Solution: Embed trust scores in `MapUiState`

1. **`MapViewModel`**: Inject `VerificationRepository`. Build a new combined flow that maps
   each event to its `TrustScore`, emitting `Map<String, TrustScore>` (keyed by `eventId`).
   Add `val trustScores: Map<String, TrustScore>` to `MapUiState`.

2. **`MapScreen.kt` `LaunchedEffect`**: React to both `uiState.events` AND `uiState.trustScores`
   changes to re-invoke `updateSymbols()`.

3. **`updateSymbols()`**: Add `trustScores: Map<String, TrustScore>` parameter.
   Inside the function, look up each event's trust score and pick `pin-{type}` or `pin-{type}-faded`.

---

## Bitmap Rendering: Faded Variant

A helper `createFadedPinBitmap(bitmap: Bitmap, alpha: Int): Bitmap` applies pixel-level alpha
multiplication to create the 40% opacity version without a second drawable inflate:

```kotlin
private fun createFadedBitmap(src: Bitmap, alpha: Int): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint().apply { this.alpha = alpha }  // 102 ≈ 0.4 * 255
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
}
```

---

## Layer Ordering

The proximity circle layer (`proximity-layer`) is already added via `style.addLayer()` BEFORE
symbols are created by `SymbolManager`. The `SymbolManager` always adds its layer on top.
**No changes needed for layer ordering.**

---

## Files to Modify

| Action   | File                                               |
|----------|----------------------------------------------------|
| [MODIFY] | `ui/screens/map/MapViewModel.kt` — add trust flow to `MapUiState` |
| [MODIFY] | `ui/screens/map/MapScreen.kt` — faded bitmap registration, `LaunchedEffect` trigger, `updateSymbols` signature update |

---

## Verification Plan
1. `./gradlew assembleDebug` — zero errors
2. Events with 0 verifications appear faded (40% opacity)
3. Events after 3+ confirms at >= 80% appear at full opacity
4. Proximity circle remains below all pins
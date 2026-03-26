# PLAN: Safe Offline Style Reversion

## Goal
Restore the offline map rendering by removing missing font dependencies (glyphs) that are causing the map loading pipeline to fail.

## Proposed Changes

### 1. Update `style-offline.json` [MODIFY]
- **File**: `app/src/main/assets/style-offline.json`
- **Action 1**: Completely remove the `"glyphs": "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf"` line from the root JSON object.
- **Action 2**: Within the `place_labels` and `road_labels` layer configurations, remove the `"text-field"` and `"text-font"` properties from the `layout` objects. This prevents MapLibre from attempting to rasterize missing fonts while preserving the layer structure for future integration when local font PBFs are supplied.

### 2. Verify `MapScreen.kt` Diagnostic Listeners [REVIEW]
- **File**: `app/src/main/java/com/example/pigeon/ui/screens/map/MapScreen.kt`
- **Action**: Ensure that `this@apply.addOnDidFailLoadingMapListener` and `map.addOnStyleImageMissingListener` are correctly configured to log (`Log.e` and `Log.w` respectively) any future rendering failures exactly to Logcat.

## Verification Plan
1. User approves the plan.
2. Coder executes the JSON and Kotlin changes.
3. User syncs and runs the project to verify the map shape rendering returns successfully in offline mode.
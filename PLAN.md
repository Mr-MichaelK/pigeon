# Implementing Proximity Radius Visualizer

## Objective
Implement a reactive 500m GeoJSON circle rendering around the user's GPS coordinates, ensuring correct styling and Z-index layering.

## Proposed Strategy

### 1. Geometry Generation
- **File:** `MapScreen.kt` or `LocationUtils.kt`
- Add a helper function `createGeoJsonCircle(center: LatLng, radiusInMeters: Double, points: Int = 64): Polygon`.
- It will calculate the Earth's curvature math (Haversine/Spherical) to plot 64 boundary points around the user.

### 2. Layer & Style Registration
- **File:** `MapScreen.kt` (in `setupMapStyle` block)
- Modify the existing `"proximity-layer"`.
- Replace the current gold visualizer with two new layers:
  - `proximity-fill-layer` (FillLayer): Color `#00FF00` at `0.10` alpha.
  - `proximity-line-layer` (LineLayer): Color `#00FF00` at `0.50` alpha, width `2f`.
- Append both to the style *before* `SymbolManager` initialization to prevent them from intercepting pin clicks.

### 3. Reactive State Updates
- **File:** `MapScreen.kt`
- Add a `LaunchedEffect(uiState.userLoc)` that recalculates the `Polygon` using the helper function.
- Fetch the `"proximity-source"` as `GeoJsonSource` and update it dynamically via `source.geometry(Feature.fromGeometry(polygon))`.
- MapLibre will natively handle redrawing the new circle bounds smoothly.

I'll wait for your permission (Proceed / Plan approved) to implement this.
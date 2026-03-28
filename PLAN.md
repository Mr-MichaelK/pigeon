# PLAN.md: Proximity Validation Visuals

## Objective
Update the visual representation of the verification radius in `MapScreen.kt` to use a true geographic polygon rather than a pixel-based circle layer, ensuring perfect scaling at all zoom levels.

## Files to Modify
1. `app/src/main/java/com/example/pigeon/ui/screens/map/MapScreen.kt`

## Logic Patterns & Implementation Steps

### 1. Generating the Polygon (64 points)
We will create a helper function `createRadiusPolygon(center: LatLng, radiusInMeters: Double, points: Int = 64): String`. 
This function will:
- Calculate 64 coordinate pairs forming a circle around the center point.
- Construct and return a raw GeoJSON string representing a `Feature` with a `Polygon` geometry.

### 2. Style Injection (addSource & addLayer)
Inside the map style initialization block (`setupMapStyle` or initial setup):
- Add a `GeoJsonSource` with ID `"proximity-source"`.
- Add a `FillLayer` with ID `"proximity-layer"`, referencing `"proximity-source"`.
- Set the layer properties to a soft blue: `fillColor("rgba(0, 122, 255, 0.27)")` (equivalent to `0x44007AFF`).
- Place the layer below text labels and pins.

### 3. Continuous Updates
Use a `LaunchedEffect(userLocation)` to observe location changes.
- When `userLocation` changes, call `createRadiusPolygon` with a 500.0m radius (or the configured radius).
- Retrieve the `"proximity-source"` from the map style and update its GeoJSON data with the newly generated polygon string.

## Next Step
Wait for the user to say "Plan approved" or "Proceed" before moving to the Coder phase.
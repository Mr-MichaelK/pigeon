# PLAN: Task 2.1 - MapLibre Integration

## Goal
Integrate MapLibre Native SDK for offline situational awareness and implement the "Top Pill" coordinate overlay.

## Proposed Changes

### 1. Build & Manifest
- **libs.versions.toml**: 
    - Add `maplibre = "11.5.2"`.
- **app/build.gradle.kts**:
    - Add `maplibre-android` dependency.
- **AndroidManifest.xml**:
    - Add `INTERNET`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`.

### 2. Domain Layer
- **[NEW] MapMetadata.kt**: Domain model for viewport state (lat, long, zoom).

### 3. Application Layer
- **PigeonApplication.kt**: Initialize MapLibre in `onCreate`.

### 4. UI Layer (Map Screen)
- **[NEW] MapScreen.kt**:
    - Implement `MapView` using `AndroidView` with strict lifecycle management (onCreate, onStart, etc.).
    - Use "Tactical Sand" theme for the layout shell.
- **[NEW] MapViewModel.kt**:
    - Expose `MapUiState`.
    - Handle `onMapMoved` to update metadata.
- **[NEW] LatLongPill.kt**: 
    - A floating overlay showing current decimal coordinates.
    - Located in `com.example.pigeon.ui.screens.map.components`.

## Logic Patterns
- **Lifecycle Safety**: The `MapView` requires manual delegation of all lifecycle events to prevent crashes on rotation/exit.
- **Offline Reliability**: For this phase, we use a remote demo style, but architect the system to easily swap to local `.mbtiles`.

## Verification Plan (User to Run)
1. **Build Check**: Sync and compile the app.
2. **UI Check**: Navigate to the Map screen.
3. **Behavior**: Verify that panning the map updates the Lat/Long pill in real-time.
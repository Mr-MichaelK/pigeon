# PLAN: Task 2.1 Refinement & Task 2.2 - Map Pins & Geolocation

## Goal
Populate map with mock events, enable geolocation awareness, and implement UI placeholders for the "Situational Awareness" dashboard.

## Proposed Changes

### 1. Data Layer
- **MapViewModel.kt**:
    - Inject `EventRepository`.
    - In `init`, call `eventRepository.populateMockData()` if needed.
    - Collect `getAllEvents()` and expose as `List<Event>` in `MapUiState`.

### 2. UI Layer (Map Screen)
- **MapScreen.kt**:
    - Add logic to handle `ACCESS_FINE_LOCATION` permission check.
    - Initialize MapLibre's `LocationComponent` to show the user's pulse.
    - Implement `Marker` rendering logic: loop through `uiState.events` and add markers to the map.
    - Center camera on user location if available, otherwise default to Beirut.
- **LatLongPill.kt**: (Minor) Ensure it reflects the real-time camera target.

### 3. UI Placeholders (Stich UI)
- **MapScreen.kt Refresh**:
    - **Header**: Add a `ConnectivityBar` placeholder showing "Mesh: Passive" and "Synced: Just now".
    - **FAB**: Add a "Report" Floating Action Button with a gold (`#DF9C20`) background.
    - **Bottom Sheet**: Add a generic `EventDetailSheet` placeholder that appears when a marker is clicked.

### 4. Emulator Geolocation (User Instruction)
To set the emulator to Beirut:
1. Click the **three dots (...)** on the Emulator control panel.
2. Navigate to **Location**.
3. Search for "Beirut" or enter `33.8938, 35.5018`.
4. Click **Set Location**.

## Logic Patterns
- **Permission Flow**: Use `rememberPermissionState` (if library available) or standard `ActivityResultLauncher` to handle location.
- **Marker Cleanup**: Ensure markers are cleared/updated when the list changes.

## Verification Plan
1. **Launch App**: Verify the map opens in Lebanon (Beirut).
2. **Pins**: Observe custom markers appearing around Beirut.
3. **Pill**: Verify current Lat/Long updates as the camera moves.
4. **Placeholders**: Confirm the Report FAB and Connectivity Bar are visible.
# PLAN: Launch Refinement & Data Persistence (Task 2.13)

Transition the app from a mock-heavy development state to a persistent, user-location-focused utility.

## 1. Data Logic Cleanup
- **`MapViewModel.kt`**:
    - Remove `eventRepository.populateMockData()` from the `init` block to prevent automatic data generation on every map launch.
- **`EventLogViewModel.kt`**:
    - Remove `eventRepository.populateMockData()` from the `init` block.
    - Ensure `loadEvents()` (or equivalent reactive flow) is properly initiated to show existing data from the database.

## 2. Map Launch Experience
- **`MapScreen.kt`**:
    - Implement a "First-Time Zoom" logic:
        - Add a state variable `var hasInitialZoomed by remember { mutableStateOf(false) }`.
        - Inside a `LaunchedEffect(mapLibreMap)` or within the location update listener:
            - If `!hasInitialZoomed` and `locationComponent.lastKnownLocation` is available:
                - Animate the camera to the user's current coordinates.
                - Set `hasInitialZoomed = true`.

## 3. Event Log Data Flow
- **`EventLogViewModel.kt`**:
    - Refactor `uiState` to be a pure reactive `StateFlow` combining filters and the repository's event flow. This ensures it always reflects the current database state without manual triggers.

## 4. Verification Plan
- **Manual Verification**:
    - **Data Persistence**: Open the app, create a report, close the app totally, and reopen. Verify the report still exists and no *new* random events are created.
    - **Auto-Zoom**: Clear app data or move location in emulator, then launch app. Verify the map automatically centers on the current "blue dot" location.
    - **Event Log**: Confirm that manual reports appear in the log screen immediately.
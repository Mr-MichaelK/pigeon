# PLAN.md: Task 5.6 - Event Log Proximity Lock

## Objective
Enforce the 500m proximity verification rule on the Event Log screen, preventing users from resolving incidents they are not physically close to.

## Files to Modify
1. `[MODIFY]` [EventLogViewModel.kt](file:///Users/malekbaghdadi/Downloads/pigeon/app/src/main/java/com/example/pigeon/ui/screens/log/EventLogViewModel.kt)
2. `[MODIFY]` [EventLogScreen.kt](file:///Users/malekbaghdadi/Downloads/pigeon/app/src/main/java/com/example/pigeon/ui/screens/log/EventLogScreen.kt)

## Logic Patterns

### 1. EventLogViewModel.kt (State Bridge)
- **UI State**: Add `val userLocation: LatLng? = null` to `EventLogUiState`.
- **Action**: Implement `onUserLocationChanged(location: LatLng)` to update the state.
- **Verification Refinement**: Since the resolve action is a simple repository call, the UI will handle the gating via the `userLocation` state.

### 2. EventLogScreen.kt (Location Tracking & UI Gating)
- **Location Listener**: Use a `LaunchedEffect` to initialize MapLibre's `LocationEngine` (independent of MapView) and feed location updates to the ViewModel at 1Hz.
- **EventLogItem Updates**:
    - Pass `uiState.userLocation` to each `EventLogItem`.
    - Inside `EventLogItem`, use `LocationUtils.isWithinRange` to calculate `isWithinRadius`.
    - **UI Feedback**:
        - Disable the "MARK RESOLVED" button if `!isWithinRadius`.
        - Change button text to "TOO FAR" when disabled.
        - Add a small distance indicator (e.g., "600m away") next to the button for clarity.

## Verification Plan
1. **Mock Testing**: Verify the resolve button toggle by simulating location changes.
2. **Naming Consistency**: Ensure `isWithinRadius` is the logical gating factor used for the resolve action.
3. **UX Balance**: Ensure the distance indicator is subtle but legible.

## Next Step
Wait for user approval of PLAN.md.
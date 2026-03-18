# PLAN: Multi-Step Reporting Wizard (Task 2.3)

Implement a robust, tactical reporting flow with type selection, detailed inputs, and TTL management.

## 1. Model Refactoring
- **`Event.kt`**:
    - Update `EventType` enum: `FIRE`, `MEDICAL`, `SUPPLIES`, `CONFLICT`, `CUSTOM`.
- **`MapScreen.kt`**:
    - Refactor `updateSymbols` to map new `EventType` values to their specific vector assets:
        - `FIRE` -> `local_fire_department_24dp`
        - `MEDICAL` -> `medical_services_24dp`
        - `SUPPLIES` -> `package_2_24dp`
        - `CONFLICT` -> `warning_24dp`
        - `CUSTOM` -> `location_on_24dp`
    - Update `createTacticalPinBitmap` to load these drawables correctly.

## 2. Reporting Wizard UI (Stage 1: Selection)
- **New Component: `ReportingWizardSheet.kt`**:
    - Implement a `ModalBottomSheet` (or a custom overlay) with two stages.
    - **Stage 1 (Grid)**:
        - 2x2 grid (or similar) showing the 5 event types with icons and tactical styling.
        - Selection state management.
        - "Cancel" and "Next" buttons (Next enabled only when selected).
    - **Location Info**: Pulse icon with "Auto-filled" metadata (Lat/Long).

## 3. Reporting Wizard UI (Stage 2: Details)
- **Stage 2 (Inputs)**:
    - Non-editable display of selected type icon/name.
    - `OutlinedTextField` for "Event Title" and "Details".
    - `TTL Selection`: Toggle or chip group for `1h`, `6h`, `24h`.
    - "Cancel" and "Confirm Report" buttons.

## 4. Logic & Storage
- **`MapViewModel.kt`**:
    - Add `reportEvent(type, title, description, ttlMillis)` function.
    - Trigger `EventRepository.createEvent(...)`.
- **`MockDataGenerator.kt`**:
    - Update to use new `EventType` values to prevent runtime crashes.

## 5. Verification Plan
- **Unit Tests**:
    - Test `EventRepository` to ensure new event types are correctly persisted and retrieved.
    - Test `MapViewModel` logic for field validation (e.g., description required?).
- **Manual QA**:
    - Visual check of the wizard stages against tactical design specs.
    - Verify reported events appear immediately on the map with correct icons.
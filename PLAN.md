# PLAN: Tactical Event Detail Sheet (Task 2.14)

Unify the UI by reworking the Map's event details to match the `ReportingWizardSheet` aesthetic.

## 1. Component Implementation
- **[NEW] `EventDetailSheet.kt`**:
    - **Base**: `ModalBottomSheet` with `MeshColor.Background`.
    - **Sections**:
        - **Header**: Large Title + Status Badge (`LIVE` / `RESOLVED`).
        - **Identity Card**: Display `EventType` with its corresponding icon and color theme.
        - **Description**: Clear body text for the report details.
        - **Metadata Grid**: Tactical pills for:
            - **Location**: `45.1234° N, 34.5678° E`
            - **Origin**: `Device: ABC...123` (Truncated)
            - **Timeline**: `Created: 14:20` | `TTL: 6h remaining`
        - **Action**: A high-impact "RESOLVE INCIDENT" button.

## 2. Integration
- **`MapScreen.kt`**:
    - Remove the legacy `EventDetailSheet` implementation.
    - Import and use the new `EventDetailSheet` component.
    - Ensure the sheet is triggered when `selectedEvent` is non-null.
    - Link the "Resolve" action to `MapViewModel.onResolveEvent`.

## 3. Logic Refinements
- **Color Mapping**: Map `EventType` to specific colors (e.g., Red for Fire, Green for Supplies) to provide instant visual context.
- **Truncation**: Implement a utility to truncate long Device IDs for the tactical display.

## 4. Verification Plan
- **Manual Verification**:
    - Click on various incident pins (Fire, Medical, etc.) and verify the sheet renders all metadata correctly.
    - Confirm the "Resolve" button updates the event status and the sheet reflects the "RESOLVED" state.
    - Check the visual consistency between the reporting wizard and this detail sheet.
# PLAN.md — Task 8.2: Identity Lock & Edit Logic

## Objective
Implement a formal confirmation flow for identity changes and polish the "Dual-State" form behavior to ensure users are fully aware of the 72-hour lockout before committing changes.

## Proposed Changes

### UI State Management
#### [MODIFY] [ProfileViewModel.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/ui/screens/profile/ProfileViewModel.kt)
- Add `showSaveConfirmation: Boolean` to `ProfileUiState`.
- Add `onSaveClick()` function to trigger the confirmation dialog.
- Update `saveAndLockIdentity()` to dismiss the dialog and execute the save.

### UI Components
#### [MODIFY] [ProfileScreen.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/ui/screens/profile/ProfileScreen.kt)
- **Implement `SaveIdentityConfirmationDialog`**:
    - Use `AlertDialog` with the "Rugged" aesthetic.
    - **Title**: "CONFIRM IDENTITY BROADCAST"
    - **Message**: "Your identity will be broadcast to the mesh and LOCKED for 72 hours. You will not be able to change your role or name during this period. Proceed?"
    - **Confirm Button**: "BROADCAST & LOCK" (Gold)
    - **Dismiss Button**: "CANCEL" (outlined)
- **Update `ProfileScreen`**:
    - Display the dialog when `uiState.showSaveConfirmation` is true.
- **Polish `CountdownCard`**:
    - Ensure it is visually distinct and prominent when the profile is locked.

---

## Verification Plan
### Automated Tests
- `./gradlew assembleDebug` to ensure no UI regressions.

### Manual Verification
1. **Confirmation Flow**:
    - Unlock the profile using the debug button.
    - Edit a field (e.g., change Role).
    - Click "SAVE & LOCK IDENTITY".
    - Verify the **Confirmation Dialog** appears with the correct warning text.
    - Click "CANCEL" and verify no changes are saved.
    - Click "BROADCAST & LOCK" and verify the profile locks and the countdown starts.
2. **Dual-State Persistence**:
    - Restart the app while the profile is locked. Verify the read-only "Identity Card" view is still shown.
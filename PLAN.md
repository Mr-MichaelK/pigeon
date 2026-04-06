# PLAN: Task 8.5 - Visual Refinement

This plan outlines the "Rugged" aesthetic overhaul for the Pigeon app, focusing on high-contrast, tactical, and functional styling as per the "Stich UI" standards.

## Proposed Changes

### [Theme & Styling]
- **MeshTheme.kt**: 
  - Ensure `MeshColor` aligns with Tactical Sand (#F8F7F6) and Operational Gold (#DF9C20).
  - Explicitly use `MeshColor.TextPrimary` (#171511) for heavy borders.

### [Identity Components]
- **IdentityComponents.kt**:
  - **MeshRoleCard**: 
    - Selected Border: `3.dp` width using `MeshColor.TextPrimary` for a high-contrast "Heavy Border".
    - Font: Apply `fontFamily = FontFamily.Monospace` to the `role.title`.
    - Shape: Standardize on `RoundedCornerShape(12.dp)` for cards.
  - **MeshTextField**: Use `fontFamily = FontFamily.Monospace` for technical labels.
  - **MeshAnonymousToggle**: Add a reactive **Privacy Alert** underneath when `isAnonymous` is toggled ON, using `MeshColor.AlertOrange`.

### [Map Screen]
- **MapScreen.kt**:
  - **LatLongPill**: Update coordinate text to use `fontFamily = FontFamily.Monospace`.
  - **MeshHeader**: Update `MESH ACTIVE` and `SYNCED` labels to Monospace.
  - **Report Button**: Ensure high-contrast `MeshColor.EmergencyRed` or a new `SafetyOrange`.

### [Profile Screen]
- **ProfileScreen.kt**:
  - **ProfileHeader**: Apply Monospace to `user.nodeName`.
  - **MeshStatisticsSection**: Ensure all technical values (Syncs, Trust rating) use Monospace.
  - **Action Button**: Use `MeshColor.SuccessGreen` (Signal Green) for the final "SAVE & LOCK" button to differentiate it from "RETURN TO MAP".

## Verification Plan
### Automated Tests
- N/A (UI-only refinement)

### Manual Verification
- [ ] Audit Onboarding role cards for heavy borders and monospaced titles.
- [ ] Verify `MapScreen` coordinates and status labels use monospaced fonts.
- [ ] Confirm "Anonymous Mode" trigger provides a clear, high-contrast visual hint.
- [ ] Verify Profile "Save & Lock" button uses the Signal Green aesthetic.
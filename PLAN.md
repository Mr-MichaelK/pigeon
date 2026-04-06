# PLAN.md — Task 8.4: Data Flow & Repository Sync (Refined)

## Objective
Implement a reactive Navigation Drawer that serves as a global reflection of the user's "Tactical Identity". Changes made in the Profile screen must immediately synchronize with the Drawer Header across all sessions (Map, Radar, Log).

## Proposed Changes

### UI Layer — Navigation & Global State
#### [MODIFY] [NavGraph.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/ui/navigation/NavGraph.kt)
- **Implement ModalNavigationDrawer**:
    - Add `rememberDrawerState`.
    - Wrap the main `Scaffold` in `ModalNavigationDrawer`.
- **Create `MeshDrawerContent`**:
    - A navigation drawer composable that collects the `userRepository.getUser()` flow.
    - **Header**: Displays the large `IdentityAvatar`, `displayName`, and `nodeId` in a tactical card style.
    - **Links**: Navigation items for Map, Radar, Log, and Profile.

#### [MODIFY] [MapScreen.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/ui/screens/map/MapScreen.kt)
- **Update `MeshHeader`**:
    - Add an "Hamburger Menu" button to the left side.
    - Keep the right side focused on **Mesh Status** (as requested).
    - Clicking the menu button triggers `drawerState.open()`.

#### [MODIFY] [IdentityComponents.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/ui/components/IdentityComponents.kt)
- **Implement `MeshDrawerHeader(user: User?)`**:
    - A specialized version of the Profile header optimized for the side drawer width.
    - Follows the "Stich UI" standards (12dp corners, Tactical Sand background).

---

## Verification Plan
### Automated Tests
- `./gradlew assembleDebug` to verify the new drawer components compile and injection works.

### Manual Verification
1. **Drawer Identity Sync**:
    - Open the side drawer on the Map screen. Observe name/avatar.
    - Navigate to Profile via the bottom nav or drawer link.
    - Update your **Display Name** and **Gender** (Avatar).
    - Save and lock the identity.
    - Open the side drawer immediately.
    - **Expected**: The Drawer Header must reflect the new name and avatar without a manual refresh.
2. **Global Access**:
    - Verify the side drawer is accessible and synchronized on the **Radar** and **Log** screens.
3. **Mesh Status Priority**:
    - Verify that the Map Header correctly continues to show "Mesh Active" / "Synced" status independently of the drawer's identity display.
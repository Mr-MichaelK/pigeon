# PLAN.md — Task 7.5: Role Purge & Verification State

## Objective
Clean up the role system by removing the "Coordinator" role and implementing a 4-grid role selector. Additionally, introduce a "Verified Mesh Member" state with a toggle in the Profile screen and conditional trust indicators in the UI.

## Proposed Changes

### Data Layer & Schema
#### [MODIFY] [User.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/domain/model/User.kt)
- Add `val isVerified: Boolean = false` to the `User` data class.

#### [MODIFY] [UserEntity.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/data/local/entities/UserEntity.kt)
- Add `val isVerified: Boolean = false` to the `UserEntity`.
- Update `toDomain()` and `toEntity()` mappers.

#### [MODIFY] [PigeonDatabase.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/data/local/PigeonDatabase.kt)
- Increment version to `7`.
- Add `MIGRATION_6_7` to add the `isVerified` column to `user_profile`.

### Business Logic
#### [MODIFY] [TacticalRole.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/ui/screens/onboarding/TacticalRole.kt)
- Remove `Coordinator` and `Scout` roles.
- Ensure the list consists of only: `Civilian`, `First Responder`.

#### [MODIFY] [ProfileViewModel.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/ui/screens/profile/ProfileViewModel.kt)
- Add `onVerifiedToggle(Boolean)` to the view model state and logic.
- Ensure `isVerified` is saved/loaded correctly.

### UI Implementation
#### [MODIFY] [OnboardingScreen.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/ui/screens/onboarding/OnboardingScreen.kt)
- **MeshRoleSelector**: Replace the `DropdownMenu` with a `LazyVerticalGrid` (or a 2x2 custom grid) showing the 4 roles with icons/descriptions.
- **MeshProfileHeader**: Make the "Verified Node" badge conditional on `isVerified`.

#### [MODIFY] [ProfileScreen.kt](file:///Users/michael/AndroidStudioProjects/Pigeon/app/src/main/java/com/example/pigeon/ui/screens/profile/ProfileScreen.kt)
- Add a "VERIFIED MESH MEMBER" toggle in the `EditProfileView`.
- Show the "Verified" badge in the `ProfileHeader` if the flag is true.

---

## Verification Plan
### Automated Tests
- `./gradlew assembleDebug` to verify compilation.

### Manual Verification
1. **Role Purge**:
   - Open Onboarding/Profile.
   - Verify "Coordinator" is nowhere to be found.
   - Verify the 4 roles are: Civilian, First Responder, Scout, Utility/Tech.
2. **4-Grid Selector**:
   - Verify the role selector in Onboarding is now a grid instead of a dropdown.
3. **Verification State**:
   - Go to Profile -> Edit.
   - Toggle "Verified Mesh Member" ON and Save.
   - Verify the "Verified Node" badge appears in the Profile header and Onboarding summary (if accessible).
   - Toggle OFF and verify the badge disappears.
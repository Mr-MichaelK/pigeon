# Implementation Plan - Task 1.2: Tactical Onboarding & Identity Lock

This plan outlines the architecture for the onboarding flow and identity management UI using Jetpack Compose and Navigation.

## User Review Required

> [!IMPORTANT]
> A **72-hour lock** countdown will be implemented in the Profile screen. This logic depends on the `lastUpdatedTimestamp` stored in `UserEntity` (implemented in Task 1.1).

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///Users/michael/AndroidStudioProjects/pigeon/gradle/libs.versions.toml)
- Add Compose BOM, Navigation, and Hilt Navigation versions and libraries.

#### [MODIFY] [app/build.gradle.kts](file:///Users/michael/AndroidStudioProjects/pigeon/app/build.gradle.kts)
- Enable Compose `buildFeatures`.
- Add Compose and Navigation dependencies.

---

### UI Layer (Compose)

#### [NEW] [NavGraph.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/ui/navigation/NavGraph.kt)
- Define navigation routes: `Onboarding`, `Map`, `Profile`.
- Implement conditional `startDestination` logic based on user existence.

#### [NEW] [OnboardingScreen.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/ui/screens/onboarding/OnboardingScreen.kt)
- "Joining the Mesh" UI (Part 5):
    - Display Name input.
    - Gender Selection toggle (Male/Female).
    - Anonymous Mode toggle.
    - Tactical Role Selection grid (Grid of Role Cards).
    - "JOIN THE MESH" button with animation.

#### [NEW] [ProfileScreen.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/ui/screens/profile/ProfileScreen.kt)
- "Identity Profile" UI (Part 4):
    - Profile Header (Avatar based on gender, Verified Badge).
    - 72-Hour Lock Countdown Timer.
    - Editable fields (Name, Role, Gender).
    - "Save & Lock Identity" confirmation modal.

#### [NEW] [OnboardingViewModel.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/ui/screens/onboarding/OnboardingViewModel.kt)
- Business logic for onboarding:
    - Checking if profile exists.
    - Saving new user profile.
    - Generating the initial "Node Key" animation state.

#### [NEW] [ProfileViewModel.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/ui/screens/profile/ProfileViewModel.kt)
- Business logic for profile management:
    - Loading user profile.
    - Calculating countdown timer.
    - Handling identity lock confirmation.

---

### Theme & Styling

#### [NEW] [PigeonTheme.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/ui/theme/Theme.kt)
- Implement `STYLE_GUIDE.md` specifications:
    - Colors: #1C1C1C (Background), #E57373 (Primary), #81C784 (Secondary).
    - Shape: 8dp rugged corners.

## Verification Plan

### Automated Tests
- N/A for UI components (Manual verification preferred for "Look & Feel").
- Unit test `ProfileViewModel` for countdown logic.

### Manual Verification
- **Scenario 1: Fresh Install**: App starts on "Joining the Mesh". Roles selected, user joined, transitions to Map.
- **Scenario 2: Return User**: App starts directly on Map.
- **Scenario 3: Identity Change**: Try editing profile. Confirm lockout warning. Verify countdown timer appears.

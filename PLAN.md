# Implementation Plan - Task 1.1: Local Persistence (User Identity)

This plan outlines the architecture for local user identity persistence using Room, Hilt, and Clean Architecture patterns.

## User Review Required

> [!IMPORTANT]
> A **72-hour lock** logic is required for profile updates. We will store a `lastUpdatedTimestamp` in the `UserEntity` to facilitate this check in the UI layer.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///Users/michael/AndroidStudioProjects/pigeon/gradle/libs.versions.toml)
- Add Room, Hilt, and KSP versions and libraries.

#### [MODIFY] [build.gradle.kts (Project)](file:///Users/michael/AndroidStudioProjects/pigeon/build.gradle.kts)
- Add Hilt and KSP plugins to the top-level build file.

#### [MODIFY] [app/build.gradle.kts](file:///Users/michael/AndroidStudioProjects/pigeon/app/build.gradle.kts)
- Apply Hilt and KSP plugins.
- Add Room, Hilt, and Lifecycle dependencies.

---

### Data Layer

#### [NEW] [UserEntity.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/data/local/entities/UserEntity.kt)
- Define `UserEntity` with:
    - `id`: Int (1 as PK)
    - `displayName`: String
    - `role`: String
    - `nodeName`: String (Immutable, auto-generated hash)
    - `isAnonymous`: Boolean
    - `lastUpdatedTimestamp`: Long

#### [NEW] [UserDao.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/data/local/dao/UserDao.kt)
- Define Room DAO for `UserEntity` with `upsert` and `get` operations.

#### [NEW] [PigeonDatabase.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/data/local/PigeonDatabase.kt)
- Define the main Room database class.

#### [NEW] [LocalUserRepository.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/data/repository/local/LocalUserRepository.kt)
- Implementation of the repository interface using `UserDao`.

---

### Domain Layer

#### [NEW] [User.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/domain/model/User.kt)
- Domain model for the user profile.

#### [NEW] [UserRepository.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/domain/repository/UserRepository.kt)
- Interface definition for user data operations.

---

### Dependency Injection

#### [NEW] [DatabaseModule.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/di/DatabaseModule.kt)
- Hilt module to provide Room database and DAOs.

#### [NEW] [RepositoryModule.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/di/RepositoryModule.kt)
- Hilt module to bind `UserRepository` to `LocalUserRepository`.

#### [NEW] [PigeonApplication.kt](file:///Users/michael/AndroidStudioProjects/pigeon/app/src/main/java/com/example/pigeon/PigeonApplication.kt)
- Application class annotated with `@HiltAndroidApp`.

## Verification Plan

### Automated Tests
- Create `UserDaoTest` to verify Room operations.
- Run using: `./gradlew test` (Unit tests for Room if using an in-memory database) or `./gradlew connectedAndroidTest`.

### Manual Verification
- N/A for this task as it's purely data layer. Verification will be done via unit tests and integration in Task 1.2.

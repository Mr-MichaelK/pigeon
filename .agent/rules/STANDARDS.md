# Pigeon Engineering Standards

## 1. Technical Stack Constraints
* **Language:** Kotlin 2.0+ with Strict Typing.
* **UI Framework:** Jetpack Compose (Material 3).
* **Concurrency:** Kotlin Coroutines & Flow (No RxJava).
* **Dependency Injection:** Hilt (preferred) or Koin.
* **Database:** Room Persistence Library with SQLite.

## 2. Architecture Rules (MVVM + Clean)
* **Packages:** Strictly follow `data`, `domain`, and `ui` separation.
* **Logic Placement:** 
    * UI must only observe State.
    * Business logic must reside in UseCases (Domain layer).
    * Data fetching/persistence must be hidden behind Repository interfaces.
* **Immutability:** Data models must be `data class` with `val` properties only.

## 3. P2P & Battery Standards (The "Pigeon" Specifics)
* **Radio Usage:** No network calls outside of the `NearbyConnections` service.
* **Race to Sleep:** Code must prioritize fast data exchange followed by immediate radio shutdown.
* **Sync Logic:** All synchronization must use the "Set Union" approach—never overwrite local data with remote data; only append missing IDs.
* **Event-Driven Sync:** State transitions from Passive to Active must be triggered by local database changes (new reports) or system-level opportunistic pings. Manual user-triggered pings are deprecated.

## 4. UI/UX Rules
* **Offline-Default:** Every screen must be functional without an internet connection.
* **Stress-UX:** Use large touch targets (min 48dp) and high-contrast colors for emergency conditions.
* **State:** Use `collectAsStateWithLifecycle()` to prevent battery drain when the app is in the background.

## 5. Review Criteria (For the Reviewer Agent)
* **Reject** code that introduces 3rd-party dependencies not mentioned in the ARCHITECTURE.md.
* **Reject** code that uses GlobalScope for Coroutines.
* **Reject** UI changes that don't match the "Rugged/Utility" aesthetic described in SCREENS.md.
* **Reject** any UI implementation that adds "Ping" or "Sync" buttons to the Radar or Map screens. Synchronization must remain an autonomous background concern of the Networking Layer.
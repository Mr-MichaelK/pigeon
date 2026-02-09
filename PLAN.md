# PLAN: Task 2.1 & 2.2 - The Event Ledger & Mock Data

## 1. Domain & Data Layer
* **Entity:** Create `EventEntity.kt` in the `data` layer. 
    * Fields: `eventId` (UUID), `creatorDeviceId` (String), `eventType` (Enum), `title` (String), `description` (String), `latitude/longitude` (Double), `timestamp` (Long), `isResolved` (Boolean), `ttl` (Long).
* **DAO:** Implement `EventDao.kt` with queries for:
    * `getAllEvents()` (Flow)
    * `getNearbyEvents(lat, lon, radius)`
    * `searchEvents(query)`
* **Repository:** `EventRepository.kt` and `EventRepositoryImpl.kt` to handle data fetching.
* **Mock Data:** Create `MockDataGenerator.kt` to populate the DB with 10-15 sample events located in the Lebanon region.

## 2. UI Layer (Compose)
* **Screen:** `EventLogScreen.kt`.
* **Components:**
    * **Timeline Rail:** A vertical line UI element connecting list items to represent the flow of time.
    * **Filter Row:** `FilterChip` components for "All", "Unresolved", "Resolved", and "Nearby".
    * **Search Bar:** A top-anchored search bar for text, type, and device ID queries.
    * **Event Card:** High-contrast cards matching "Stich" aesthetics (#F8F7F6 background, 12dp corners).
* **ViewModel:** `EventLogViewModel.kt` to manage UI State (Loading, Success, Empty) and filtering logic.

## 3. Logic Patterns
* **Mesh Pulse:** Implement a small UI indicator in the header that "pulses" green to simulate a sync window.
* **Immutability:** Ensure the UI only displays data and triggers a "Resolve" action by creating a new status update (not editing the original event).
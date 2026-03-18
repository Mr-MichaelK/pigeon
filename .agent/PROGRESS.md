# Project Pigeon: Progress Tracker

## Current Sprint: UI Skeleton & Mock Data (No Network)

---

## 🟢 PHASE 0: Context & Architecture (COMPLETED)
- [x] Technical Architecture defined (Set Union, 3-State Power)
- [x] Multi-Agent System Prompts & Engineering Standards established
- [x] UI/UX Screen Specifications finalized

## 🟡 PHASE 1: Identity & Gateway (IN PROGRESS)
### Task 1.1: Local Persistence (User Identity)
- [x] **Architect:** Define Room Entity for User Profile including the 72-hour lock timestamp.
- [x] **Coder:** Implement Room database setup and LocalUserRepository.
- [x] **Reviewer:** Verify data models match the "Trust Identity" requirements in `SCREENS.md`.

### Task 1.2: Tactical Onboarding & Identity Lock
- [x] **Architect:** Plan the conditional Start Destination (Onboarding vs. Map).
- [x] **Coder:** Build the "Joining the Mesh" screen and the Profile screen with the 72-hour countdown timer.
- [x] **Reviewer:** Audit UI for "Rugged/Utility" aesthetic and check "Anonymous Mode" logic.

### Task 1.3: Stich UI Refinement
- [x] **Coder:** Refine the Onboarding and Identity UI to match the "Stich" design reference.
- [x] **Reviewer:** Verify adherence to Pigeon Visual Standards and Stich aesthetics.

### Task 1.4: Core Screen Skeleton
- [x] **Coder:** Create placeholder screens for Map, Event Log, and Radar.
- [x] **Architect:** Define navigation logic and individual screen view states.

### Task 1.5: Navigation Architecture
- [x] **Coder:** Implement the Bottom Navigation Bar and the flow between core screens.
- [x] **Reviewer:** Ensure smooth transitions and correct backstack handling.

## 🟡 PHASE 2: Spatial Awareness (Mock Data)
### Task 2.1: MapLibre Integration
- [x] **Architect:** Design the MapView container and Plan implementation.
- [x] **Coder:** Integrate MapLibre SDK and implement the Top Pill (Lat/Long display).
- [x] **Reviewer:** Ensure map components are optimized for offline-default rendering.

### Task 2.2: The Event Ledger (Mocked)
- [x] **Architect:** Define the `Event` Room Entity (Immutable) and a `MockDataGenerator`.
- [x] **Coder:** Implement the Event Log Screen and the Map Event Pins using static mock events.
- [x] **Reviewer:** Confirm the "Timeline Rail" in the Log screen matches the UX spec.

### Task 2.3: Multi-Step Reporting Wizard
- [x] **Architect:** Design the state machine for the 3-step reporting flow.
- [x] **Coder:** Build the Reporting Bottom Sheet. Note: Broadcast action should only update local DB for now.
- [x] **Reviewer:** Test "Two-Tap" efficiency for reporting under stress.

## 🟢 PHASE 3: Peer Discovery UI (Radar Only) (COMPLETED)
### Task 3.1: Visual Radar & Peer Discovery
- [x] **Architect:** Design the Radar Canvas logic using RSSI distance math.
- [x] **Coder:** Implement the Visual Radar and Peer List with placeholder "Nearby Peers."
- [x] **Reviewer:** Audit Radio Hygiene (OFF state) and Canvas performance.

## 🔴 PHASE 4: Networking & Real-Time Sync (FUTURE)
### Task 4.1: Sync/Exchange Layer (Google Nearby Connections)

- [] **Architect:** Design the NearbySyncManager to handle P2P_CLUSTER topology and multi-peer discovery.

- [] **Coder:** Implement the Google Nearby Connections wrapper for BLE (Discovery/Advertising) and Wi-Fi Direct (High-speed transfer).

- [] **Coder:** Integrate Protocol Buffers (Protobuf) for compact binary serialization of Event and Manifest payloads.

- [] **Reviewer:** Audit "Radio Hygiene" to ensure Wi-Fi Direct is only invoked for payloads > 32KB.

### Task 4.2: The "Set Union" Engine (Delta Exchange)

- [] **Architect:** Define the Manifest Exchange Protocol (comparing lists of unique Event IDs and hashes between nodes).

- [] **Coder:** Implement the Set Union Algorithm using Room's EXCEPT or NOT IN queries to identify missing "deltas".

- [] **Coder:** Build the Delta Transfer logic to specifically request and append missing records to the local immutable ledger.

- [] **Reviewer:** Verify that incoming data is appended as new records and never overwrites existing local data (Eventual Consistency).

### Task 4.3: Control Layer (3-State Power & "Ping" Logic)

- [] **Architect:** Formalize the State Machine transitions: OFF ↔ PASSIVE (BLE) ↔ ACTIVE (Wi-Fi).

- [] **Coder:** Implement the Event-Driven "Ping": automatically trigger a broadcast when a new report is saved to the local DB.

- [] **Coder:** Implement the "Wake-up" Frame logic and the 60-second "Race to Sleep" timer to return the device to a passive state after sync.

- [] **Reviewer:** Conduct battery-drain testing to ensure background scanning stays below the 5% per hour target.

### Task 4.4: Data Lifecycle & Integrity

- [] **Architect:** Design the Purge Controller logic for automatic cleanup based on user-defined TTL (Time-to-Live).

- [] **Coder:** Implement the WorkManager background task to monitor and delete expired events from the Room DB.

- [] **Coder:** Implement the Sidecar Verification logic, allowing nodes to "Verify" or "Contradict" events via signed delta records.

- [] **Reviewer:** Ensure the 72-hour Identity Lock correctly prevents profile changes from affecting existing mesh signatures.

### Task 4.5: Multi-Device Mesh Validation

- [] **Coder:** Implement RSSI-based distance math to populate the Radar UI with real-time peer proximity.

- [] **Reviewer:** Perform Field/Mesh Testing with at least three physical devices to simulate opportunistic "Store-and-Forward" propagation.
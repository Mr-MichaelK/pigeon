# Project Pigeon: Progress Tracker

## Current Sprint: UI Skeleton & Mock Data (No Network)

---

## 🟢 PHASE 0: Context & Architecture (COMPLETED)
- [x] Technical Architecture defined (Set Union, 3-State Power)
- [x] Multi-Agent System Prompts & Engineering Standards established
- [x] UI/UX Screen Specifications finalized

## � PHASE 1: Identity & Gateway (COMPLETED)
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

## � PHASE 2: Spatial Awareness (Mock Data) (COMPLETED)
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

## 🟢 PHASE 4: Networking & Real-Time Sync (COMPLETED)
### Task 4.1: Sync/Exchange Layer (Google Nearby Connections)

- [x] **Architect:** Design the NearbySyncManager to handle P2P_CLUSTER topology and multi-peer discovery.

- [x] **Coder:** Implement the Google Nearby Connections wrapper for BLE (Discovery/Advertising) and Wi-Fi Direct (High-speed transfer).

- [x] **Coder:** Integrate Protocol Buffers (Protobuf) for compact binary serialization of Event and Manifest payloads.

- [x] **Reviewer:** Audit "Radio Hygiene" to ensure Wi-Fi Direct is only invoked for payloads > 32KB.

### Task 4.2: The "Set Union" Engine (Delta Exchange)

- [x] **Architect:** Define the Manifest Exchange Protocol (comparing lists of unique Event IDs and hashes between nodes).

- [x] **Coder:** Implement the Set Union Algorithm using Room's EXCEPT or NOT IN queries to identify missing "deltas".

- [x] **Coder:** Build the Delta Transfer logic to specifically request and append missing records to the local immutable ledger.

- [x] **Reviewer:** Verify that incoming data is appended as new records and never overwrites existing local data (Eventual Consistency).

### Task 4.3: Control Layer (3-State Power & "Ping" Logic)

- [x] **Architect:** Formalize the State Machine transitions: OFF ↔ PASSIVE (BLE) ↔ ACTIVE (Wi-Fi).

- [x] **Coder:** Implement the Event-Driven "Ping": automatically trigger a broadcast when a new report is saved to the local DB.

- [x] **Coder:** Implement the "Wake-up" Frame logic and the 60-second "Race to Sleep" timer to return the device to a passive state after sync.

- [x] **Reviewer:** Conduct battery-drain testing to ensure background scanning stays below the 5% per hour target.

### Task 4.4: Data Lifecycle & Integrity

- [x] **Architect:** Design the Purge Controller logic for automatic cleanup based on user-defined TTL (Time-to-Live).

- [x] **Coder:** Implement the WorkManager background task to monitor and delete expired events from the Room DB.

- [x] **Coder:** Implement the Sidecar Verification logic, allowing nodes to "Verify" or "Contradict" events via signed delta records.

- [x] **Reviewer:** Ensure the 72-hour Identity Lock correctly prevents profile changes from affecting existing mesh signatures.

### Task 4.5: Multi-Device Mesh Validation

- [x] **Coder:** Implement RSSI-based distance math to populate the Radar UI with real-time peer proximity.

- [x] **Reviewer:** Perform Field/Mesh Testing with at least three physical devices to simulate opportunistic "Store-and-Forward" propagation.

## 🟢 PHASE 5: Proximity-Based Sidecar Verification (COMPLETED)
### Task 5.1: Spatial Trust Logic (Haversine Implementation)
- [x] **Architect:** Define the `calculateDistance` utility interface for spherical geometry.
- [x] **Coder:** Implement the Haversine formula in Kotlin to return distance in meters.
- [x] **Reviewer:** Unit test logic against GPS benchmarks to ensure verification precision.

### Task 5.2: Visual Trust Zone (Map Overlay)
- [x] **Architect:** Define the GeoJSON coordinate math for a 64-point circular polygon.
- [x] **Coder:** Implement the `proximity-source` and `proximity-layer` (FillLayer) in MapLibre.
- [x] **Reviewer:** Audit UI to ensure the radius overlay doesn't obstruct map labels.

### Task 5.3: Verification Gating & State Bridge
- [x] **Architect:** Design the state-flow between MapScreen (Location) and EventDetailSheet.
- [x] **Coder:** Implement `isWithinRadius` check and pass it to UI components.
- [x] **Reviewer:** Verify real-time toggle of verification status as the user moves.

### Task 5.4: Sidecar UI & Interaction Constraints
- [x] **Architect:** Design the visual language for "Out of Bounds" interaction.
- [x] **Coder:** Update the Event Detail Sidecar to disable the "RESOLVE" button and display a "Too Far to Verify" proximity warning.
- [x] **Reviewer:** Ensure UX clarity—users must understand why they are barred from resolving specific incidents.

### Task 5.5: Dynamic Anchor & Sync
- [x] **Architect:** Plan the update frequency for the radius source (1Hz).
- [x] **Coder:** Bind the radius polygon source to `LocationEngine` for smooth updates.
- [x] **Reviewer:** Performance check for frame drops during map movement.
 
 ### Task 5.6: Logs Screen Proximity Lock
 - [x] **Architect:** Centralize location management in `LocationRepository` for cross-screen state sharing.
 - [x] **Coder:** Update `EventLogViewModel` to consume shared location and gate the "MARK RESOLVED" action.
 - [x] **Reviewer:** Verify that distant incidents in the log display physical distance and lock resolution.
 
## � PHASE 6: Distributed Trust & Reputation (COMPLETED)
### Task 6.1: The Verification Data Model (Sidecar Entity)
- [x] **Architect:** Define the Verification Room Entity linked to EventID. Include fields for signerID, isConfirm (Boolean), and timestamp.
- [x] **Coder:** Implement the Many-to-One relationship. Ensure the Protobuf message for sync is optimized for "Delta-only" transfers.
- [x] **Reviewer:** Audit the database to ensure a user cannot submit multiple verifications for the same EventID (Unique constraint).

### Task 6.2: The Hybrid Consensus Engine
- [x] **Architect:** Define the 80% Rule: TrustScore = (TotalConfirms / (TotalConfirms + TotalContradicts)) * 100. isVerified = true if TrustScore >= 80 AND TotalConfirms >= 3.
- [x] **Coder:** Implement real-time trust calculation in the Repository layer using Room flows.
- [x] **Reviewer:** Test edge cases: Ensure events fail verification with < 3 confirmations even if 100% positive.

### Task 6.3: "Quick Glance" Map UI
- [x] **Architect:** Define visual states for Map Pins: Verified (Solid) vs. Unverified (Faded/Semi-transparent).
- [x] **Coder:** Implement pre-rasterized bitmap strategy for alpha-blended pins in MapLibre SymbolManager.
- [x] **Reviewer:** Ensure pins update instantly on the map when a sync completes via reactive StateFlow.

### Task 6.4: The Verification Sidecar UI
- [x] **Architect:** Design the EventDetailSheet layout to show the hybrid Consensus Score and peer count.
- [x] **Coder:** Implement the "DISTRIBUTED TRUST" section with Confirm/Contradict buttons and proximity locks.
- [x] **Reviewer:** Verify the "Resolve" button logic now requires a verified consensus state before allowing closure.

### Task 6.5: Mesh Propagation & Sync Integration
- [x] **Coder:** Update NearbySyncManager to broadcast VerificationMessages and relay them across the mesh.
- [x] **Reviewer:** Verify the full consensus loop—votes cast on one node reach all other nodes and update their local map pins.

### Summary of Phase 6: Distributed Trust & Reputation System
*   **Trust Model**: Implemented a local consensus engine that calculates trust scores (80% threshold) based on peer votes.
*   **Verification Sidecar**: Added a tactical UI overlay to `EventDetailSheet` that allows responders to confirm or contradict incidents with a single tap, enforced by a **500m Proximity Lock**.
*   **Mesh Propagation**: Integrated `VerificationMessage` into the P2P protocol, enabling real-time trust synchronization across all connected nodes.
*   **Reactive Pins**: Updated MapLibre symbol management to visually distinguish between unverified (40% alpha) and verified (100% alpha) incident markers, improving situational awareness.

## 🟡 PHASE 7: Onboarding & Identity Refinement (IN PROGRESS)
### Task 7.1: Expanded Identity Schema (Gender & Logic)
- [x] **Architect:** Update the UserProfile Room Entity to include gender (Enum: MALE, FEMALE, UNDISCLOSED) and nodeId (UUID).
- [x] **Coder:** Implement a ProfileViewModel logic that reactively switches the default avatar: if gender == MALE set to avatar_male, else avatar_female. Default to avatar_male for UNDISCLOSED.
- [x] **Reviewer:** Verify that the 72-hour lock correctly prevents gender/name flipping after the initial "Join Mesh" action.

### Task 7.2: UX/UI Cleanup & Placeholder Polish
- [x] **Coder:** Replace the "Alpha One" placeholder in the Display Name field with a context-neutral hint (e.g., "John Doe").
- [x] **Coder:** Remove the fixed, non-functional countdown timer from the Onboarding screen to reduce visual clutter for first-time users.
- [x] **Reviewer:** Audit the "Rugged/Utility" high-contrast colors for the new Gender selection radio-group/tabs.

### Task 7.3: Anonymous Mode & Privacy Indicators
- [x] **Architect:** Define the "Privacy Toggle" behavior—Anonymous mode should nullify the displayName in the broadcast payload but keep it in the local DB.
- [x] **Coder:** Implement a dynamic "Privacy Hint" beneath the name input: "Your name will be hidden; you will appear as 'Anonymous Civilian' to the mesh" when toggled ON.
- [x] **Reviewer:** Ensure the "Rugged" aesthetic is maintained—use a warning-style yellow/amber for the privacy indicator.

### Task 7.4: Automated Node Identity
- [x] **Architect:** Define the nodeId generation strategy: A unique, persistent String derived from a random UUID or Settings.Secure.ANDROID_ID.
- [x] **Coder:** Implement the auto-generation logic so the "Device Node Name" field is populated on first boot and set to read-only in the UI.
- [x] **Reviewer:** Confirm that the generated ID is short enough to be readable on the Radar and Peer List screens.

### Task 7.5: Role Purge & Verification State
- [x] **Architect:** Remove all references to the "Coordinator" role from the codebase, Enums, and UI layouts.
- [x] **Coder:** Implement conditional visibility for the "Verified Node" badge: Visibility = GONE by default. It must only appear if the local isVerified flag is true (post-threshold syncs).
- [x] **Coder:** Finalize the 4-Grid Role Selector: [Civilian, First Responder, Scout, Utility/Tech].
- [x] **Reviewer:** Perform a full "Onboarding-to-Map" walkthrough to ensure the transition is seamless and the "Rugged" UI feels cohesive.
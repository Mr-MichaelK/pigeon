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
- [ ] **Architect:** Design the MapView container and a mock Tile Provider (using Lebanon region).
- [ ] **Coder:** Integrate MapLibre SDK and implement the Top Pill (Lat/Long display).
- [ ] **Reviewer:** Ensure map components are optimized for offline-default rendering.

### Task 2.2: The Event Ledger (Mocked)
- [x] **Architect:** Define the `Event` Room Entity (Immutable) and a `MockDataGenerator`.
- [x] **Coder:** Implement the Event Log Screen and the Map Event Pins using static mock events.
- [x] **Reviewer:** Confirm the "Timeline Rail" in the Log screen matches the UX spec.

### Task 2.3: Multi-Step Reporting Wizard
- [ ] **Architect:** Design the state machine for the 3-step reporting flow.
- [ ] **Coder:** Build the Reporting Bottom Sheet. Note: Broadcast action should only update local DB for now.
- [ ] **Reviewer:** Test "Two-Tap" efficiency for reporting under stress.

## 🟢 PHASE 3: Peer Discovery UI (Radar Only) (COMPLETED)
### Task 3.1: Visual Radar & Peer Discovery
- [x] **Architect:** Design the Radar Canvas logic using RSSI distance math.
- [x] **Coder:** Implement the Visual Radar and Peer List with placeholder "Nearby Peers."
- [x] **Reviewer:** Audit Radio Hygiene (OFF state) and Canvas performance.

## 🔴 PHASE 4: Networking & Real-Time Sync (FUTURE)
- [ ] Implement Google Nearby Connections wrapper.
- [ ] Event-Driven "Ping" logic (Wake-up signal on broadcast).
- [ ] Set Union Algorithm for DB delta exchange.
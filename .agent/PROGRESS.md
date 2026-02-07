# Project Pigeon: Progress Tracker

## Current Sprint: UI Skeleton & Mock Data (No Network)

---

## 🟢 PHASE 0: Context & Architecture (COMPLETED)
- [x] Technical Architecture defined (Set Union, 3-State Power)
- [x] Multi-Agent System Prompts & Engineering Standards established
- [x] UI/UX Screen Specifications finalized

## 🟡 PHASE 1: Identity & Gateway (IN PROGRESS)
### Task 1.1: Local Persistence (User Identity)
- [ ] **Architect:** Define Room Entity for User Profile including the 72-hour lock timestamp.
- [ ] **Coder:** Implement Room database setup and LocalUserRepository.
- [ ] **Reviewer:** Verify data models match the "Trust Identity" requirements in `SCREENS.md`.

### Task 1.2: Tactical Onboarding & Identity Lock
- [ ] **Architect:** Plan the conditional Start Destination (Onboarding vs. Map).
- [ ] **Coder:** Build the "Joining the Mesh" screen and the Profile screen with the 72-hour countdown timer.
- [ ] **Reviewer:** Audit UI for "Rugged/Utility" aesthetic and check "Anonymous Mode" logic.

## 🟡 PHASE 2: Spatial Awareness (Mock Data)
### Task 2.1: MapLibre Integration
- [ ] **Architect:** Design the MapView container and a mock Tile Provider (using Lebanon region).
- [ ] **Coder:** Integrate MapLibre SDK and implement the Top Pill (Lat/Long display).
- [ ] **Reviewer:** Ensure map components are optimized for offline-default rendering.

### Task 2.2: The Event Ledger (Mocked)
- [ ] **Architect:** Define the `Event` Room Entity (Immutable) and a `MockDataGenerator`.
- [ ] **Coder:** Implement the Event Log Screen and the Map Event Pins using static mock events.
- [ ] **Reviewer:** Confirm the "Timeline Rail" in the Log screen matches the UX spec.

### Task 2.3: Multi-Step Reporting Wizard
- [ ] **Architect:** Design the state machine for the 3-step reporting flow.
- [ ] **Coder:** Build the Reporting Bottom Sheet. Note: Broadcast action should only update local DB for now.
- [ ] **Reviewer:** Test "Two-Tap" efficiency for reporting under stress.

## ⚪ PHASE 3: Peer Discovery UI (Radar Only)
- [ ] **Architect:** Design the Radar Canvas logic using RSSI distance math.
- [ ] **Coder:** Implement the Visual Radar and Peer List with placeholder "Nearby Peers."

## 🔴 PHASE 4: Networking & Real-Time Sync (FUTURE)
- [ ] Implement Google Nearby Connections wrapper.
- [ ] Event-Driven "Ping" logic (Wake-up signal on broadcast).
- [ ] Set Union Algorithm for DB delta exchange.
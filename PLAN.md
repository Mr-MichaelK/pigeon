# PLAN: Task 3.1 - Mesh Radar & Peer Discovery UI

## 1. Domain & State Management
* **State Logic:** Define an Enum `MeshPowerState` { OFF, PASSIVE, ACTIVE }.
* **Peer Model:** Create `Peer.kt` data class.
    * Fields: `deviceId`, `callsign`, `connectionType` (BLE/Wi-Fi), `rssi` (Signal Strength), `syncProgress` (0-100), `lastSeen` (Long).
* **ViewModel:** `RadarViewModel.kt` to expose a `StateFlow<List<Peer>>` and handle the transition logic between power states.

## 2. UI Layer (Compose)
* **3-Way Toggle:** A segmented control at the top to switch between OFF, PASSIVE, and ACTIVE modes.
* **Radar Canvas:** A custom `Canvas` component.
    * Use `drawCircle` for the radar rings.
    * Map `Peer` RSSI values to distance from the center.
    * Render peers as small icons; ensure they are **Read-Only** (no click listeners).
* **Peer List:** A `LazyColumn` below the radar.
    * **Live Peers:** Show callsign, signal meter, and a linear progress bar if a sync is active.
    * **History:** A grayed-out section for peers synced in the last 24h.

## 3. Visual & Technical Constraints
* **Aesthetic:** Background #F8F7F6, Primary Gold #DF9C20 for active states.
* **Battery Efficiency:** The Radar Canvas must use `remember` for drawing paths to avoid unnecessary recompositions during animation.
* **Sync Pulse:** The "Mesh Pulse" indicator should pulse green when the device is in its 1-minute Active Window.
### Part 1: The Situational Awareness Map

#### **1. Main Map Interface**

* **Engine:** MapLibre Native SDK (Offline-Default).
* **Coordinate Display:** A persistent top-anchored pill-shaped overlay showing current `Latitude` and `Longitude` in decimal format (e.g., `33.8938° N, 35.5018° E`).
* **Connectivity Bar:** A header status indicator showing:
* **Mesh Status:** Active (Scanning/Broadcasting) vs. Passive (Listening).
* **Sync Metadata:** "Synced X minutes ago" based on the last Room DB transaction.


* **Map Behavior:**
* Must use a local `.mbtiles` or `PMTiles` source.
* **Agent Task:** Implement a download manager/initializer that fetches the **Lebanon Region** tiles during initial setup (when internet is available) and stores them for zero-connectivity use.
* **Event Markers:** Custom SVG pins based on `eventType`. Tapping a pin opens the **Event Detail Sheet**.



#### **2. Event Detail Sheet**

* **UI Pattern:** Persistent Bottom Sheet.
* **Functionality:** Displays the Event Title, detailed Description, coordinates, and "Update History" (a timeline of status changes).
* **Action:** Includes a "Resolve Incident" button which, when tapped, creates a new "Resolution" event to be propagated through the mesh.

#### **3. Multi-Step Reporting Wizard**

* **Trigger:** The "Report" button (Bottom Action Bar).
* **Step 1: Type Selection:** A clean 4-grid selector (Fire, Medical, Infrastructure, Resource).
* *Note:* "Resource" replaces "Need Help" to announce supplies like food, water, or extinguishers.


* **Step 2: Details & Context:** A simplified form to add:
* A short Title/Label.
* A brief text Description (Stress-optimized UI).
* **TTL Selector:** Default expiry (e.g., 6h, 12h, 24h).


* **Step 3: Confirmation:** One-tap "Confirm & Broadcast" that auto-grabs current GPS and timestamps the event.
* **"Other" Branch:** If "Other" is selected in Step 1, provide a free-text field for `eventType`.

---

That simplifies the logic significantly. By removing "Urgent" and "Critical" tags, we lean fully into the **Information-Oriented** philosophy where every piece of data is treated with equal transport priority, leaving the "importance" to the user's manual filtering.

Regarding the `creatorDeviceId`, while it’s a hash, it's useful for "following" reports from a specific known entity (like a local medic or scout).

---

### Part 2: The Event History Log (Refined)

#### **1. Centralized Event Ledger**

* **Design Pattern:** A vertical list utilizing a **Timeline Rail** to show the flow of time.
* **Filter Chips:**
* **All Logs:** The default view showing every valid event in the Room DB.
* **Unresolved:** Filters for events where the `isResolved` flag is false.
* **Resolved:** Filters for events marked as resolved (useful for post-disaster audit).
* **Nearby:** A spatial filter that only shows events within a specific radius (e.g., 5km) of the current GPS coordinate.



#### **2. Search & Discovery**

* **Multi-Criteria Search:** The search bar must support the following queries:
* **Text Search:** Match against `title` or `payload` (description) strings.
* **Type Search:** Filter by `eventType` Enum (e.g., "Fire", "Water").
* **Device Search:** Match by `creatorDeviceId`.
* **Date Range:** A "From - To" date picker to view events from specific time windows.



#### **3. Conflict-Free Logic (Architect Note)**

* **No Conflicts:** Do not implement "Merge Conflict" UI. The Set Union algorithm ensures that the Log is simply the union of all known valid events.
* **Sync Status:** Replace "Force Sync" with a **Mesh Pulse** indicator. It pulses green when the device enters its **1-minute Active Window** to exchange deltas.

---

To provide your agents with a complete technical "vision," I have synthesized your latest instructions and screenshots into the final section of the `ai/SCREENS.md` file.

This document uses precise Android development terminology (RADAR view drawing, State management, RSSI indicators) so the **Architect** and **Coder** agents know exactly how to implement the logic.

---

### Part 3: Peer Discovery & Connectivity

#### **1. Mesh Radar & State Control**

* **Power State Toggle:** A prominent 3-way segmented control at the top:
* **OFF:** Disables all Nearby Connections radios (Scanning & Advertising).
* **PASSIVE:** BLE Advertising/Listening only (Synchronizes via small BYTES payloads).
* **ACTIVE:** Full P2P Cluster mode (Wi-Fi Direct enabled for high-speed delta transfers).


* **Visual Radar:** * A custom-drawn circular canvas centered on the user.
* **Logic:** Dynamically renders icons for discovered peers. Distance from the center is determined by the **RSSI (Signal Strength)**.
* **Interaction:** Tapping a peer icon on the radar triggers a manual **Ping** (Handshake) to initiate an opportunistic sync.



#### **2. Peer List Management**

* **Active Peers (Live):** * A list of devices currently within radio range.
* **Metadata:** Display peer `Callsign`, connection type (BLE vs. Wi-Fi), and a real-time signal strength meter.
* **Sync Progress:** If a Delta transfer is occurring, show a linear progress bar (e.g., "Syncing Manifest.db... 64%").


* **Sync History (Last 24h):** * A grayed-out "History" section showing peers that are no longer in range but with whom a successful Set Union sync was completed recently.

---

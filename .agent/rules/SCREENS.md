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

### Part 3: Peer Discovery & Connectivity

#### **1. Mesh Radar & State Control**

* **Power State Toggle:** A prominent 3-way segmented control at the top:
* **OFF:** Disables all Nearby Connections radios (Scanning & Advertising).
* **PASSIVE:** BLE Advertising/Listening only (Synchronizes via small BYTES payloads).
* **ACTIVE:** Full P2P Cluster mode (Wi-Fi Direct enabled for high-speed delta transfers).


* **Visual Radar:** * A custom-drawn circular canvas centered on the user.
* **Logic:** Peer discovery remains constant, but the "Ping" (transition to Active) is now a background broadcast triggered automatically whenever the user submits a report via the **Reporting Wizard**.
* **Interaction:** The radar is **Read-Only**. Tapping icons is disabled.



#### **2. Peer List Management**

* **Active Peers (Live):** * A list of devices currently within radio range.
* **Metadata:** Display peer `Callsign`, connection type (BLE vs. Wi-Fi), and a real-time signal strength meter.
* **Sync Progress:** If a Delta transfer is occurring, show a linear progress bar (e.g., "Syncing Manifest.db... 64%").


* **Sync History (Last 24h):** * A grayed-out "History" section showing peers that are no longer in range but with whom a successful Set Union sync was completed recently.

---

### Part 4: Profile Configuration & Trust Identity

#### **1. Identity Profile Header**

* **User Avatar:** A large, circular placeholder for the user's local avatar.
* **Verified Status:** A "Verified Node" badge (green checkmark) that appears if the device has participated in a threshold number of successful syncs.
* **Identity Summary:** Displays the current `Display Name` and `Role` prominently.

#### **2. The 72-Hour Identity Lock**

* **Logic:** * To maintain network trust and prevent malicious actors from spoofing multiple identities, any change to the Profile data triggers a **72-hour lockout**.
* The UI must display a `PROFILE LOCK` information box explaining this constraint.


* **The Countdown Timer:** * When a user attempts to edit, a "Confirmation Modal" appears (as seen in Screenshot 6).
* It features a live countdown timer (e.g., `71:59:58`) and a warning that this identity will be "locked" across the emergency mesh.
* **Action:** "Save & Lock Identity" button.



#### **3. Form Fields**

* **Display Name:** A text input for the user's public-facing handle (e.g., "Alpha-One").
* **Operational Role:** A dropdown/spinner to select their primary function (e.g., Lead Medic, Scout, Logistics, Civilian).
* **Device Node Name:** A technical read-only or semi-editable field (e.g., `NODE-ALPHA-X`) that maps to the `creatorDeviceId`.

---

### Part 5: "Joining the Mesh" (Onboarding)

#### **1. Identity & Node Setup (The "Digital Signature")**

* **Logic:** This is a **one-time** screen displayed only if no local profile exists in the Room DB.
* **Fields:**
* **Display Name / Callsign:** Text field for public identification.
* **Device Nickname:** Used for BLE/Wi-Fi Direct discovery (e.g., "Pigeon-Node-01").
* **Anonymous Mode Toggle:** Instantly clears the name fields and sets the identity to `Anonymous Civilian`.


* **Privacy Disclaimer:** A high-visibility "Rugged-style" warning box: *"Your role and callsign will be permanently attached to every event you broadcast. This cannot be undone once synced with the mesh."*

#### **2. Tactical Role Selection**

* **Layout:** A grid of high-contrast "Role Cards."
* **Roles & Visual Tokens:**
* **Civilian:** Slate Grey / Default Icon.
* **Medic/Doctor:** Soft Red / Cross Icon.
* **Fire/Rescue:** Orange / Flame Icon.
* **Utility/Tech:** Blue / Wrench Icon.


* **Smart Defaults:** If no selection is made within the "Speed to Map" threshold, default to **Civilian**.

#### **3. The "Finish" Action**

* **Button Label:** `JOIN THE MESH`
* **Feedback:** Upon clicking, the app should show a brief "Generating Node Keys..." animation to emphasize the security/local-first nature of the identity.
* **Offline Indicator:** A footer note: *"No Internet Required. Local Storage Only."*

---

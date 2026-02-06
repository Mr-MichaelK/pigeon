# Project Pigeon: Technical Architecture

## 1. Core Philosophy: Information-Oriented vs. Message-Oriented
Pigeon is built on the **Epidemic Broadcast Model**. [cite_start]Unlike chat apps that require a direct path to a recipient, Pigeon treats data like a "virus" that propagates through the mesh via human movement (Store-and-Forward). 
* **The Goal:** Achieve "Shared Truth" (Eventual Consistency) across all nodes.
* [cite_start]**The Ubiquity Test:** Hardware-agnostic; must work on any Android device (API 24+) using standard radios.

## [cite_start]2. The Layered System Model 
* **Sync/Exchange Layer:** Google Nearby Connections API using `P2P_CLUSTER` topology.
* **Data Layer (The Ledger):** An immutable Room (SQLite) database. 
    * Uses a **Set Union Algorithm** to merge "deltas" (missing Event IDs) during peer encounters.
    * Prevents duplicates via unique Event Hashes.
* **Control Layer (The Ping System):** Manages transitions between Passive (BLE) and Active (Wi-Fi Direct) states.

## 3. Power Management: The 3-State Logic
[cite_start]To solve the "Battery-Utility Paradox," devices toggle between:
1. **OFF:** Radios disabled.
2. **PASSIVE:** BLE Advertising/Listening (Low Power). Small `BYTES` payloads only.
3. **ACTIVE:** Wi-Fi Direct Discovery/Transfer (High Power). Used for large `FILE` delta transfers.

## [cite_start]4. The "Ping" Synchronization Flow 
1. **Discovery:** An "Active" device pings a "Passive" device via BLE.
2. **Wake-up:** The Passive device wakes into the "Active" state for exactly **1 minute**.
3. **Handshake:** Devices exchange a list of missing Event IDs (Deltas).
4. **Transfer:** Data is exchanged. If the delta > 32KB, it upgrades to a Wi-Fi Direct `FILE` payload to "Race to Sleep."
5. **Sleep:** Devices return to Passive/Off to conserve energy.

## 5. Data Integrity & Lifecycle
* **Immutability:** Once an event is created, its core data (ID, Type, Lat/Long) is final. Updates are appended as new linked events.
* [cite_start]**Purge Controller:** A background `WorkManager` purges events once their user-defined **TTL (Time-to-Live)** expires to prevent local storage bloat.

## [cite_start]6. Technical Stack Summary 
* **Language:** Kotlin 2.0+ (Native Android).
* **Architecture:** MVVM + Clean Architecture.
* **Mapping:** MapLibre Native SDK (Offline MVT/Vector Tiles).
* **Serialization:** Protocol Buffers (Protobuf) for compact binary transfer.
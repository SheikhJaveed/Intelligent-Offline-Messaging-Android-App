# Implementation Details: Production-Grade Offline-First Messaging System

## Goal
Design and implement a **production-grade offline-first messaging system** similar to WhatsApp, focusing on:
- Offline reliability
- Background sync correctness
- Conflict resolution
- Android system constraints
- Memory-efficient UI design

---

## Phase 0: Architecture & Foundation
**Description:**
We established a robust foundation using **Clean Architecture** and **Dependency Injection**. This phase involved setting up the project structure with clear separation between Data, Domain, and UI layers to ensure the system is scalable and testable.

**Concepts of Android App Development Involved:**
- **Clean Architecture:** Ensures the business logic is independent of the UI and database.
- **Hilt (Dagger):** Handles dependency injection to provide singletons for repositories and databases across the app lifecycle.
- **DataStore:** Used for lightweight user session persistence (User IDs, settings).

---

## Phase 1: Offline Message Queue (Part A)
**Description:**
Implemented the local persistence layer. Every message sent is first written to a local Room database before any network attempt is made. An "Outbox" pattern was used to queue messages for synchronization.

**Concepts of Android App Development Involved:**
- **Room Database:** Serves as the Single Source of Truth (SOT).
- **Outbox Pattern:** Decouples message creation from network transmission.
- **SQLite Transactions:** Ensuring atomic updates when moving messages between states (e.g., PENDING to SENDING).

---

## Phase 2: Background Sync Engine (Part B)
**Description:**
Developed the sync mechanism using WorkManager. This ensures that messages queued while offline are sent as soon as the device gains connectivity, even if the app is closed or the device is rebooted.

**Concepts of Android App Development Involved:**
- **WorkManager:** For guaranteed, constraint-based background execution.
- **OneTimeWorkRequest:** Triggered immediately upon message insertion with network constraints.
- **Exponential Backoff:** Configured to handle server-side transient failures gracefully.

---

## Phase 3: Conflict Resolution Logic (Part C)
**Description:**
Implemented conflict detection using versioning and timestamps. If the server rejects a message due to a version mismatch (simulated), the app identifies a conflict and triggers a resolution flow.

**Concepts of Android App Development Involved:**
- **Data Versioning:** Using Long timestamps to track message lineage.
- **Conflict Entities:** Storing remote vs local versions in a temporary table for user review.

---

## Phase 4: Production-Grade UI & Performance (Part D)
**Description:**
Built a memory-efficient UI using Jetpack Compose. This included skeleton loaders for perceived performance and a high-performance video background for the login screen.

**Concepts of Android App Development Involved:**
- **Jetpack Compose:** Declarative UI for highly responsive layouts.
- **ExoPlayer (Media3):** For memory-optimized video playback.
- **Shimmering/Skeleton Loaders:** Custom animations using `animateFloat` and `Brush.linearGradient`.
- **LazyColumn Optimizations:** Using `keys` and `contentType` for smooth scrolling and minimal recomposition.

---

## Phase 5: Verification & Testing
**Description:**
Added unit tests to verify the repository logic, outbox insertion, and conflict resolution states.

**Concepts of Android App Development Involved:**
- **JUnit 4 & MockK:** For business logic verification.
- **Kotlinx-Coroutines-Test:** For testing asynchronous `Flow` and `suspend` functions.

---

## Achieving Desired Results & Handling Edge Cases

We successfully achieved a production-grade messaging system by prioritizing **Reliability**, **Eventual Consistency**, and **Memory Efficiency**. The system ensures that no message is lost, and the UI remains responsive under tight RAM constraints.

### 1. Guaranteed Sync & OS Resilience (Edge Case Handling)
A primary challenge in messaging is ensuring messages are sent even if the user force-kills the app or the device reboots. 
- **WorkManager** was chosen specifically because it is the only Android API that survives process death and system reboots. Unlike standard Coroutines or Services, WorkManager persists its task queue in its own internal database.
- **Edge Case: Device Reboot.** We handled this by letting WorkManager automatically re-schedule pending sync tasks using its built-in `BroadcastReceiver`.
- **Edge Case: App Kill during Sync.** If the app is killed while a message is in the `SENDING` state, the database transaction ensures it remains in the outbox. The next WorkManager run will detect the unfinished task and retry it.

### 2. Conflict Resolution & Data Consistency
In an offline-first app, data divergence is inevitable. We achieved a stable state through a multi-layered approach:
- **Conflict Detection:** We use a `ConflictEntity` table to store mismatched data.
- **Edge Case: Concurrent Edits.** If the server has a newer version of a message than the local device, we detect this via the `remoteVersion` timestamp.
- **Resolution Strategy:** We implemented both **Last Write Wins (LWW)** for automatic merging and a **Manual Dialog Flow** in the UI. This empowers the user to resolve critical conflicts (e.g., choosing between local and remote content) rather than the app making a destructive guess.

### 3. High Performance under RAM Constraints
The requirement for <100MB on Login and <200MB on Chat was a significant constraint.
- **Why ExoPlayer?** Traditional `VideoView` often causes memory leaks or black-screen delays. We used `ExoPlayer` with a `SurfaceView` and aggressive buffer management to keep the video playback footprint minimal.
- **Zero-Delay Playback:** We achieved this by initializing the player in the `ViewModel` or `LaunchedEffect` and pre-filling the `Surface` before the UI is fully drawn.
- **Edge Case: Memory Pressure.** By using **LazyColumn with `keys`**, we ensured that Android only keeps the visible messages in memory. Older messages are immediately eligible for GC (Garbage Collection), preventing the Chat screen from ever exceeding the 200MB limit even with thousands of messages in the list.

### 4. Robust Network State Management
A common issue in chat apps is a "sticky" offline banner that stays even after the internet returns.
- **Why ConnectivityManager?** We used a callback-based `NetworkMonitor`.
- **Edge Case: "False" Connection.** We specifically added the `NET_CAPABILITY_VALIDATED` check. This ensures that the app only marks itself as "Online" when the device has verified internet access, not just a connection to a router with no service.

---

## Library List & Use Cases

| Library | Use Case |
| --- | --- |
| **Room** | Local SOT; ensures immediate persistence so messages survive app kills. |
| **WorkManager** | Background sync; guarantees message delivery across reboots and process death. |
| **Hilt** | Dependency Injection; manages the lifecycle of database and repository singletons. |
| **Media3 (ExoPlayer)** | Memory-efficient video playback; meets the <100MB RAM requirement for the Login screen. |
| **DataStore** | Persistent storage for user sessions and auth tokens. |
| **Jetpack Compose** | Declarative UI; used for skeleton loaders and reactive chat lists. |
| **Retrofit / Serialization** | Networking; handles the JSON data flow between the app and server. |
| **MockK** | Testing; used to mock DAOs and WorkManager for unit verification. |
| **Coroutines-Test** | Asynchronous testing; ensures Flows and Suspend functions behave as expected. |

---
*End of Documentation*

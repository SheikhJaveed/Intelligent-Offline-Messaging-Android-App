# Intelligent Messaging App (Offline-First)

A production-grade, offline-first messaging application built with modern Android development practices. This app ensures message reliability, robust background synchronization, and conflict resolution while maintaining a highly optimized UI within strict memory constraints.

## Key Features

- **Offline-First Architecture**: Messages are persisted immediately to a local Single Source of Truth (Room DB) before network transmission.
- **Reliable Background Sync**: Leveraging WorkManager to ensure messages are delivered even if the app is force-killed or the device reboots.
- **Conflict Resolution**: Advanced handling of data divergence using versioning and manual user resolution flows.
- **Optimized Video Login**: Immediate, zero-delay background video playback on the login screen with `< 100MB` RAM footprint.
- **Premium UX**: Includes shimmering skeleton loaders, real-time connectivity banners, and per-message delivery status (⏳, ✓, ✓✓).
- **Memory Efficient**: Chat screen optimized for `< 200MB` RAM usage using `LazyColumn` key/type optimizations.

---

## Architecture & Design

The project follows **Clean Architecture** principles combined with the **Repository Pattern** and **MVVM**.

### High-Level Design (HLD)
The HLD illustrates the global data flow between the UI, the Local Outbox, and the Sync Engine.

![High-Level Design](docs/Messaging-android-app-HLD.png)

### Low-Level Design (LLD)
The LLD focuses on the specific implementation of the Sync Worker and the Conflict Resolution logic.

![Low-Level Design](docs/Messaging-android-app-LLD.png)

---

## Tech Stack

- **UI**: Jetpack Compose (Modern Declarative UI)
- **Language**: Kotlin
- **Dependency Injection**: Hilt (Dagger)
- **Database**: Room (Offline Persistence)
- **Background Work**: WorkManager (Reliable Sync)
- **Media**: ExoPlayer/Media3 (Efficient Video Playback)
- **Networking**: Retrofit & Kotlinx Serialization
- **Architecture**: MVVM + Clean Architecture
- **Testing**: MockK + JUnit 4 + Coroutines-Test

---

## Testing

The app includes unit tests for core business logic, including message sending and conflict resolution.

To run the tests and see detailed results in your terminal:
```bash
./gradlew :app:testDebugUnitTest --rerun-tasks
```

The terminal will output pass/fail status for individual test cases like:
- `sendMessage should insert message and outbox entry PASSED`
- `resolveConflict with useLocal true should retry message PASSED`

---

## Performance Metrics

| Screen | Target RAM Usage | Achievement |
| --- | --- | --- |
| **Login Screen** | ≤ 100 MB | Optimized via Media3 SurfaceView |
| **Chat Screen** | ≤ 200 MB | Optimized via LazyColumn reuse |

---

## Project Structure

```text
app/src/main/java/com/example/intelligent_messaging_app/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── ConflictDao.kt
│   │   │   ├── ConversationDao.kt
│   │   │   ├── MessageDao.kt
│   │   │   └── OutboxDao.kt
│   │   ├── entity/
│   │   │   ├── ConflictEntity.kt
│   │   │   ├── ConversationEntity.kt
│   │   │   ├── MessageEntity.kt
│   │   │   ├── OutboxEntity.kt
│   │   │   └── SyncStateEntity.kt
│   │   ├── ChatDatabase.kt
│   │   └── Converters.kt
│   └── repository/
│       ├── MessageRepositoryImpl.kt
│       └── UserPreferencesRepository.kt
├── di/
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
├── domain/
│   └── model/
│       └── MessageStatus.kt
├── sync/
│   └── SyncWorker.kt
├── ui/
│   ├── chat/
│   │   ├── ChatScreen.kt
│   │   └── ChatViewModel.kt
│   └── login/
│       ├── LoginScreen.kt
│       └── LoginViewModel.kt
├── util/
│   └── NetworkMonitor.kt
├── MainActivity.kt
└── MessagingApp.kt
```

### File Descriptions

- **`ConflictDao.kt`**: Data access for message conflict resolution.
- **`ConversationDao.kt`**: Data access for chat conversation metadata.
- **`MessageDao.kt`**: Data access for local message persistence.
- **`OutboxDao.kt`**: Data access for the outgoing message queue.
- **`ConflictEntity.kt`**: Room entity representing a message conflict.
- **`ConversationEntity.kt`**: Room entity for conversation details.
- **`MessageEntity.kt`**: Room entity for chat messages.
- **`OutboxEntity.kt`**: Room entity for messages waiting to sync.
- **`SyncStateEntity.kt`**: Room entity for tracking global sync status.
- **`ChatDatabase.kt`**: Main Room database configuration.
- **`Converters.kt`**: Room type converters for non-primitive types.
- **`MessageRepositoryImpl.kt`**: Repository implementation orchestrating data between local and remote.
- **`UserPreferencesRepository.kt`**: Manages user session and preferences using DataStore.
- **`DatabaseModule.kt`**: Hilt module for providing database-related dependencies.
- **`RepositoryModule.kt`**: Hilt module for providing repository implementations.
- **`MessageStatus.kt`**: Domain enum for the lifecycle of a message.
- **`SyncWorker.kt`**: WorkManager implementation for background message synchronization.
- **`ChatScreen.kt`**: Jetpack Compose UI for the chat interface.
- **`ChatViewModel.kt`**: ViewModel handling chat screen state and actions.
- **`LoginScreen.kt`**: Jetpack Compose UI for the login screen with video background.
- **`LoginViewModel.kt`**: ViewModel for handling user login and session.
- **`NetworkMonitor.kt`**: Utility to monitor and report network connectivity status.
- **`MainActivity.kt`**: Main activity hosting the application's navigation.
- **`MessagingApp.kt`**: Application class for Hilt and WorkManager initialization.

---

## License

This project is developed for demonstration of production-grade Android engineering principles.

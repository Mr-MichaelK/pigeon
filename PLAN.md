# Architect Plan: Event Title Data Loss Fix

## Problem
Event titles (names) are missing or blank when synced between devices via the mesh network.
The `PigeonEvent` protobuf message lacks a `title` field. The current implementation fallback to using a truncated `description` as the title, which fails when the description is empty.

## Proposed Changes

### 1. Protobuf Update
- **File**: `app/src/main/proto/pigeon_models.proto`
- **Action**: Add `string title = 10;` to the `PigeonEvent` message.

### 2. Network Mapping Update (NearbySyncManagerImpl)
- **File**: `app/src/main/java/com/example/pigeon/data/network/NearbySyncManagerImpl.kt`
- **Actions**:
    - Update `domainToProto`: Add `.setTitle(event.title)`.
    - Update `protoToDomain`: Change `title = proto.description.take(50)` to `title = proto.title`.

### 3. ViewModel Mapping Update (ReportViewModel)
- **File**: `app/src/main/java/com/example/pigeon/ui/screens/map/ReportViewModel.kt`
- **Action**: Update `domainToProto`: Add `.setTitle(event.title)`.

## Next Step
Wait for user to state "Plan approved" or "Proceed".
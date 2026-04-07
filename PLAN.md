# Architect Plan: EventDetailSheet Click Bug Fix

## Description of the Bug
When clicking on a tactical map symbol, the `EventDetailSheet` does not appear. 
In `MapScreen.kt`, the `addClickListener` attempts to locate the clicked event by comparing exact floating-point values of latitude and longitude:
```kotlin
val event = events.find { 
    it.latitude == symbol.latLng.latitude && 
    it.longitude == symbol.latLng.longitude 
}
```
Due to precision loss when MapLibre's native C++ engine processes and returns the coordinates, this equality check almost always fails resulting in `event == null`.

## Architectural Solution (Role: Coder Action Plan)

To fix this reliably and robustly, we will utilize MapLibre's `data` property on `SymbolOptions` and `Symbol` to carry the unique `eventId` instead of relying on coordinate matching.

### File Modifications
**1. `app/src/main/java/com/example/pigeon/ui/screens/map/MapScreen.kt`**

- **During Symbol Creation (`setupMapStyle` ~line 711 & ~730):**
  - Import `com.google.gson.JsonPrimitive`.
  - Add `.withData(JsonPrimitive(event.eventId))` when creating `SymbolOptions` for both the "combined view" (labels) and the "icon only view".

- **During Symbol Click Handling (`addClickListener` ~line 616):**
  - Read the `eventId` from the clicked symbol's data: `val eventId = symbol.data?.asString`.
  - Find the corresponding event in the `events` list: `val event = events.find { it.eventId == eventId }`.
  - Call `onEventClick(event)` if the event is found.

## Next Step (Reviewer)
Wait for user to state "Plan approved" or "Proceed".
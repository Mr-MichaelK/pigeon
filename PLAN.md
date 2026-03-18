# PLAN: Map Crosshair Overlay (Task 2.4)

Add a tactical crosshair to the center of the map to improve coordinate precision for the user.

## 1. UI Implementation
- **`MapScreen.kt`**:
    - Update `MapCrosshair` to `Modifier.fillMaxSize()`.
    - Enhance `Canvas` drawing:
        - **Center Circle**: Draw a small outlined circle (e.g., 12dp diameter) at the exact center.
        - **Full-Screen Lines**: Extend the horizontal and vertical lines from the edges of the center circle (with a small gap) all the way to the screen borders (`0` to `size.width/height`).
        - **Refined Styling**: 
            - Use `MeshColor.Primary` (Operational Gold) for the central elements (circle and inner line segments).
            - Use a slightly more transparent or neutral color (e.g., `MeshColor.Border` or `MeshColor.TextSecondary`) for the screen-wide extensions to reduce visual clutter.
            - Maintain the subtle drop shadow for visibility.

## 2. Refinements
- Ensure the crosshair is placed *above* the map but *below* other UI overlays like the coordinate pill and tool stack to maintain a clean z-index hierarchy.
- Add a subtle shadow or glow to ensure visibility against diverse map backgrounds (satellite, light, dark).

## 3. Verification Plan
- **Manual Verification**:
    - Build and run the app.
    - Verify that the crosshair remains perfectly centered while panning and zooming the map.
    - Confirm that the crosshair is clearly visible but non-obstructive.
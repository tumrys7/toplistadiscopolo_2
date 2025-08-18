# Spotify Bottom Sheet Swipe Behavior Fix

## Date: January 2025

## Issue
The Spotify bottom sheet was hiding completely when users swiped down from the expanded state, which could disconnect the Spotify connection and interrupt playback.

## Solution
Modified the bottom sheet behavior to prevent complete hiding on swipe, ensuring it stays at the collapsed state (mini player visible) as the minimum.

## Changes Made

### 1. Disabled Swipe-to-Hide by Default
**File**: `SpotifyBottomSheetController.java`
**Line**: 120

```java
// Before:
bottomSheetBehavior.setHideable(true);

// After:
bottomSheetBehavior.setHideable(false); // Prevent hiding on swipe
```

This prevents users from swiping the bottom sheet to the hidden state while still allowing programmatic hiding.

### 2. Smart State Management for Showing
**File**: `SpotifyBottomSheetController.java`
**Lines**: 493-504

When showing the bottom sheet from a hidden state:
- Temporarily enables `setHideable(true)` to allow the transition
- Transitions to collapsed state
- Disables `setHideable(false)` after the sheet is visible
- Then expands if requested

```java
if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
    bottomSheetBehavior.setHideable(true);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    // Disable hideable after transitioning from hidden
    bottomSheet.postDelayed(() -> {
        bottomSheetBehavior.setHideable(false);
        if (expanded) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }, 100);
}
```

### 3. Programmatic Hiding Still Works
**File**: `SpotifyBottomSheetController.java`
**Lines**: 527-529

The `hideBottomSheet()` method (called by the close button) temporarily enables hiding:

```java
public void hideBottomSheet() {
    // Temporarily enable hideable to allow programmatic hiding
    bottomSheetBehavior.setHideable(true);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    // ... rest of cleanup code
}
```

### 4. Simplified State Callback
**File**: `SpotifyBottomSheetController.java`
**Lines**: 265-292

Removed complex logic that was trying to prevent hiding after it occurred, since we now prevent it at the behavior level.

## Behavior After Fix

### User Interactions:
- **Swipe down from EXPANDED**: Goes to COLLAPSED state (mini player visible)
- **Swipe down from COLLAPSED**: Stays at COLLAPSED state (cannot hide)
- **Click close button**: Completely hides the bottom sheet
- **Click mini player**: Expands to full player

### Benefits:
1. **Maintains Spotify connection**: Prevents accidental disconnection
2. **Better UX**: Mini player always accessible during playback
3. **Intentional hiding only**: Users must explicitly tap close button to hide
4. **Smooth transitions**: All state changes work seamlessly

## Technical Details

The fix leverages Material Design's `BottomSheetBehavior.setHideable()` property:
- When `false`: Prevents hiding through user swipes
- When `true`: Allows both swipe and programmatic hiding

By dynamically toggling this property, we achieve:
- Prevention of accidental hiding via swipe
- Preservation of programmatic control for showing/hiding
- Maintenance of all other bottom sheet functionality

## Testing Scenarios

1. ✅ Start playing a track - bottom sheet appears in collapsed state
2. ✅ Expand bottom sheet - shows full player
3. ✅ Swipe down from expanded - goes to collapsed (not hidden)
4. ✅ Try to swipe down from collapsed - stays collapsed
5. ✅ Tap close button - hides completely
6. ✅ Play another track - bottom sheet reappears

## Files Modified

- `/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/SpotifyBottomSheetController.java`

## Related Issues

This fix complements previous Spotify integration improvements:
- Connection stability fixes
- Bottom sheet initialization fixes
- Player state management improvements
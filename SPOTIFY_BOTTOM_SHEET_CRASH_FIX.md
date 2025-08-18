# Spotify Bottom Sheet Crash Fix

## Problem
The app was crashing with two errors when attempting to play a Spotify track:

1. **ClassCastException**: `android.widget.LinearLayout cannot be cast to androidx.coordinatorlayout.widget.CoordinatorLayout`
2. **NullPointerException**: Attempt to invoke method on null object reference in `setupListeners()`

## Root Cause
The `utwory_wykonawcy.xml` layout file used a `LinearLayout` as the root view, but the `SpotifyBottomSheetController` expected a `CoordinatorLayout` for the BottomSheet behavior to work properly.

## Solution Applied

### 1. Layout File Update
**File**: `/workspace/toplistadiscopolo/src/main/res/layout/utwory_wykonawcy.xml`

Changed the root view from `LinearLayout` to `CoordinatorLayout` and wrapped the existing content in a nested `LinearLayout`:

```xml
<!-- Before -->
<LinearLayout android:id="@+id/root">
    <!-- content -->
</LinearLayout>

<!-- After -->
<androidx.coordinatorlayout.widget.CoordinatorLayout android:id="@+id/root">
    <LinearLayout>
        <!-- content -->
    </LinearLayout>
</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 2. SpotifyBottomSheetController Improvements
**File**: `/workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/SpotifyBottomSheetController.java`

#### a. Better Error Handling in Constructor
- Changed `initializeBottomSheet()` to return a boolean indicating success
- Only call `setupListeners()` if initialization succeeds

#### b. Type Checking for CoordinatorLayout
- Added explicit check to verify rootView is a CoordinatorLayout
- Throw informative exception if wrong type is provided

#### c. Null Safety in setupListeners()
- Added null checks before setting click listeners
- Early return if required views are not initialized

## Benefits
1. **Prevents ClassCastException**: The layout now provides the correct view type
2. **Prevents NullPointerException**: Listeners are only set up when views are properly initialized
3. **Better Error Messages**: Clear logging helps diagnose issues if they occur
4. **More Robust Code**: Graceful handling of initialization failures

## Testing Recommendation
Test the Spotify playback functionality in the UtworyWykonawcy activity to ensure:
1. The bottom sheet appears correctly when playing a track
2. No crashes occur during initialization
3. All controls (play/pause, close) work as expected
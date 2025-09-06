# Spotify Authorization Error Fix - Comprehensive Solution

## Problem Analysis

The error logs showed two main issues:

1. **UserNotAuthorizedException**: The user hasn't authorized the app to control Spotify
2. **Background Activity Launch Block**: Android is preventing the app from launching Spotify when it's in the background

```
2025-09-06 09:39:58.951  2169-4570  ActivityTaskManager     system_server                        E  Background activity launch blocked! [callingPackage: com.grandline.toplistadiscopolo; callingPackageTargetSdk: 36; callingUid: 11005; callingPid: 26219; appSwitchState: 2; callingUidHasVisibleActivity: false; ...]
```

## Root Cause

The app was trying to launch Spotify from the background when the user wasn't authorized, which Android blocks for security reasons. This created a cycle where:
1. User tries to play a track
2. App detects user is not authorized
3. App tries to launch Spotify (blocked by Android)
4. User sees authorization error with no way to resolve it

## Solution Implemented

### 1. Removed Background Activity Launches

**File**: `SpotifyService.java`

- Removed automatic Spotify app launching during connection attempts
- Removed background activity launches in `handleAuthorizationRequired()`
- Increased connection timeout from 10s to 30s to allow for authorization

```java
// Before: Tried to launch Spotify automatically (would be blocked)
Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.spotify.music");
context.startActivity(intent); // This was blocked by Android

// After: Don't try to launch from background
// Don't try to launch Spotify from background - Android will block this
// Just proceed with the connection attempt directly
```

### 2. Added Proper Authorization Flow

**File**: `SpotifyService.java`

Added a new method that can only be called from an active Activity:

```java
public boolean launchSpotifyForAuthorization(Context activityContext) {
    // Only launches if called from active Activity context
    // Avoids background activity launch restrictions
}
```

### 3. Enhanced Main Activity Integration

**File**: `ListaPrzebojowDiscoPolo.java`

Added method to handle Spotify authorization with proper user feedback:

```java
public void launchSpotifyForAuthorization() {
    // Launches Spotify from active Activity context
    // Shows appropriate Toast messages
    // Fallback to Play Store if Spotify not installed
}
```

### 4. Improved Retry Button Functionality

**File**: `SpotifyBottomSheetController.java`

Updated the retry button to:
- Launch Spotify for authorization when clicked
- Wait 3 seconds for user to authorize
- Then retry the connection

```java
retryButton.setOnClickListener(v -> {
    // Launch Spotify for authorization
    activity.launchSpotifyForAuthorization();
    
    // Delay retry to give user time to authorize
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
        playTrack(currentTrackId, currentTrackTitle, currentTrackArtist);
    }, 3000);
});
```

### 5. Automatic Reconnection on App Resume

**File**: `ListaPrzebojowDiscoPolo.java`

Enhanced `onResume()` to detect when user returns from Spotify:

```java
@Override
public void onResume() {
    // Check if user returned from Spotify and try to reconnect
    if (spotifyBottomSheetController.isBottomSheetVisible()) {
        spotifyService.forceReconnect();
    }
}
```

### 6. Better Error Messages

**File**: `SpotifyBottomSheetController.java`

Improved error messages in Polish for better user experience:

- "Zaloguj się do Spotify" - "Login to Spotify"
- "Otwórz Spotify → Zaloguj się → Wróć tutaj → Spróbuj ponownie" - "Open Spotify → Login → Return here → Try again"

### 7. Force Reconnection Method

**File**: `SpotifyService.java`

Added method to force a clean reconnection:

```java
public void forceReconnect() {
    // Reset connection state
    // Clear existing connections
    // Attempt new connection
}
```

## How the Fix Works

### User Flow After Fix:

1. **User clicks play on a track**
2. **App shows bottom sheet with track info**
3. **If not authorized**: Shows "Zaloguj się do Spotify" with retry button
4. **User clicks retry button**:
   - App launches Spotify (from active Activity - not blocked)
   - User sees toast: "Otwórz Spotify, zaloguj się i wróć tutaj"
5. **User authorizes in Spotify and returns to app**:
   - `onResume()` detects return and calls `forceReconnect()`
   - Connection succeeds and track plays automatically

### Key Improvements:

- ✅ **No more background activity launch blocks**
- ✅ **Clear user guidance in Polish**
- ✅ **Automatic retry when user returns**
- ✅ **Proper error handling**
- ✅ **Longer timeout for authorization (30s)**
- ✅ **Fallback to Play Store if Spotify not installed**

## Technical Details

### Android Background Activity Restrictions

Android (especially API 29+) blocks background apps from launching activities to prevent abuse. The fix ensures all Spotify launches happen from the foreground Activity context.

### Connection Parameters

The connection parameters remain unchanged as they were already correct:

```java
ConnectionParams connectionParams = new ConnectionParams.Builder(Constants.SPOTIFY_CLIENT_ID)
    .setRedirectUri(Contants.SPOTIFY_REDIRECT_URI)
    .showAuthView(true)
    .build();
```

### Error Handling

The fix maintains all existing error handling while adding better user guidance for authorization issues.

## Testing

To test the fix:

1. **Ensure Spotify is not authorized** (fresh install or revoke permissions)
2. **Try to play a track** - should show authorization message
3. **Click retry button** - should launch Spotify
4. **Authorize in Spotify and return** - should automatically retry and play

## Files Modified

1. `SpotifyService.java` - Core connection logic and authorization handling
2. `SpotifyBottomSheetController.java` - UI error handling and retry button
3. `ListaPrzebojowDiscoPolo.java` - Activity integration and onResume handling

This comprehensive fix resolves the UserNotAuthorizedException and background activity launch issues while providing a smooth user experience.
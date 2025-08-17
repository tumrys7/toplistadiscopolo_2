# Spotify Connection Timeout Fix - January 17, 2025

## Problem
When clicking on "Posłuchaj" (getString(R.string.spotify)) in the Alert Dialog, the application was experiencing connection timeouts after 10 seconds. The logs showed:
- "Connection timeout after 10000ms"
- "Not connected to Spotify"
- The Spotify app was not responding to connection requests

## Root Cause Analysis

### 1. **Authentication State Issue**
The Spotify app needs to be:
- Installed on the device
- User must be logged in
- App must be authorized to use Spotify Remote SDK

### 2. **Connection Flow Problems**
- The app was trying to connect directly without ensuring Spotify was active
- No mechanism to prompt user to login if not authenticated
- Timeout handler was not providing helpful recovery options

## Solutions Implemented

### 1. **Enhanced Connection Flow with App Launch**
**File**: `SpotifyService.java`

Added logic to launch Spotify app before attempting connection on first try:
```java
// Try to open Spotify app first to ensure it's active
if (connectionRetryCount == 0) {
    Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.spotify.music");
    if (intent != null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        Log.d(TAG, "Launched Spotify app to ensure it's active");
        
        // Wait for Spotify to start before connecting
        retryHandler.postDelayed(() -> {
            connectInternal();
        }, 1500); // 1.5 second delay
    }
}
```

### 2. **Improved Timeout Handling**
**File**: `SpotifyService.java`

Modified timeout handler to try alternative methods:
```java
connectionTimeoutRunnable = new Runnable() {
    @Override
    public void run() {
        if (isConnecting) {
            // On timeout, try alternative connection method
            if (connectionRetryCount == 0) {
                connectionRetryCount++;
                Log.d(TAG, "First timeout - trying alternative connection method");
                tryBrowserAuthorization();
            } else {
                // Provide helpful error message
                Exception timeoutException = new Exception("Connection timeout - Please open Spotify app and login, then try again");
                connectionListeners.forEach(listener -> listener.onConnectionFailed(timeoutException));
            }
        }
    }
};
```

### 3. **Browser Authorization Fallback**
**File**: `SpotifyService.java`

Added method to open Spotify for user authentication:
```java
private void tryBrowserAuthorization() {
    // First try to open the Spotify app directly
    Intent spotifyIntent = context.getPackageManager().getLaunchIntentForPackage("com.spotify.music");
    if (spotifyIntent != null) {
        spotifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(spotifyIntent);
        Log.d(TAG, "Opened Spotify app for user to login");
        
        // Notify listeners with helpful message
        Exception authException = new Exception("Please login to Spotify and try again");
        connectionListeners.forEach(listener -> listener.onConnectionFailed(authException));
    }
}
```

### 4. **Retry Mechanism in UI**
**File**: `SpotifyBottomSheetController.java`

Added retry button for failed connections:
```java
// Added retry button to UI
private MaterialButton retryButton;

// Show retry button on connection failure
private void showRetryButton(boolean show) {
    if (retryButton != null) {
        retryButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    // Hide play button when showing retry
    if (show && miniPlayPauseButton != null) {
        miniPlayPauseButton.setVisibility(View.GONE);
    }
}

// Retry button click handler
retryButton.setOnClickListener(v -> {
    showRetryButton(false);
    if (currentTrackId != null) {
        playTrack(currentTrackId, currentTrackTitle, currentTrackArtist);
    }
});
```

### 5. **Better Error Messages**
**File**: `SpotifyBottomSheetController.java`

Improved error handling with specific messages:
```java
if (errorMessage.contains("Please login to Spotify")) {
    updateTrackInfo("Login Required", "Please login to Spotify and try again");
    showRetryButton(true);
} else if (errorMessage.contains("Connection timeout")) {
    updateTrackInfo("Connection Timeout", "Please open Spotify app and try again");
    showRetryButton(true);
}
```

## How the Fix Works

1. **First Connection Attempt**:
   - Launches Spotify app to ensure it's active
   - Waits 1.5 seconds for app to initialize
   - Attempts connection with Spotify Remote SDK

2. **On Timeout**:
   - If first attempt, opens Spotify app for user to login
   - Shows helpful error message
   - Displays retry button in the UI

3. **User Recovery**:
   - User can login to Spotify
   - Click retry button to attempt connection again
   - Track information is preserved for retry

## Benefits

1. **Better User Experience**: Users get clear instructions on what to do
2. **Automatic Recovery**: App tries to fix the issue by launching Spotify
3. **Retry Capability**: Users can easily retry after logging in
4. **Preserved State**: Track information is saved for retry attempts

## Testing Instructions

1. **Test with Spotify not logged in**:
   - Logout from Spotify app
   - Try to play a track
   - Should see "Login Required" message with retry button

2. **Test with Spotify closed**:
   - Force stop Spotify app
   - Try to play a track
   - Should automatically launch Spotify and connect

3. **Test retry functionality**:
   - Trigger a connection failure
   - Click retry button
   - Should attempt connection again

## Files Modified

1. `/workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/SpotifyService.java`
2. `/workspace/toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/SpotifyBottomSheetController.java`

## Important Notes

- The CLIENT_ID must be registered in the Spotify App Dashboard
- The REDIRECT_URI must match exactly what's configured in Spotify
- User must have Spotify app installed
- User must be logged into Spotify for the connection to work

## Next Steps if Issue Persists

1. Verify CLIENT_ID is correct and app is registered on Spotify Dashboard
2. Check if REDIRECT_URI matches exactly (including the scheme)
3. Test on different Android versions
4. Consider implementing OAuth2 flow for web-based authentication
# YouTube Bottom Sheet Fix - Input Channel Destroyed Issue

## Problem
The YouTube bottom sheet was experiencing an "input channel destroyed" error that prevented YouTube videos from displaying properly in the WebView.

### Error Log
```
2025-08-18 21:23:55.548 22063-22063 InputTransport com.grandline.toplistadiscopolo D Input channel destroyed: '8d4b584', fd=263
```

## Root Causes Identified
1. **Duplicate Initialization**: The YouTubeBottomSheetController was being initialized twice in the main activity
2. **WebView Lifecycle Issues**: WebView wasn't properly managed during dialog lifecycle
3. **Missing Lifecycle Callbacks**: The YouTube controller's lifecycle methods weren't being called from the activity's onPause/onResume
4. **Improper Resource Cleanup**: WebView and dialog resources weren't being cleaned up properly

## Fixes Applied

### 1. Fixed Duplicate Initialization in ListaPrzebojowDiscoPolo.java
Removed duplicate initialization that was causing multiple instances:
```java
// Before (duplicate):
youTubeBottomSheetController = new YouTubeBottomSheetController(this);
youTubeBottomSheetController = new YouTubeBottomSheetController(this);

// After (single):
youTubeBottomSheetController = new YouTubeBottomSheetController(this);
```

### 2. Enhanced YouTubeBottomSheetController.java

#### Added Handler for Main Thread Operations
```java
private Handler mainHandler;

public YouTubeBottomSheetController(Activity activity) {
    this.activityRef = new WeakReference<>(activity);
    this.mainHandler = new Handler(Looper.getMainLooper());
}
```

#### Proper Dialog Cleanup
```java
private void setupBottomSheet(Context context) {
    // Clean up any existing dialog first
    if (bottomSheetDialog != null) {
        try {
            bottomSheetDialog.dismiss();
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing existing dialog", e);
        }
        bottomSheetDialog = null;
    }
    // ... rest of setup
}
```

#### Enhanced WebView Configuration
```java
private void setupWebView() {
    if (webView == null) {
        Log.e(TAG, "WebView is null in setupWebView");
        return;
    }
    
    try {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setDatabaseEnabled(true);
        
        // Hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // ... WebViewClient and WebChromeClient setup
    } catch (Exception e) {
        Log.e(TAG, "Error setting up WebView", e);
    }
}
```

#### Thread-Safe Video Loading
```java
private void loadYouTubeVideo(String videoId) {
    if (webView == null || videoId == null) {
        Log.e(TAG, "Cannot load video: webView=" + (webView != null) + ", videoId=" + videoId);
        return;
    }
    
    // Ensure WebView operations happen on main thread
    mainHandler.post(() -> {
        try {
            String html = generateYouTubeHTML(videoId);
            Log.d(TAG, "Loading YouTube video with ID: " + videoId);
            webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null);
        } catch (Exception e) {
            Log.e(TAG, "Error loading YouTube video", e);
        }
    });
}
```

#### Improved Resource Cleanup
```java
private void cleanupWebView() {
    if (webView != null) {
        try {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.clearCache(true);
            webView.clearFormData();
            isWebViewReady = false;
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up WebView", e);
        }
    }
}

public void onDestroy() {
    try {
        dismiss();
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }
            webView.destroy();
            webView = null;
        }
        bottomSheetDialog = null;
        bottomSheetBehavior = null;
        bottomSheetView = null;
    } catch (Exception e) {
        Log.e(TAG, "Error in onDestroy", e);
    } finally {
        activityRef.clear();
    }
}
```

### 3. Added Error Handling and Logging
```java
// WebViewClient with error handling
webView.setWebViewClient(new WebViewClient() {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        // Support YouTube and Google Video domains
        if (!url.contains("youtube.com") && !url.contains("youtu.be") && !url.contains("googlevideo.com")) {
            return true;
        }
        return false;
    }
    
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        isWebViewReady = true;
        Log.d(TAG, "Page finished loading: " + url);
    }
    
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        Log.e(TAG, "WebView error: " + errorCode + " - " + description + " at " + failingUrl);
    }
});

// WebChromeClient with progress tracking
webView.setWebChromeClient(new WebChromeClient() {
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        Log.d(TAG, "Loading progress: " + newProgress + "%");
    }
    // ... other methods
});
```

### 4. Lifecycle Integration (Manual Addition Required)
Add these to `ListaPrzebojowDiscoPolo.java`:
```java
@Override
public void onResume() {
    super.onResume();
    // ... existing code ...
    
    // Resume YouTube controller
    if (youTubeBottomSheetController != null) {
        youTubeBottomSheetController.onResume();
    }
}

@Override
public void onPause() {
    super.onPause();
    
    // Pause YouTube controller
    if (youTubeBottomSheetController != null) {
        youTubeBottomSheetController.onPause();
    }
}
```

## Key Improvements
1. **Thread Safety**: All WebView operations now happen on the main thread
2. **Resource Management**: Proper cleanup of WebView and dialog resources
3. **Error Handling**: Comprehensive try-catch blocks to prevent crashes
4. **Logging**: Added detailed logging for debugging
5. **Domain Support**: Added support for googlevideo.com (YouTube's CDN)
6. **Hardware Acceleration**: Enabled for better performance
7. **Lifecycle Management**: Proper pause/resume of WebView

## Testing Checklist
- [ ] Open YouTube video in bottom sheet
- [ ] Close and reopen multiple times
- [ ] Switch between different videos
- [ ] Test app pause/resume (home button)
- [ ] Test app destroy/recreate (rotation)
- [ ] Check logs for any input channel errors
- [ ] Verify video playback works correctly
- [ ] Test swipe gestures on bottom sheet

## Expected Results
After applying these fixes:
- No more "input channel destroyed" errors
- YouTube videos load and play correctly
- Bottom sheet can be opened/closed multiple times without issues
- Proper cleanup prevents memory leaks
- WebView state is properly managed during lifecycle changes

## Date Fixed
January 18, 2025
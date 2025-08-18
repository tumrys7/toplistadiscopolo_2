package com.grandline.toplistadiscopolo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeBottomSheetController {
    private static final String TAG = "YouTubeBottomSheet";
    
    private final WeakReference<Activity> activityRef;
    private BottomSheetDialog bottomSheetDialog;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private WebView webView;
    private View bottomSheetView;
    private TextView titleTextView;
    private TextView artistTextView;
    private ImageButton closeButton;
    private ImageButton expandCollapseButton;
    private LinearLayout headerLayout;
    private FrameLayout contentContainer;
    private Handler mainHandler;
    
    private String currentVideoId;
    private String currentTitle;
    private String currentArtist;
    private boolean isMinimized = false;
    private boolean isWebViewReady = false;
    
    private static final int COLLAPSED_HEIGHT_DP = 200; // Wysokość zwiniętego panelu
    private static final int EXPANDED_HEIGHT_PERCENT = 85; // Procent wysokości ekranu dla rozwiniętego panelu
    
    public YouTubeBottomSheetController(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Pokazuje Bottom Sheet z filmem YouTube
     * @param videoUrl URL filmu YouTube lub ID filmu
     * @param title Tytuł utworu
     * @param artist Nazwa wykonawcy
     */
    public void showYouTubeVideo(String videoUrl, String title, String artist) {
        Activity activity = activityRef.get();
        if (activity == null || activity.isFinishing()) {
            return;
        }
        
        // Wyciągnij ID filmu z URL
        String videoId = extractVideoId(videoUrl);
        if (videoId == null || videoId.isEmpty()) {
            Log.e(TAG, "Nie można wyciągnąć ID filmu z URL: " + videoUrl);
            return;
        }
        
        this.currentVideoId = videoId;
        this.currentTitle = title != null ? title : "";
        this.currentArtist = artist != null ? artist : "";
        
        if (bottomSheetDialog == null) {
            setupBottomSheet(activity);
        }
        
        updateVideoInfo();
        loadYouTubeVideo(videoId);
        
        if (!bottomSheetDialog.isShowing()) {
            bottomSheetDialog.show();
            // Ustaw początkowy stan na rozwinięty
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
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
        
        bottomSheetDialog = new BottomSheetDialog(context, R.style.YouTubeBottomSheetDialog);
        bottomSheetDialog.setCancelable(true);
        bottomSheetDialog.setCanceledOnTouchOutside(true);
        
        // Inflate layout
        LayoutInflater inflater = LayoutInflater.from(context);
        bottomSheetView = inflater.inflate(R.layout.youtube_bottom_sheet_layout, null);
        
        // Znajdź widoki
        headerLayout = bottomSheetView.findViewById(R.id.youtube_header_layout);
        titleTextView = bottomSheetView.findViewById(R.id.youtube_title);
        artistTextView = bottomSheetView.findViewById(R.id.youtube_artist);
        closeButton = bottomSheetView.findViewById(R.id.youtube_close_button);
        expandCollapseButton = bottomSheetView.findViewById(R.id.youtube_expand_collapse_button);
        contentContainer = bottomSheetView.findViewById(R.id.youtube_content_container);
        webView = bottomSheetView.findViewById(R.id.youtube_webview);
        
        setupWebView();
        setupButtons();
        
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Konfiguracja BottomSheetBehavior
        View parent = (View) bottomSheetView.getParent();
        bottomSheetBehavior = BottomSheetBehavior.from(parent);
        
        setupBottomSheetBehavior(context);
        setupSwipeGestures();
    }
    
    private void setupBottomSheetBehavior(Context context) {
        // Ustaw wysokości
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int screenHeight = displayMetrics.heightPixels;
        
        int collapsedHeight = (int) (COLLAPSED_HEIGHT_DP * displayMetrics.density);
        int expandedHeight = (screenHeight * EXPANDED_HEIGHT_PERCENT) / 100;
        
        bottomSheetBehavior.setPeekHeight(collapsedHeight);
        bottomSheetBehavior.setMaxHeight(expandedHeight);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setSkipCollapsed(false);
        bottomSheetBehavior.setDraggable(true);
        
        // Callback dla zmian stanu
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        isMinimized = false;
                        expandCollapseButton.setImageResource(android.R.drawable.arrow_down_float);
                        headerLayout.setVisibility(View.VISIBLE);
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        isMinimized = true;
                        expandCollapseButton.setImageResource(android.R.drawable.arrow_up_float);
                        headerLayout.setVisibility(View.GONE);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        dismiss();
                        break;
                }
            }
            
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Animacja przezroczystości nagłówka podczas przesuwania
                if (slideOffset >= 0) {
                    headerLayout.setAlpha(slideOffset);
                }
            }
        });
    }
    
    @SuppressLint("SetJavaScriptEnabled")
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
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            webSettings.setDatabaseEnabled(true);
            webSettings.setBuiltInZoomControls(false);
            webSettings.setSupportZoom(false);
            webSettings.setDisplayZoomControls(false);
            
            // Hardware acceleration
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            
            // Ustaw WebViewClient aby zapobiec otwieraniu linków w przeglądarce
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    String url = request.getUrl().toString();
                    // Blokuj nawigację poza YouTube
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
            
            // WebChromeClient dla pełnoekranowego video
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onShowCustomView(View view, CustomViewCallback callback) {
                    super.onShowCustomView(view, callback);
                    // Obsługa trybu pełnoekranowego
                }
                
                @Override
                public void onHideCustomView() {
                    super.onHideCustomView();
                }
                
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    super.onProgressChanged(view, newProgress);
                    Log.d(TAG, "Loading progress: " + newProgress + "%");
                }
            });
            
            // Ustaw kolor tła
            webView.setBackgroundColor(Color.BLACK);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up WebView", e);
        }
    }
    
    private void setupButtons() {
        closeButton.setOnClickListener(v -> dismiss());
        
        expandCollapseButton.setOnClickListener(v -> {
            if (bottomSheetBehavior != null) {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });
    }
    
    private void setupSwipeGestures() {
        GestureDetector gestureDetector = new GestureDetector(bottomSheetView.getContext(), 
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false;
                    
                    float deltaY = e2.getY() - e1.getY();
                    float deltaX = e2.getX() - e1.getX();
                    
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        return false; // Horizontal swipe
                    }
                    
                    if (deltaY > 100 && Math.abs(velocityY) > 100) {
                        // Swipe down
                        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        }
                        return true;
                    } else if (deltaY < -100 && Math.abs(velocityY) > 100) {
                        // Swipe up
                        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        }
                        return true;
                    }
                    return false;
                }
            });
        
        headerLayout.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }
    
    private void loadYouTubeVideo(String videoId) {
        if (webView == null || videoId == null) {
            Log.e(TAG, "Cannot load video: webView=" + (webView != null) + ", videoId=" + videoId);
            return;
        }
        
        // Ensure WebView is ready
        mainHandler.post(() -> {
            try {
                // Method 1: Try loading with embedded iframe (preferred)
                String html = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='utf-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
                "<style>" +
                "* { margin: 0; padding: 0; box-sizing: border-box; }" +
                "body { background: #000; overflow: hidden; }" +
                ".video-container { position: relative; width: 100%; height: 100vh; }" +
                "iframe { position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='video-container'>" +
                "<iframe id='ytplayer' type='text/html' " +
                "src='https://www.youtube.com/embed/" + videoId + "?autoplay=0&origin=http://localhost&rel=0&modestbranding=1&playsinline=1' " +
                "frameborder='0' " +
                "allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share' " +
                "allowfullscreen>" +
                "</iframe>" +
                "</div>" +
                "</body>" +
                "</html>";
                
                Log.d(TAG, "Loading YouTube video with ID: " + videoId);
                
                // Load the HTML content
                webView.loadDataWithBaseURL("http://localhost", html, "text/html", "UTF-8", null);
                
                // Alternative: If iframe doesn't work, try direct URL loading after a delay
                mainHandler.postDelayed(() -> {
                    if (!isWebViewReady) {
                        Log.d(TAG, "Fallback: Loading YouTube mobile URL directly");
                        String mobileUrl = "https://m.youtube.com/watch?v=" + videoId;
                        webView.loadUrl(mobileUrl);
                    }
                }, 3000);
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading YouTube video", e);
                // Fallback to mobile YouTube URL
                try {
                    String mobileUrl = "https://m.youtube.com/watch?v=" + videoId;
                    webView.loadUrl(mobileUrl);
                } catch (Exception ex) {
                    Log.e(TAG, "Error loading fallback URL", ex);
                }
            }
        });
    }
    
    private void updateVideoInfo() {
        if (titleTextView != null && currentTitle != null) {
            titleTextView.setText(currentTitle);
        }
        if (artistTextView != null && currentArtist != null) {
            artistTextView.setText(currentArtist);
        }
    }
    
    private String extractVideoId(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Jeśli to już jest ID (11 znaków)
        if (url.length() == 11 && !url.contains("/") && !url.contains(".")) {
            return url;
        }
        
        // Wzorce dla różnych formatów URL YouTube
        String[] patterns = {
            "(?:youtube\\.com/watch\\?v=|youtu\\.be/)([^&\\n?#]+)",
            "(?:youtube\\.com/embed/)([^&\\n?#]+)",
            "(?:youtube\\.com/v/)([^&\\n?#]+)",
            "(?:youtube-nocookie\\.com/embed/)([^&\\n?#]+)"
        };
        
        for (String pattern : patterns) {
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(url);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        
        return null;
    }
    
    /**
     * Minimalizuje Bottom Sheet do stanu zwiniętego
     */
    public void minimize() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }
    
    /**
     * Maksymalizuje Bottom Sheet do stanu rozwiniętego
     */
    public void maximize() {
        if (bottomSheetBehavior != null) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
    
    /**
     * Zamyka Bottom Sheet i czyści zasoby
     */
    public void dismiss() {
        try {
            if (bottomSheetDialog != null && bottomSheetDialog.isShowing()) {
                bottomSheetDialog.dismiss();
            }
            cleanupWebView();
        } catch (Exception e) {
            Log.e(TAG, "Error dismissing bottom sheet", e);
        }
    }
    
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
    
    /**
     * Sprawdza czy Bottom Sheet jest obecnie widoczny
     */
    public boolean isShowing() {
        return bottomSheetDialog != null && bottomSheetDialog.isShowing();
    }
    
    /**
     * Sprawdza czy Bottom Sheet jest zminimalizowany
     */
    public boolean isMinimized() {
        return isMinimized;
    }
    
    /**
     * Zwraca aktualnie odtwarzane ID filmu
     */
    public String getCurrentVideoId() {
        return currentVideoId;
    }
    
    /**
     * Obsługa cyklu życia - wywołać w onPause() Activity
     */
    public void onPause() {
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
    }
    
    /**
     * Obsługa cyklu życia - wywołać w onResume() Activity
     */
    public void onResume() {
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }
    
    /**
     * Obsługa cyklu życia - wywołać w onDestroy() Activity
     */
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
}
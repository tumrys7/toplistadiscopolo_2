package com.grandline.toplistadiscopolo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.appbar.MaterialToolbar;

public class PlayActivity extends AppCompatActivity {

    private WebView webView;
    AdView mAdView;
    private String spotifyUrl;
    private String title;
    private String artist;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_play);

        // Wywołanie metody, ustawiającej czarne ikony i przezroczyste paski systemowe
        getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                setLightSystemBars(getWindow(), true, true);
            }
        });

        // Get data from intent
        Bundle extras = getIntent() != null ? getIntent().getExtras() : null;
        if (extras != null) {
            spotifyUrl = extras.getString("spotify_url");
            title = extras.getString("title");
            artist = extras.getString("artist");
        }

        // Initialize views
        MaterialToolbar toolbar = findViewById(R.id.play_toolbar);
        TextView titleTextView = findViewById(R.id.play_title);
        TextView artistTextView = findViewById(R.id.play_artist);
     //   TextView openSpotifyTextView = findViewById(R.id.play_open_label);
    //    TextView urlTextView = findViewById(R.id.play_url_text);
        webView = findViewById(R.id.play_webview);

        toolbar.setTitle(getString(R.string.spotify));
// jeśli dostępne (w nowoczesnych wersjach) ustaw:
        toolbar.setTitleCentered(true);

        // Toolbar setup with back button
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.arrow_back);
            }
            toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        final View root = findViewById(R.id.root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), sysBars.top, v.getPaddingRight(), sysBars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        // Set title and artist
        if (!TextUtils.isEmpty(title)) {
            titleTextView.setText(title);
        }
        if (!TextUtils.isEmpty(artist)) {
            artistTextView.setText(artist);
        }

        if (!Constants.VERSION_PRO_DO_NOT_SHOW_BANNER) {
            createAd();
          //  createNativeAd();
        }

        // Setup WebView
        setupWebView();

        // Load Spotify URL - convert to embed format if needed
       if (!TextUtils.isEmpty(spotifyUrl)) {
          String embedUrl = convertToEmbedUrl(spotifyUrl);
            // Prefer loading an iframe wrapper for better control
          String html = createMusicWrapper(embedUrl);
           webView.loadDataWithBaseURL("https://open.spotify.com", html, "text/html", "utf-8", null);

            // Setup bottom link area
        //    openSpotifyTextView.setText(getString(R.string.spotify_go_to_app));
       //     urlTextView.setText(spotifyUrl);
       //    View.OnClickListener openInSpotify = v -> openInSpotifyApp(spotifyUrl);
       //     openSpotifyTextView.setOnClickListener(openInSpotify);
       //     urlTextView.setOnClickListener(openInSpotify);
        }
   }

    public void setLightSystemBars(Window window, boolean lightStatusBar, boolean lightNavigationBar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Fallback to deprecated method for API 23-29
            int flags = window.getDecorView().getSystemUiVisibility();
            if (lightStatusBar) {
                flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            } else {
                flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (lightNavigationBar) {
                    flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags &= ~android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
            }
            window.getDecorView().setSystemUiVisibility(flags);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            }
        }
    }
    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

//    private void openInSpotifyApp(String url) {
//        try {
//            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//            intent.setPackage("com.spotify.music");
//            startActivity(intent);
//        } catch (ActivityNotFoundException e) {
//            // Fallback to default handler (browser)
//            Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//          startActivity(fallback);
//        }
//    }

    private boolean handleUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;
        try {
            if (url.startsWith("spotify:") || url.contains("open.spotify.com")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setPackage("com.spotify.music");
                startActivity(intent);
                return true;
            }
            if (url.startsWith("intent://")) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent != null) {
                        startActivity(intent);
                        return true;
                    }
                } catch (Exception ignored) { }
            }
        } catch (ActivityNotFoundException e) {
            Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(fallback);
            return true;
        }
        return false;
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        // Enable DOM storage and database for better music support
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        // Enable media playback
        settings.setMediaPlaybackRequiresUserGesture(false);
        // Updated User-Agent to latest Chrome version for better compatibility
        settings.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36");
        // Enable hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        // Enable mixed content for better compatibility
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest request) {
                return handleUrl(request.getUrl() != null ? request.getUrl().toString() : null);
            }
        });

        // Add WebChromeClient to handle console messages and filter warnings
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String msg = consoleMessage != null ? consoleMessage.message() : "";
                // Filter out non-critical warnings to reduce noise
                if (msg.contains("A cookie associated with a cross-site resource")
                        || msg.contains("SameSite")
                        || msg.contains("'unsafe-inline' is not a recommended")
                        || msg.contains("crbug")
                        || msg.contains("Autoplay is only allowed")) {
                    return true;
                }
                // Log other console messages for debugging (optional)
                return super.onConsoleMessage(consoleMessage);
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                WebView newWebView = new WebView(view.getContext());
                WebSettings s = newWebView.getSettings();
                s.setJavaScriptEnabled(true);
                newWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        boolean handled = handleUrl(url);
                        view.destroy();
                        return handled;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        if (handleUrl(url)) {
                            view.stopLoading();
                            view.destroy();
                        }
                    }
                });
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }
        });
    }

    private String convertToEmbedUrl(String url) {
        if (TextUtils.isEmpty(url)) return "";

        // Handle spotify: URIs
        if (url.startsWith("spotify:")) {
            String[] parts = url.split(":");
            if (parts.length >= 3) {
                String type = parts[1];
                String id = parts[2];
                return "https://open.spotify.com/embed/" + type + "/" + id;
            }
        }

        // If it already is an embed URL
        if (url.contains("open.spotify.com/embed")) {
            return url;
        }

        // Generic conversion for open.spotify.com links
        if (url.contains("open.spotify.com/")) {
            return url.replace("open.spotify.com/", "open.spotify.com/embed/");
        }

        // As a safe fallback, return as-is
        return url;
    }

    public void createAd(){
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }
    private String createMusicWrapper(String musicUrl) {
        // Simple iframe wrapper for Spotify content
        return "<!DOCTYPE html>" +
                "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>" +
                "<style>html,body{margin:0;padding:0;background:transparent;}" +
                "#wrap{display:flex;align-items:center;justify-content:center;width:100%;}iframe{border:0;width:100%;height:280px;}</style>" +
                "</head><body><div id='wrap'>" +
                "<iframe src='" + musicUrl + "' allowfullscreen allow='autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture' loading='lazy'></iframe>" +
                "</div></body></html>";
    }
}
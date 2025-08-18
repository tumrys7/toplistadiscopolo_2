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
import androidx.core.view.ViewCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeBottomSheetController {
	private static final String TAG = "YouTubeBottomSheet";

	private final WeakReference<Activity> activityRef;
	private final WeakReference<ViewGroup> rootViewRef;
	private BottomSheetBehavior<View> bottomSheetBehavior;
	private WebView webView;
	private LinearLayout bottomSheetView;
	private TextView titleTextView;
	private TextView artistTextView;
	private ImageButton closeButton;
	private ImageButton expandCollapseButton;
	private LinearLayout headerLayout;
	private FrameLayout contentContainer;
	private Handler mainHandler;
	private View customFullScreenView;
	private WebChromeClient.CustomViewCallback customViewCallback;
	private FrameLayout fullScreenContainer;
	private int originalSystemUiVisibility;

	private String currentVideoId;
	private String currentTitle;
	private String currentArtist;
	private boolean isMinimized = false;
	private boolean isWebViewReady = false;

	private static final int COLLAPSED_HEIGHT_DP = 200;
	private static final int EXPANDED_HEIGHT_PERCENT = 85;

	public YouTubeBottomSheetController(Activity activity) {
		this(activity, activity != null ? (ViewGroup) activity.findViewById(R.id.root) : null);
	}

	public YouTubeBottomSheetController(Activity activity, ViewGroup rootView) {
		this.activityRef = new WeakReference<>(activity);
		this.rootViewRef = new WeakReference<>(rootView);
		this.mainHandler = new Handler(Looper.getMainLooper());
	}

	public void showYouTubeVideo(String videoUrlOrId, String title, String artist) {
		Activity activity = activityRef.get();
		if (activity == null || activity.isFinishing()) {
			return;
		}

		String videoId = extractVideoId(videoUrlOrId);
		if (videoId == null || videoId.isEmpty()) {
			Log.e(TAG, "Could not extract video ID from: " + videoUrlOrId);
			return;
		}

		this.currentVideoId = videoId;
		this.currentTitle = title != null ? title : "";
		this.currentArtist = artist != null ? artist : "";

		setupBottomSheet(activity);

		updateVideoInfo();
		loadYouTubeVideo(videoId);

		if (bottomSheetBehavior != null && bottomSheetView != null) {
			bottomSheetView.setVisibility(View.VISIBLE);
			bottomSheetView.bringToFront();
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	private void setupBottomSheet(Context context) {
		if (bottomSheetView != null && bottomSheetBehavior != null) {
			return;
		}

		Activity activity = activityRef.get();
		ViewGroup root = rootViewRef != null ? rootViewRef.get() : null;
		if (root == null && activity != null) {
			root = activity.findViewById(R.id.root);
		}
		if (root == null) {
			Log.e(TAG, "Root view is null; cannot setup YouTube bottom sheet");
			return;
		}

		LayoutInflater inflater = LayoutInflater.from(context);
		View inflated = inflater.inflate(R.layout.youtube_bottom_sheet_layout, root, false);
		LinearLayout container = inflated.findViewById(R.id.youtube_bottom_sheet_container);
		if (container == null) {
			Log.e(TAG, "youtube_bottom_sheet_container not found in layout");
			return;
		}
		ViewGroup tempParent = (ViewGroup) container.getParent();
		if (tempParent != null) {
			tempParent.removeView(container);
		}
		ViewGroup.LayoutParams lp = container.getLayoutParams();
		if (lp instanceof CoordinatorLayout.LayoutParams) {
			CoordinatorLayout.LayoutParams clp = (CoordinatorLayout.LayoutParams) lp;
			clp.setMargins(0, 0, 0, getNavigationBarHeight(context));
			container.setLayoutParams(clp);
		}
		root.addView(container);

		bottomSheetView = container;
		headerLayout = bottomSheetView.findViewById(R.id.youtube_header_layout);
		titleTextView = bottomSheetView.findViewById(R.id.youtube_title);
		artistTextView = bottomSheetView.findViewById(R.id.youtube_artist);
		closeButton = bottomSheetView.findViewById(R.id.youtube_close_button);
		expandCollapseButton = bottomSheetView.findViewById(R.id.youtube_expand_collapse_button);
		contentContainer = bottomSheetView.findViewById(R.id.youtube_content_container);
		webView = bottomSheetView.findViewById(R.id.youtube_webview);

		setupWebView();
		setupButtons();

		bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetView);
		setupBottomSheetBehavior(context);
		setupSwipeGestures();
	}

	private void setupBottomSheetBehavior(Context context) {
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		int screenHeight = dm.heightPixels;
		int collapsedHeight = (int) (COLLAPSED_HEIGHT_DP * dm.density);
		int expandedHeight = (screenHeight * EXPANDED_HEIGHT_PERCENT) / 100;

		bottomSheetBehavior.setPeekHeight(collapsedHeight);
		try {
			bottomSheetBehavior.setMaxHeight(expandedHeight);
		} catch (Throwable ignored) {
			// setMaxHeight may not exist on older versions; ignore
		}
		bottomSheetBehavior.setHideable(true);
		bottomSheetBehavior.setSkipCollapsed(false);
		bottomSheetBehavior.setDraggable(true);

		ViewCompat.setNestedScrollingEnabled(bottomSheetView, false);

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
						// Keep hidden; cleanup happens via dismiss()
						break;
				}
			}

			@Override
			public void onSlide(@NonNull View bottomSheet, float slideOffset) {
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
			webSettings.setMediaPlaybackRequiresUserGesture(true);
			webSettings.setAllowFileAccess(false);
			webSettings.setAllowContentAccess(false);
			webSettings.setLoadWithOverviewMode(true);
			webSettings.setUseWideViewPort(true);
			webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
			webSettings.setDatabaseEnabled(true);
			webSettings.setBuiltInZoomControls(false);
			webSettings.setSupportZoom(false);
			webSettings.setDisplayZoomControls(false);
			webSettings.setJavaScriptCanOpenWindowsAutomatically(false);

			webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

			webView.setWebViewClient(new WebViewClient() {
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
					String url = request.getUrl().toString();
					if (url.contains("youtube.com") || url.contains("youtu.be")) {
						return false;
					}
					return true;
				}

				@Override
				public void onPageFinished(WebView view, String url) {
					isWebViewReady = true;
					Log.d(TAG, "Page finished: " + url);
				}
			});

			webView.setWebChromeClient(new WebChromeClient() {
				@Override
				public void onShowCustomView(View view, CustomViewCallback callback) {
					Activity activity = activityRef.get();
					if (activity == null) {
						callback.onCustomViewHidden();
						return;
					}
					if (customFullScreenView != null) {
						callback.onCustomViewHidden();
						return;
					}
					customFullScreenView = view;
					customViewCallback = callback;
					ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
					fullScreenContainer = new FrameLayout(activity);
					fullScreenContainer.setBackgroundColor(Color.BLACK);
					fullScreenContainer.addView(customFullScreenView, new FrameLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT
					));
					decor.addView(fullScreenContainer, new FrameLayout.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT
					));
					originalSystemUiVisibility = decor.getSystemUiVisibility();
					decor.setSystemUiVisibility(
						View.SYSTEM_UI_FLAG_LAYOUT_STABLE
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
							| View.SYSTEM_UI_FLAG_FULLSCREEN
							| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					);
					if (bottomSheetView != null) {
						bottomSheetView.setVisibility(View.GONE);
					}
				}

				@Override
				public void onHideCustomView() {
					Activity activity = activityRef.get();
					ViewGroup decor = activity != null ? (ViewGroup) activity.getWindow().getDecorView() : null;
					if (decor != null) {
						decor.setSystemUiVisibility(originalSystemUiVisibility);
						if (fullScreenContainer != null) {
							decor.removeView(fullScreenContainer);
							fullScreenContainer = null;
						}
					}
					customFullScreenView = null;
					if (customViewCallback != null) {
						customViewCallback.onCustomViewHidden();
						customViewCallback = null;
					}
					if (bottomSheetView != null) {
						bottomSheetView.setVisibility(View.VISIBLE);
					}
				}
			});

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
		GestureDetector detector = new GestureDetector(bottomSheetView.getContext(), new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				if (e1 == null || e2 == null) return false;
				float deltaY = e2.getY() - e1.getY();
				float deltaX = e2.getX() - e1.getX();
				if (Math.abs(deltaX) > Math.abs(deltaY)) {
					return false;
				}
				if (deltaY > 100 && Math.abs(velocityY) > 100) {
					if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
						bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
					} else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
						bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
					}
					return true;
				} else if (deltaY < -100 && Math.abs(velocityY) > 100) {
					if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
						bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
					}
					return true;
				}
				return false;
			}
		});
		headerLayout.setOnTouchListener((v, event) -> {
			detector.onTouchEvent(event);
			return false;
		});
	}

	private void loadYouTubeVideo(String videoId) {
		if (webView == null || videoId == null) {
			Log.e(TAG, "Cannot load video; webView or videoId null");
			return;
		}
		mainHandler.post(() -> {
			try {
				String html = "<!DOCTYPE html>" +
					"<html><head>" +
					"<meta charset='utf-8'>" +
					"<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>" +
					"<style>*{margin:0;padding:0;box-sizing:border-box;}html,body{width:100%;height:100%;background:#000;overflow:hidden}.video-wrapper{position:relative;padding-bottom:56.25%;height:0;overflow:hidden}.video-wrapper iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style>" +
					"</head><body>" +
					"<div class='video-wrapper'>" +
					"<iframe id='ytplayer' type='text/html' src='https://www.youtube.com/embed/" + videoId + "?rel=0&modestbranding=1&playsinline=1&fs=1' " +
					"frameborder='0' allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share' allowfullscreen></iframe>" +
					"</div>" +
					"</body></html>";
				webView.loadDataWithBaseURL("http://localhost", html, "text/html", "UTF-8", null);
			} catch (Exception e) {
				Log.e(TAG, "Error loading iframe, falling back to mobile URL", e);
				String mobileUrl = "https://m.youtube.com/watch?v=" + videoId;
				webView.loadUrl(mobileUrl);
			}
		});
	}

	private void updateVideoInfo() {
		if (titleTextView != null) {
			titleTextView.setText(currentTitle != null ? currentTitle : "");
		}
		if (artistTextView != null) {
			artistTextView.setText(currentArtist != null ? currentArtist : "");
		}
	}

	private String extractVideoId(String input) {
		if (input == null || input.isEmpty()) return null;
		if (input.length() == 11 && !input.contains("/") && !input.contains(".")) {
			return input;
		}
		String[] patterns = new String[] {
			"(?:youtube\\.com/watch\\?v=|youtu\\.be/)([^&\\n?#]+)",
			"(?:youtube\\.com/embed/)([^&\\n?#]+)",
			"(?:youtube\\.com/v/)([^&\\n?#]+)",
			"(?:youtube-nocookie\\.com/embed/)([^&\\n?#]+)"
		};
		for (String p : patterns) {
			Pattern cp = Pattern.compile(p);
			Matcher m = cp.matcher(input);
			if (m.find()) return m.group(1);
		}
		return null;
	}

	public void minimize() {
		if (bottomSheetBehavior != null) {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
		}
	}

	public void maximize() {
		if (bottomSheetBehavior != null) {
			bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
		}
	}

	public void dismiss() {
		try {
			if (bottomSheetBehavior != null) {
				bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
			}
			if (bottomSheetView != null) {
				bottomSheetView.setVisibility(View.GONE);
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
				Log.e(TAG, "Error cleaning WebView", e);
			}
		}
	}

	public boolean isShowing() {
		return bottomSheetView != null && bottomSheetView.getVisibility() == View.VISIBLE && bottomSheetBehavior != null && bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN;
	}

	public boolean isMinimized() {
		return isMinimized;
	}

	public String getCurrentVideoId() {
		return currentVideoId;
	}

	public void onPause() {
		if (webView != null) {
			webView.onPause();
			webView.pauseTimers();
		}
	}

	public void onResume() {
		if (webView != null) {
			webView.onResume();
			webView.resumeTimers();
		}
	}

	public void onDestroy() {
		try {
			dismiss();
			if (webView != null) {
				ViewGroup parent = (ViewGroup) webView.getParent();
				if (parent != null) parent.removeView(webView);
				webView.destroy();
				webView = null;
			}
			if (bottomSheetView != null) {
				ViewGroup parent = (ViewGroup) bottomSheetView.getParent();
				if (parent != null) parent.removeView(bottomSheetView);
				bottomSheetView = null;
			}
			bottomSheetBehavior = null;
		} catch (Exception e) {
			Log.e(TAG, "Error in onDestroy", e);
		} finally {
			Activity activity = activityRef.get();
			if (activity != null && activity.isFinishing()) {
				activityRef.clear();
			}
		}
	}

	private int getNavigationBarHeight(Context context) {
		int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			return context.getResources().getDimensionPixelSize(resourceId);
		}
		return 0;
	}
}
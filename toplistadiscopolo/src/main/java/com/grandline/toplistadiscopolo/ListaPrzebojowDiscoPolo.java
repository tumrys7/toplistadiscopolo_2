package com.grandline.toplistadiscopolo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.grandline.toplistadiscopolo.adapters.TabPagerAdapter;
import com.grandline.toplistadiscopolo.fragments.ListaFragment;
import com.grandline.toplistadiscopolo.fragments.MojaListaFragment;
import com.grandline.toplistadiscopolo.fragments.NotowaniaFragment;
import com.grandline.toplistadiscopolo.fragments.NowosciFragment;
import com.grandline.toplistadiscopolo.fragments.PoczekalniaFragment;
import com.grandline.toplistadiscopolo.fragments.WykonawcyFragment;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ListaPrzebojowDiscoPolo extends AppCompatActivity  {

	private static final String TAG = "MyActivity";
	private boolean adReward;
	private RewardedAd rewardedAd;
	// RewardedVideoAd mAdRewarded;
	AdView adView;
	AdView mAdView;
	boolean adError;
	AdRequest adRequest;

	// ExecutorService and Handler for background tasks
	private ExecutorService executorService;
	private Handler mainHandler;
	private volatile boolean isDataLoading = false; // Prevent concurrent data loading

	private TabPagerAdapter tabPagerAdapter;
	private TabLayoutMediator tabLayoutMediator;
	
	// Spotify Bottom Sheet Controller
	private SpotifyBottomSheetController spotifyBottomSheetController;
	// YouTube Bottom Sheet Controller
	private YouTubeBottomSheetController youTubeBottomSheetController;

	// Activity result launcher
	private ActivityResultLauncher<Intent> activityResultLauncher;

	ListView wykUtwory;
	String teledysk;
	String spotify;
	String androidId;
	String myListType;
	String myIdWykonawcy;
	String votingListId;
	String language;
	String notowanieId;
	String glosTeledysk;
	AlertDialog progressDialog;
	AlertDialog progressDialogVote;
	boolean isLoading;
	public ArrayList<HashMap<String, String>> songsList;
	public ArrayList<HashMap<String, String>> songsListPocz;
	public ArrayList<HashMap<String, String>> songsListNowosci;
	public ArrayList<HashMap<String, String>> songsListMojalista;
	public ArrayList<HashMap<String, String>> wykonList;
	public ArrayList<HashMap<String, String>> filteredWykonList;
	//public ArrayList<HashMap<String, String>> swiateczneList;
	public ArrayList<HashMap<String, String>> notowaniaList;
	public ArrayList<HashMap<String, String>> notowPrzedzialyList;
	public ArrayList<String> listNotowPrzedzialy;
	public String info;
	public String info2012;
	public boolean CheckboxPreference;

	private Toast toast;
	private long lastBackPressTime = 0;

	int spinnerPosition = -1;
	// [START declare_analytics]
	private FirebaseAnalytics mFirebaseAnalytics;
	// [END declare_analytics]


	// Włączenie na android >=7 Dangerous permission zapis okładek w pamięci urządzenia
	// Copyright Jarek Wołoszyn 04-11-2017 r
	// Storage Permissions variables
	private static final int REQUEST_EXTERNAL_STORAGE = 1;
	private static final String[] PERMISSIONS_STORAGE = {
			"android.permission.WRITE_EXTERNAL_STORAGE",
			"android.permission.READ_EXTERNAL_STORAGE"
	};
	//persmission method.
	public static void verifyStoragePermissions(Activity activity) {
		// Check if we have read or write permission
		int writePermission = ActivityCompat.checkSelfPermission(activity, "android.permission.WRITE_EXTERNAL_STORAGE");
		int readPermission = ActivityCompat.checkSelfPermission(activity, "android.permission.READ_EXTERNAL_STORAGE");

		if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED) {
			// We don't have permission so prompt the user
			ActivityCompat.requestPermissions(
					activity,
					PERMISSIONS_STORAGE,
					REQUEST_EXTERNAL_STORAGE
			);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//FacebookSdk.sdkInitialize(getApplicationContext());
		//AppEventsLogger.activateApp(this);

		// Enable edge-to-edge and manage system bar insets
		EdgeToEdge.enable(this);

		// Initialize ExecutorService and Handler
		executorService = Executors.newFixedThreadPool(3);
		mainHandler = new Handler(Looper.getMainLooper());

		// Initialize ActivityResultLauncher
		activityResultLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					if (result.getResultCode() == RESULT_OK && result.getData() != null) {
						Bundle conData = result.getData().getExtras();
						if (conData != null) {
							boolean voted = conData.getBoolean("param_return", false);
							if (voted) {
								refreshListBackground();
							}
						}
					}
				}
		);

		language = getLocaleSettings();
		notowanieId = Constants.VALUE_START_NOTOWANIE_ID;
		//setLocale(language);
		setContentView(R.layout.main);

		// Wywołanie metody, ustawiającej czarne ikony i przezroczyste paski systemowe
		getWindow().getDecorView().post(() -> setLightSystemBars(getWindow(), true, true));

		final View root = findViewById(R.id.root);
		if (root != null) {
		ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
			Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
			v.setPadding(
					v.getPaddingLeft(),    // zostawia istniejący padding po lewej
					sysBars.top,           // ustawia padding na górze wg paska statusu
					v.getPaddingRight(),   // zostawia istniejący padding po prawej
					v.getPaddingBottom()   // pozostawia istniejący padding na dole bez zmian
			);
			Insets navBarInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
			lp.bottomMargin = navBarInsets.bottom; // dynamicznie wg systemowego navigation bar
			v.setLayoutParams(lp);
			return insets;
		});
		}

		// Włączenie na android >=7 Dangerous permission: zapis okładek w pamięci urządzenia
		verifyStoragePermissions(this);

		// Remove deprecated system UI light status bar flag usage; rely on theme for contrast
		// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
		// 	getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		// }

		// Using a more privacy-friendly approach instead of device identifier
		SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
		androidId = prefs.getString("user_id", null);
		if (androidId == null) {
			androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
			if (androidId != null) {
				prefs.edit().putString("user_id", androidId).apply();
			} else {
				// Fallback to a default value if ANDROID_ID is also null
				androidId = "unknown_device";
				androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
				prefs.edit().putString("user_id", androidId).apply();
			}
		}
		// [START shared_app_measurement]
		// Log package information for debugging
		logPackageInfo();
		
		// Check Google Play Services availability before initializing Firebase
		if (isGooglePlayServicesAvailable()) {
			try {
				// Obtain the FirebaseAnalytics instance.
				mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
				Log.i(TAG, "Firebase Analytics initialized successfully");
				
				// [START user_property]
				if (androidId != null) {
					mFirebaseAnalytics.setUserProperty("android_id", androidId);
				}
				// [END user_property]
			} catch (Exception e) {
				Log.e(TAG, "Failed to initialize Firebase Analytics: " + e.getMessage(), e);
				mFirebaseAnalytics = null;
			}
		} else {
			Log.w(TAG, "Google Play Services not available, skipping Firebase Analytics initialization");
			mFirebaseAnalytics = null;
		}
		// [END shared_app_measurement]


		setupTabLayoutWithViewPager();
		
		// Initialize Spotify Bottom Sheet Controller
		ViewGroup rootView = findViewById(R.id.root);
		if (rootView != null) {
			spotifyBottomSheetController = new SpotifyBottomSheetController(this, rootView);
			youTubeBottomSheetController = new YouTubeBottomSheetController(this, rootView);
		}

		//only for Free Version
		if (!Constants.VERSION_PRO_DO_NOT_SHOW_BANNER) {
			// reklama
			createAd();
		}
		refreshListBackground();
		getPrefs();
		setupBackPressedCallback();

	}

	public void setLightSystemBars(Window window, boolean lightStatusBar, boolean lightNavigationBar) {
        // Fallback to deprecated method for API 23-29
        int flags = window.getDecorView().getSystemUiVisibility();
        if (lightStatusBar) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (lightNavigationBar) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
        }
        window.getDecorView().setSystemUiVisibility(flags);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        }
    }

	public void onStart () {
		super.onStart();
		final SharedPreferences settings =
				getSharedPreferences("localPreferences", MODE_PRIVATE);
		if (settings.getBoolean("isFirstRun", true)) {
			new AlertDialog.Builder(ListaPrzebojowDiscoPolo.this)
					.setTitle(R.string.text_cookie_title)
					.setMessage(R.string.text_cookie)
					.setNeutralButton(R.string.text_cookie_close, null ).show();
			settings.edit().putBoolean("isFirstRun", false).apply();

		} adReward = false;
	}


	@Override
	public void onDestroy() {
		try {
			// Clean up ads to prevent input channel issues
			if (adView != null) {
				adView.destroy();
				adView = null;
			}
			if (mAdView != null) {
				mAdView.destroy();
				mAdView = null;
			}
			
			// Clean up dialogs to prevent window leaks
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
				progressDialog = null;
			}
			if (progressDialogVote != null && progressDialogVote.isShowing()) {
				progressDialogVote.dismiss();
				progressDialogVote = null;
			}
			
			// Clean up TabLayoutMediator
			if (tabLayoutMediator != null) {
				tabLayoutMediator.detach();
				tabLayoutMediator = null;
			}
			
			// Clean up ViewPager2 and adapter
			ViewPager2 viewPager = findViewById(R.id.viewPager);
			if (viewPager != null) {
				viewPager.setAdapter(null);
			}
			if (tabPagerAdapter != null) {
				tabPagerAdapter = null;
			}
			
			// Clean up rewarded ad
			if (rewardedAd != null) {
				rewardedAd.setFullScreenContentCallback(null);
				rewardedAd = null;
			}
			
			// Clear focus and input connections
			if (getCurrentFocus() != null) {
				getCurrentFocus().clearFocus();
			}
			
			// Clean up ExecutorService to prevent memory leaks
			if (executorService != null && !executorService.isShutdown()) {
				executorService.shutdown();
				try {
					if (!executorService.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)) {
						executorService.shutdownNow();
					}
				} catch (InterruptedException e) {
					executorService.shutdownNow();
				}
				executorService = null;
			}
			
			// Clean up handler
			if (mainHandler != null) {
				mainHandler.removeCallbacksAndMessages(null);
				mainHandler = null;
			}

				if (spotifyBottomSheetController != null) {
					spotifyBottomSheetController.onDestroy();
				}
				if (youTubeBottomSheetController != null) {
					youTubeBottomSheetController.onDestroy();
				}

		} catch (Exception e) {
			Log.e("InputCleanup", "Error in onDestroy cleanup: " + e.getMessage());
		}
		
		super.onDestroy();
	}

	@Override
	public void onResume() {
		///rewardedAd.getAdMetadata();
		super.onResume();
		
		// Resume YouTube bottom sheet WebView timers safely
		if (youTubeBottomSheetController != null) {
			youTubeBottomSheetController.onResume();
		}
		
		// Check if user returned from Spotify and try to reconnect if needed
		if (spotifyBottomSheetController != null) {
			SpotifyService spotifyService = SpotifyService.getInstance(this);
			// If user was trying to play something and Spotify is not connected, try to reconnect
			if (!spotifyService.isConnected() && !spotifyService.isConnecting()) {
				// Small delay to ensure app is fully resumed and any authorization callback is processed
				new Handler(Looper.getMainLooper()).postDelayed(() -> {
					if (spotifyBottomSheetController.isBottomSheetVisible()) {
						Log.d(TAG, "User returned from Spotify, attempting to force reconnect...");
						
						// Add a connection listener to retry the track after successful connection
						spotifyService.addConnectionListener(new SpotifyService.SpotifyConnectionListener() {
							@Override
							public void onConnected() {
								Log.d(TAG, "Connected after user return, retrying track");
								// Retry the current track
								if (spotifyBottomSheetController != null) {
									spotifyBottomSheetController.retryCurrentTrack();
								}
								// Remove this listener
								spotifyService.removeConnectionListener(this);
							}
							
							@Override
							public void onConnectionFailed(Throwable error) {
								Log.e(TAG, "Connection failed after user return", error);
								// Remove this listener
								spotifyService.removeConnectionListener(this);
							}
							
							@Override
							public void onDisconnected() {
								// Remove this listener
								spotifyService.removeConnectionListener(this);
							}
						});
						
						spotifyService.forceReconnect();
					}
				}, 2000); // Increased delay to 2 seconds to allow for authorization processing
			}
		}

		// Resume ads safely
		try {
			if (mAdView != null) {
				mAdView.resume();
			}
		} catch (Exception e) {
			Log.e("InputCleanup", "Error resuming ads: " + e.getMessage());
		}
	}

	@Override
	public void onPause() {
		//rewardedAd.pause(this);
		// Pause YouTube bottom sheet WebView timers safely
		if (youTubeBottomSheetController != null) {
			youTubeBottomSheetController.onPause();
		}


		super.onPause();
	}

	@Override
	public void onStop() {
		// Clean up input channels and prevent window leaks
		try {
			// Pause ads to prevent input channel issues
			if (mAdView != null) {
				mAdView.pause();
			}
			
			// Clear focus from any focused views
			if (getCurrentFocus() != null) {
				getCurrentFocus().clearFocus();
			}
			
		} catch (Exception e) {
			Log.e("InputCleanup", "Error in onStop cleanup: " + e.getMessage());
		}
		super.onStop();
	}

	@Override
	public void finish() {
		// Clear focus before finishing to prevent input channel issues
		try {
			if (getCurrentFocus() != null) {
				getCurrentFocus().clearFocus();
			}
			
			// Hide any showing dialogs
			if (progressDialog != null && progressDialog.isShowing()) {
				progressDialog.dismiss();
			}
			if (progressDialogVote != null && progressDialogVote.isShowing()) {
				progressDialogVote.dismiss();
			}
			
		} catch (Exception e) {
			Log.e("InputCleanup", "Error in finish cleanup: " + e.getMessage());
		}
		
		super.finish();
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Clear focus during configuration changes to prevent input channel issues
		try {
			if (getCurrentFocus() != null) {
				getCurrentFocus().clearFocus();
			}
		} catch (Exception e) {
			Log.e("InputCleanup", "Error in configuration change: " + e.getMessage());
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);
		
		// Handle Spotify authorization callback
		if (intent != null && intent.getData() != null) {
			String scheme = intent.getData().getScheme();
			String host = intent.getData().getHost();
			
			// Check if this is a Spotify callback
			if ("com.grandline.toplistadiscopolo".equals(scheme) && "callback".equals(host)) {
				Log.d(TAG, "Received Spotify authorization callback");
				
				// The Spotify SDK will handle the callback automatically, 
				// but we can add a small delay before attempting reconnection
				// to ensure the authorization is fully processed
				new Handler(Looper.getMainLooper()).postDelayed(() -> {
					if (spotifyBottomSheetController != null && spotifyBottomSheetController.isBottomSheetVisible()) {
						SpotifyService spotifyService = SpotifyService.getInstance(this);
						if (!spotifyService.isConnected() && !spotifyService.isConnecting()) {
							Log.d(TAG, "Authorization callback received, attempting reconnect...");
							
							// Add a connection listener to retry the track after successful connection
							spotifyService.addConnectionListener(new SpotifyService.SpotifyConnectionListener() {
								@Override
								public void onConnected() {
									Log.d(TAG, "Connected after authorization callback, retrying track");
									// Retry the current track
									if (spotifyBottomSheetController != null) {
										spotifyBottomSheetController.retryCurrentTrack();
									}
									// Remove this listener
									spotifyService.removeConnectionListener(this);
								}
								
								@Override
								public void onConnectionFailed(Throwable error) {
									Log.e(TAG, "Connection failed after authorization callback", error);
									// Remove this listener
									spotifyService.removeConnectionListener(this);
								}
								
								@Override
								public void onDisconnected() {
									// Remove this listener
									spotifyService.removeConnectionListener(this);
								}
							});
							
							spotifyService.forceReconnect();
						}
					}
				}, 1500); // Slightly longer delay to ensure authorization is processed
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		// Handle Spotify OAuth authorization result
		if (requestCode == SpotifyAuthManager.REQUEST_CODE) {
			Log.d(TAG, "Received Spotify OAuth PKCE authorization result - RequestCode: " + requestCode + ", ResultCode: " + resultCode);
			
			// Handle the authorization response through SpotifyAuthManager
			SpotifyAuthManager authManager = SpotifyAuthManager.getInstance(this);
			authManager.handleAuthorizationResponse(requestCode, resultCode, data);
			
			// Note: The SpotifyAuthManager will handle the success/failure callbacks
			// and the success callback will trigger the App Remote connection
			return;
		}

		boolean voted = data.getBooleanExtra("param_return",false);
		if (voted){
			if (CheckboxPreference){
				refreshListBackground();
			}
		}
	}

	//funkcja podwojnego nacisniecia wstecz aby wyjsc z aplikacji
	private void setupBackPressedCallback() {
		OnBackPressedCallback callback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				if (lastBackPressTime < System.currentTimeMillis() - 3000) {
					toast = Toast.makeText(ListaPrzebojowDiscoPolo.this, R.string.text_exit_app, Toast.LENGTH_SHORT);
					toast.show();
					lastBackPressTime = System.currentTimeMillis();
				} else {
					if (toast != null) {
						toast.cancel();
					}
					finish();
				}
			}
		};
		getOnBackPressedDispatcher().addCallback(this, callback);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	private void zaglosuj(String idUtworu, String listType, String idWykonawcy, String idGrupy, String teledysk) {
		if (canUserVotes(idUtworu)){
			// Using the same privacy-friendly user ID
			SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
			String androidId = prefs.getString("user_id", this.androidId);
			String url;
			try {
				url = Constants.VOTE_URL.replace("ID_LISTY", idUtworu).replace("DEV_ID", androidId).replace("LANG", language).replace("ID_GRUPY", idGrupy).replace("TEL_PARAM", teledysk);
			} catch (NullPointerException e) {
				url = Constants.VOTE_URL.replace("ID_LISTY", idUtworu).replace("DEV_ID", "UNKNOWN").replace("LANG", language).replace("ID_GRUPY", idGrupy).replace("TEL_PARAM", teledysk);
			}
					// Safely create and show vote progress dialog
		try {
			progressDialogVote = createProgressDialog(getString(R.string.text_voting));
			if (progressDialogVote != null && !isFinishing() && !isDestroyed()) {
				progressDialogVote.show();
			}
		} catch (Exception e) {
			Log.e("DialogShow", "Error showing vote progress dialog: " + e.getMessage());
		}
			myListType = listType;
			myIdWykonawcy = idWykonawcy;
			votingListId = idUtworu;
			voteInBackground(url, listType);
			// [START image_view_event]
			if (mFirebaseAnalytics != null) {
				Bundle bundle = new Bundle();
				bundle.putString(FirebaseAnalytics.Param.ITEM_ID, idUtworu);
				bundle.putString(FirebaseAnalytics.Param.GROUP_ID, androidId);
				bundle.putString(FirebaseAnalytics.Param.LEVEL, teledysk);
				bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "glos_lista");
				mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
			}
			// [END image_view_event]

		} else {
			if (!Objects.equals(teledysk, "1")){
				Toast.makeText(getApplicationContext(), getString(R.string.text_too_many_votes).replace("VOTES_INTERVAL",Integer.toString(Constants.VOTES_INTERVAL)), Toast.LENGTH_LONG).show();
			//	Snackbar snackbar = make(ListaPrzebojowDiscoPolo.this.getCurrentFocus(), getString(R.string.text_too_many_votes).replace("VOTES_INTERVAL",Integer.toString(Constants.VOTES_INTERVAL)),  Snackbar.LENGTH_LONG);
			//	snackbar.show();
			}
		}


	}

	public void refreshListBackground(){
		// Prevent concurrent data loading to avoid threading issues
		if (isDataLoading) {
			return;
		}
		
		// Check if activity is still valid
		if (isFinishing() || isDestroyed()) {
			return;
		}
		
		isDataLoading = true;
		
		// Initialize MobileAds with proper error handling
		if (isGooglePlayServicesAvailable()) {
			try {
				MobileAds.initialize(this, initializationStatus -> {
					Log.i(TAG, "MobileAds initialized successfully");
					// You can check initialization status here if needed
					Map<String, com.google.android.gms.ads.initialization.AdapterStatus> statusMap = 
						initializationStatus.getAdapterStatusMap();
					for (String adapterClass : statusMap.keySet()) {
						com.google.android.gms.ads.initialization.AdapterStatus status = statusMap.get(adapterClass);
						Log.d(TAG, String.format("Adapter name: %s, Description: %s, Latency: %d",
							adapterClass, status.getDescription(), status.getLatency()));
					}
				});
			} catch (Exception e) {
				Log.e(TAG, "Failed to initialize MobileAds: " + e.getMessage(), e);
			}
		} else {
			Log.w(TAG, "Google Play Services not available, skipping MobileAds initialization");
		}

		loadRewardedAd();

		// Clear existing data lists instead of creating new instances
		// This preserves the adapter references
		if (songsList == null) songsList = new ArrayList<>();
		else synchronized(songsList) { songsList.clear(); }

		if (songsListPocz == null) songsListPocz = new ArrayList<>();
		else synchronized(songsListPocz) { songsListPocz.clear(); }

		if (songsListNowosci == null) songsListNowosci = new ArrayList<>();
		else synchronized(songsListNowosci) { songsListNowosci.clear(); }

		if (songsListMojalista == null) songsListMojalista = new ArrayList<>();
		else synchronized(songsListMojalista) { songsListMojalista.clear(); }

		if (wykonList == null) wykonList = new ArrayList<>();
		else synchronized(wykonList) { wykonList.clear(); }

		if (filteredWykonList == null) filteredWykonList = new ArrayList<>();
		else synchronized(filteredWykonList) { filteredWykonList.clear(); }

		if (notowaniaList == null) notowaniaList = new ArrayList<>();
		else synchronized(notowaniaList) { notowaniaList.clear(); }

		if (notowPrzedzialyList == null) notowPrzedzialyList = new ArrayList<>();
		else synchronized(notowPrzedzialyList) { notowPrzedzialyList.clear(); }

		if (listNotowPrzedzialy == null) listNotowPrzedzialy = new ArrayList<>();
		else synchronized(listNotowPrzedzialy) { listNotowPrzedzialy.clear(); }

		// Safely create and show progress dialog
		try {
			progressDialog = createProgressDialog(getString(R.string.text_refresh_list));
			if (progressDialog != null && !isFinishing() && !isDestroyed()) {
				progressDialog.show();
			}
		} catch (Exception e) {
			Log.e("DialogShow", "Error showing progress dialog: " + e.getMessage());
		}
		refreshListInBackground();
		if (!Constants.VERSION_PRO_DO_NOT_SHOW_BANNER) {
			if (adError){
				adView.loadAd(adRequest);
			}
		}

	}

	// Update all fragment adapters when data changes (replaces direct adapter updates)
	public void updateAllFragmentAdapters() {
		updateAllFragmentAdapters(0);
	}
	
	// Overloaded method with retry count for improved fragment update handling
	private void updateAllFragmentAdapters(int retryCount) {
		// Ensure we're on the main UI thread
		if (Looper.myLooper() != Looper.getMainLooper()) {
			if (mainHandler != null) {
				mainHandler.post(() -> updateAllFragmentAdapters(retryCount));
			}
			return;
		}

		// Check if activity is still valid
		if (isFinishing() || isDestroyed()) {
			Log.w("FragmentUpdate", "Activity is finishing or destroyed, skipping adapter updates");
			return;
		}

		try {
			Log.i("FragmentUpdate", "Starting updateAllFragmentAdapters (attempt " + (retryCount + 1) + ")");
			
			// Debug ViewPager2 state
			debugViewPagerState();
			
			// Get fragment references directly on main thread to avoid timing issues
			ListaFragment listaFragment = getFragmentByPosition(TabPagerAdapter.TAB_LISTA);
			PoczekalniaFragment poczekalniaFragment = getFragmentByPosition(TabPagerAdapter.TAB_POCZEKALNIA);
			NowosciFragment nowosciFragment = getFragmentByPosition(TabPagerAdapter.TAB_NOWOSCI);
			MojaListaFragment mojaListaFragment = getFragmentByPosition(TabPagerAdapter.TAB_MOJALISTA);
			WykonawcyFragment wykonawcyFragment = getFragmentByPosition(TabPagerAdapter.TAB_WYKONAWCY);
			NotowaniaFragment notowaniaFragment = getFragmentByPosition(TabPagerAdapter.TAB_NOTOWANIA);

			Log.i("FragmentUpdate", "Fragment references - Lista: " + (listaFragment != null) + 
				", Poczekalnia: " + (poczekalniaFragment != null) + 
				", Nowosci: " + (nowosciFragment != null) + 
				", MojaLista: " + (mojaListaFragment != null) + 
				", Wykonawcy: " + (wykonawcyFragment != null) + 
				", Notowania: " + (notowaniaFragment != null));

			int updatedFragments = 0;

			// Update fragment adapters immediately on main thread
			try {
				if (listaFragment != null && listaFragment.isAdded() && listaFragment.getView() != null) {
					Log.i("FragmentUpdate", "Updating ListaFragment adapter");
					listaFragment.updateAdapter();
					updatedFragments++;
				} else {
					Log.w("FragmentUpdate", "ListaFragment is null or not added");
				}
				
				if (poczekalniaFragment != null && poczekalniaFragment.isAdded() && poczekalniaFragment.getView() != null) {
					Log.i("FragmentUpdate", "Updating PoczekalniaFragment adapter");
					poczekalniaFragment.updateAdapter();
					updatedFragments++;
				} else {
					Log.w("FragmentUpdate", "PoczekalniaFragment is null or not added");
				}
				
				if (nowosciFragment != null && nowosciFragment.isAdded() && nowosciFragment.getView() != null) {
					Log.i("FragmentUpdate", "Updating NowosciFragment adapter");
					nowosciFragment.updateAdapter();
					updatedFragments++;
				} else {
					Log.w("FragmentUpdate", "NowosciFragment is null or not added");
				}
				
				if (mojaListaFragment != null && mojaListaFragment.isAdded() && mojaListaFragment.getView() != null) {
					Log.i("FragmentUpdate", "Updating MojaListaFragment adapter");
					mojaListaFragment.updateAdapter();
					updatedFragments++;
				} else {
					Log.w("FragmentUpdate", "MojaListaFragment is null or not added");
				}
				
				if (wykonawcyFragment != null && wykonawcyFragment.isAdded() && wykonawcyFragment.getView() != null) {
					Log.i("FragmentUpdate", "Updating WykonawcyFragment adapter");
					wykonawcyFragment.updateAdapter();
					updatedFragments++;
				} else {
					Log.w("FragmentUpdate", "WykonawcyFragment is null or not added");
				}
				
				if (notowaniaFragment != null && notowaniaFragment.isAdded() && notowaniaFragment.getView() != null) {
					Log.i("FragmentUpdate", "Updating NotowaniaFragment adapter");
					notowaniaFragment.updateAdapter();
					// Handle spinner for NotowaniaFragment
					notowaniaFragment.updateSpinnerAdapter();
					if (spinnerPosition != -1) {
						notowaniaFragment.setSpinnerSelection(spinnerPosition);
					}
					updatedFragments++;
				} else {
					Log.w("FragmentUpdate", "NotowaniaFragment is null or not added");
				}
				
				Log.i("FragmentUpdate", "Completed updateAllFragmentAdapters successfully. Updated " + updatedFragments + " fragments.");
				
				// If no fragments were found and we haven't retried too many times, schedule a retry
				// This handles ViewPager2 lazy fragment creation with exponential backoff
				if (updatedFragments == 0 && retryCount < 5) { // Increased retry count
					int delay = (int) Math.pow(2, retryCount) * 250; // 250ms, 500ms, 1000ms, 2000ms, 4000ms
					Log.w("FragmentUpdate", "No fragments found, scheduling retry " + (retryCount + 1) + " in " + delay + "ms");
					
					// On the first retry, try to ensure fragments are created
					if (retryCount == 0) {
						Log.i("FragmentUpdate", "First retry - attempting to ensure fragments are created");
						ensureFragmentsCreated();
					}
					
					if (mainHandler != null) {
						mainHandler.postDelayed(() -> updateAllFragmentAdapters(retryCount + 1), delay);
					}
				} else if (updatedFragments == 0) {
					Log.e("FragmentUpdate", "Failed to update any fragments after " + (retryCount + 1) + " attempts");
					// As a last resort, try the force refresh method
					Log.w("FragmentUpdate", "Attempting force refresh as last resort");
					forceRefreshAllFragmentAdapters();
				}
			} catch (Exception e) {
				Log.e("FragmentUpdate", "Error updating fragment adapters: " + e.getMessage(), e);
			}
		} catch (Exception e) {
			Log.e("FragmentUpdate", "Error in updateAllFragmentAdapters: " + e.getMessage(), e);
		}
	}

	// Force refresh all fragment adapters by recreating them
	public void forceRefreshAllFragmentAdapters() {
		// Ensure we're on the main UI thread
		if (Looper.myLooper() != Looper.getMainLooper()) {
			if (mainHandler != null) {
				mainHandler.post(this::forceRefreshAllFragmentAdapters);
			}
			return;
		}

		// Check if activity is still valid
		if (isFinishing() || isDestroyed()) {
			Log.w("FragmentUpdate", "Activity is finishing or destroyed, skipping forced adapter refresh");
			return;
		}

		try {
			Log.i("FragmentUpdate", "Force refreshing all fragment adapters");
			
			// Get fragment references
			ListaFragment listaFragment = getFragmentByPosition(TabPagerAdapter.TAB_LISTA);
			PoczekalniaFragment poczekalniaFragment = getFragmentByPosition(TabPagerAdapter.TAB_POCZEKALNIA);
			NowosciFragment nowosciFragment = getFragmentByPosition(TabPagerAdapter.TAB_NOWOSCI);
			MojaListaFragment mojaListaFragment = getFragmentByPosition(TabPagerAdapter.TAB_MOJALISTA);
			WykonawcyFragment wykonawcyFragment = getFragmentByPosition(TabPagerAdapter.TAB_WYKONAWCY);
			NotowaniaFragment notowaniaFragment = getFragmentByPosition(TabPagerAdapter.TAB_NOTOWANIA);

			// Force refresh fragment adapters
			if (listaFragment != null && listaFragment.isAdded()) {
				Log.i("FragmentUpdate", "Force refreshing ListaFragment adapter");
				listaFragment.refreshAdapter();
			}
			if (poczekalniaFragment != null && poczekalniaFragment.isAdded()) {
				Log.i("FragmentUpdate", "Force refreshing PoczekalniaFragment adapter");
				poczekalniaFragment.refreshAdapter();
			}
			if (nowosciFragment != null && nowosciFragment.isAdded()) {
				Log.i("FragmentUpdate", "Force refreshing NowosciFragment adapter");
				nowosciFragment.refreshAdapter();
			}
			if (mojaListaFragment != null && mojaListaFragment.isAdded()) {
				Log.i("FragmentUpdate", "Force refreshing MojaListaFragment adapter");
				mojaListaFragment.refreshAdapter();
			}
			if (wykonawcyFragment != null && wykonawcyFragment.isAdded()) {
				Log.i("FragmentUpdate", "Force refreshing WykonawcyFragment adapter");
				wykonawcyFragment.refreshAdapter();
			}
			if (notowaniaFragment != null && notowaniaFragment.isAdded()) {
				Log.i("FragmentUpdate", "Force refreshing NotowaniaFragment adapter");
				notowaniaFragment.refreshAdapter();
			}
			
			Log.i("FragmentUpdate", "Completed force refresh of all fragment adapters");
		} catch (Exception e) {
			Log.e("FragmentUpdate", "Error in forceRefreshAllFragmentAdapters: " + e.getMessage(), e);
		}
	}

	// Helper method to get fragment by position
	@SuppressWarnings("unchecked")
	private <T extends Fragment> T getFragmentByPosition(int position) {
		if (tabPagerAdapter != null) {
			ViewPager2 viewPager = findViewById(R.id.viewPager);
			if (viewPager != null) {
				// For ViewPager2 with FragmentStateAdapter, the correct tag format is "f" + itemId
				// Since we override getItemId() to return position, the tag will be "f" + position
				String correctTag = "f" + position;
				
				Fragment fragment = getSupportFragmentManager().findFragmentByTag(correctTag);
				if (fragment != null) {
					Log.d("FragmentRetrieval", "Found fragment at position " + position + " with tag: " + correctTag);
					return (T) fragment;
				}
				
				// If fragment not found by tag, try to force creation by accessing the ViewPager2 adapter
				try {
					// Force ViewPager2 to create the fragment if it hasn't been created yet
					if (position >= 0 && position < TabPagerAdapter.NUM_TABS) {
						// Trigger fragment creation by briefly setting current item if needed
						int currentItem = viewPager.getCurrentItem();
						if (Math.abs(currentItem - position) > viewPager.getOffscreenPageLimit()) {
							Log.d("FragmentRetrieval", "Fragment at position " + position + " not in offscreen limit, forcing creation");
							viewPager.setCurrentItem(position, false);
							viewPager.post(() -> {
								// Restore original position after fragment creation
								viewPager.setCurrentItem(currentItem, false);
							});
						}
						
						// Try finding the fragment again after potential creation
						fragment = getSupportFragmentManager().findFragmentByTag(correctTag);
						if (fragment != null) {
							Log.d("FragmentRetrieval", "Found fragment at position " + position + " after forced creation with tag: " + correctTag);
							return (T) fragment;
						}
					}
				} catch (Exception e) {
					Log.e("FragmentRetrieval", "Error forcing fragment creation: " + e.getMessage(), e);
				}
				
				Log.w("FragmentRetrieval", "Fragment at position " + position + " not found with tag: " + correctTag);
			}
		}
		return null;
	}

	// Handle spinner selection for NotowaniaFragment
	public void handleSpinnerSelection(String notowanieId, int position) {
		this.notowanieId = notowanieId;
		this.spinnerPosition = position;
		// Instead of refreshing all data, only refresh notowania data
		refreshNotowaniaDataBackground();
	}

	// New method to refresh only notowania data for the selected period
	private void refreshNotowaniaDataBackground() {
		// Show progress dialog for notowania refresh
		try {
			if (progressDialog == null || !progressDialog.isShowing()) {
				progressDialog = createProgressDialog(getString(R.string.text_refresh_list));
				if (progressDialog != null && !isFinishing() && !isDestroyed()) {
					progressDialog.show();
				}
			}
		} catch (Exception e) {
			Log.e("DialogShow", "Error showing notowania progress dialog: " + e.getMessage());
		}

		executorService.execute(() -> {
			boolean connectionError = false;

			try {
				XMLParser parser = new XMLParser();

				// Clear only notowania related data
				synchronized(notowaniaList) { notowaniaList.clear(); }

				String xml = parser.getXmlFromUrl(Constants.URL_NOTOWANIA.replace("LANG", language).replace("START_NOTOWANIE_ID", notowanieId));
				Document doc = parser.getDomElement(xml);

				// Update info for notowania
				NodeList nlInfo = doc.getElementsByTagName(Constants.KEY_INFO);
				Element el = (Element) nlInfo.item(0);
				info2012 = parser.getValue(el, Constants.KEY_INFO);

				int votesProgress;
				int maxVotes = 0;
				int currentVotes;

				// Load notowania songs for the selected period
				NodeList nl = doc.getElementsByTagName(Constants.KEY_SONG);

				for (int i = 0; i < nl.getLength(); i++) {
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);

					// Add all required fields for notowania
					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));

					// Only for PRO Version
					if (adReward) {
						map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					}

					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_POSITION, parser.getValue(e, Constants.KEY_POSITION));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));
					map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_NO_CHANGE);

					// Calculate votes progress
					if (i == 0) {
						maxVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
						if (maxVotes == 0) {
							maxVotes = 1;
						}
					}

					currentVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
					votesProgress = (currentVotes * 100) / maxVotes;
					map.put(Constants.KEY_VOTES_PROGRESS, Integer.toString(votesProgress));
					map.put(Constants.KEY_SHOW_VOTES_PROGRESS, "TRUE");

					synchronized(notowaniaList) { notowaniaList.add(map); }
					Log.i(TAG, "notowaniaList refresh element: " + notowaniaList );
				}

			} catch (IOException e) {
				connectionError = true;
			}

			// Update UI on main thread - only update NotowaniaFragment
			final boolean finalConnectionError = connectionError;
			mainHandler.post(() -> {
				// Update only NotowaniaFragment adapter
				NotowaniaFragment notowaniaFragment = getFragmentByPosition(TabPagerAdapter.TAB_NOTOWANIA);
				if (notowaniaFragment != null) {
					notowaniaFragment.updateAdapter();
				}

				progressDialog.dismiss();

				if (finalConnectionError) {
					new AlertDialog.Builder(ListaPrzebojowDiscoPolo.this)
						.setTitle(R.string.text_connection_error_title)
						.setMessage(getString(R.string.text_connection_error))
						.setNeutralButton("Ok", null).show();
				}
			});
		});
	}

	// Filter wykonawcy method (moved from old implementation)
	public void filterWykonawcy(ArrayList<HashMap<String, String>> wykonList, CharSequence searchText) {
		synchronized(filteredWykonList) { filteredWykonList.clear(); }
		if (searchText.length() == 0) {
			synchronized(filteredWykonList) { filteredWykonList.addAll(wykonList); }
		} else {
			String searchString = searchText.toString().toLowerCase();
			for (HashMap<String, String> item : wykonList) {
				String wykonawca = item.get(Constants.KEY_WYKONAWCA);
				if (wykonawca != null && wykonawca.toLowerCase().contains(searchString)) {
					synchronized(filteredWykonList) { filteredWykonList.add(item); }
				}
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection using if-else instead of switch for resource IDs
		int itemId = item.getItemId();
		if (itemId == R.id.odswiez) {
			refreshListBackground();
			return true;
		} else if (itemId == R.id.policy) {
			Intent browserIntent2 = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.POLICY_URL));
			startActivity(browserIntent2);
			return true;
		} else if (itemId == R.id.kontakt) {
			Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Constants.EMAIL_TO + "?subject=" +
					Uri.encode(getString(R.string.email_subject)) + "&body="));
			startActivity(email);
			return true;
		} else if (itemId == R.id.jezyk) {
			showLanguageMenu();
			return true;
		} else if (itemId == R.id.oProgramie) {
			Intent intentAbout = new Intent(Intent.ACTION_VIEW);
			intentAbout.setClassName(this, OProgramie.class.getName());
			startActivity(intentAbout);
			return true;
		} else if (itemId == R.id.facebook) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FACEBOOK_URL));
			startActivity(browserIntent);
			return true;
		} else if (itemId == R.id.ustawienia) {
			Intent settingsIntent = new Intent(getBaseContext(),Preferences.class);
			startActivity(settingsIntent);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}


	// Setup TabLayout with ViewPager2 (replaced createTabs)
	public void setupTabLayoutWithViewPager(){
		TabLayout tabLayout = findViewById(R.id.tabLayout);
		// New ViewPager2 and TabLayout
		ViewPager2 viewPager = findViewById(R.id.viewPager);

		// Initialize TabPagerAdapter
		tabPagerAdapter = new TabPagerAdapter(this);
		viewPager.setAdapter(tabPagerAdapter);
		
		// Set offscreen page limit to ensure all fragments are created immediately
		// This prevents the "fragments are null" issue when updateAllFragmentAdapters is called
		viewPager.setOffscreenPageLimit(TabPagerAdapter.NUM_TABS - 1);

		// Create TabLayoutMediator with tab configuration
		tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			switch (position) {
				case TabPagerAdapter.TAB_LISTA:
					tab.setText(getString(R.string.tab_lista));
					break;
				case TabPagerAdapter.TAB_POCZEKALNIA:
					tab.setText(getString(R.string.tab_poczekalnia));
					break;
				case TabPagerAdapter.TAB_NOWOSCI:
					tab.setText(getString(R.string.tab_nowosci));
					break;
				case TabPagerAdapter.TAB_MOJALISTA:
					tab.setText(getString(R.string.tab_mojalista));
					break;
				case TabPagerAdapter.TAB_WYKONAWCY:
					tab.setText(getString(R.string.tab_wykonawcy));
					break;
				case TabPagerAdapter.TAB_NOTOWANIA:
					tab.setText(getString(R.string.tab_notowanie));
					break;
			}
		});
		tabLayoutMediator.attach();
		
		// Ensure fragments are created by triggering ViewPager2 to create them
		// This is done after a short delay to allow the ViewPager2 to be fully initialized
		if (mainHandler != null) {
			mainHandler.postDelayed(this::ensureFragmentsCreated, 100);
		}
	}
	
	// Method to ensure all fragments are created in ViewPager2
	private void ensureFragmentsCreated() {
		ViewPager2 viewPager = findViewById(R.id.viewPager);
		if (viewPager != null && tabPagerAdapter != null) {
			Log.i("FragmentCreation", "Ensuring all fragments are created");
			
			// Since we set offscreen page limit to NUM_TABS - 1, all fragments should be created automatically
			// Wait for ViewPager2 to initialize and create fragments
			mainHandler.postDelayed(() -> {
				int createdFragments = 0;
				for (int i = 0; i < TabPagerAdapter.NUM_TABS; i++) {
					Fragment fragment = getFragmentByPosition(i);
					if (fragment != null) {
						createdFragments++;
						Log.d("FragmentCreation", "Fragment at position " + i + " created successfully with tag: f" + i);
					} else {
						Log.w("FragmentCreation", "Fragment at position " + i + " still null after creation attempt");
					}
				}
				Log.i("FragmentCreation", "Fragment creation verification completed. Created: " + createdFragments + "/" + TabPagerAdapter.NUM_TABS);
				
				// If fragments are still not created, try alternative approach
				if (createdFragments == 0) {
					Log.w("FragmentCreation", "No fragments created, trying alternative approach");
					forceCreateAllFragments();
				}
			}, 500); // Give more time for ViewPager2 to create fragments
			
			Log.i("FragmentCreation", "Fragment creation process initiated");
		}
	}
	
	// Debug method to log ViewPager2 state and fragment information
	private void debugViewPagerState() {
		ViewPager2 viewPager = findViewById(R.id.viewPager);
		if (viewPager != null) {
			Log.d("ViewPagerDebug", "ViewPager2 ID: " + viewPager.getId() + " (R.id.viewPager: " + R.id.viewPager + ")");
			Log.d("ViewPagerDebug", "Current item: " + viewPager.getCurrentItem());
			Log.d("ViewPagerDebug", "Offscreen page limit: " + viewPager.getOffscreenPageLimit());
			Log.d("ViewPagerDebug", "Adapter: " + (viewPager.getAdapter() != null ? viewPager.getAdapter().getClass().getSimpleName() : "null"));
			
			// Log all fragments in FragmentManager
			Log.d("ViewPagerDebug", "All fragments in FragmentManager:");
			for (Fragment fragment : getSupportFragmentManager().getFragments()) {
				if (fragment != null) {
					Log.d("ViewPagerDebug", "Fragment: " + fragment.getClass().getSimpleName() + 
						", Tag: " + fragment.getTag() + 
						", Added: " + fragment.isAdded() + 
						", Visible: " + fragment.isVisible());
				}
			}
		} else {
			Log.w("ViewPagerDebug", "ViewPager2 is null!");
		}
	}
	
	// Alternative method to force fragment creation
	private void forceCreateAllFragments() {
		Log.i("FragmentCreation", "Force creating all fragments using alternative method");
		ViewPager2 viewPager = findViewById(R.id.viewPager);
		if (viewPager != null && tabPagerAdapter != null) {
			// Set a very high offscreen page limit to force all fragments to be created
			int originalLimit = viewPager.getOffscreenPageLimit();
			viewPager.setOffscreenPageLimit(6);
			// Trigger adapter refresh
			viewPager.getAdapter().notifyDataSetChanged();
			
			// Give time for fragments to be created
			mainHandler.postDelayed(() -> {
				// Restore original offscreen limit
				viewPager.setOffscreenPageLimit(originalLimit);
				
				// Verify again
				int createdFragments = 0;
				for (int i = 0; i < TabPagerAdapter.NUM_TABS; i++) {
					Fragment fragment = getFragmentByPosition(i);
					if (fragment != null) {
						createdFragments++;
					}
				}
				Log.i("FragmentCreation", "Alternative creation method result: " + createdFragments + "/" + TabPagerAdapter.NUM_TABS + " fragments created");
			}, 300);
		}
	}
	
	// Callback method for fragments to notify when they are ready
	public void onFragmentReady(String fragmentName) {
		Log.i("FragmentReadiness", "Fragment " + fragmentName + " is ready");
		readyFragments.add(fragmentName);
		
		// Check if we have pending data updates and all fragments are ready
		if (pendingFragmentUpdate && dataLoadCompleted) {
			checkAndUpdateFragments();
		}
	}
	
	// Check if we can update fragments and do so if ready
	private void checkAndUpdateFragments() {
		if (readyFragments.size() >= 6) { // All 6 fragments are ready
			Log.i("FragmentReadiness", "All fragments are ready, updating adapters");
			pendingFragmentUpdate = false;
			updateAllFragmentAdapters();
		} else {
			Log.i("FragmentReadiness", "Waiting for more fragments. Ready: " + readyFragments.size() + "/6");
		}
	}

	//reklama
	public void createAd(){
		mAdView = findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder().build();
		mAdView.loadAd(adRequest);
	}


	public void showAuthSongs(String authId){
		Intent intent = new Intent();
		Bundle bun = new Bundle();

		bun.putString("param_auth_id", authId);
		bun.putBoolean("param_ad_reward", adReward);

		intent.setClass(this, UtworyWykonawcy.class);
		intent.putExtras(bun);
		activityResultLauncher.launch(intent);
	}




	public void showSongMenu(int position, String listType){
		HashMap<String, String> o = new HashMap<>();

		try {
			if (Objects.equals(listType, Constants.KEY_LISTA)) {
				if (songsList != null && position >= 0 && position < songsList.size()) {
					o = songsList.get(position);
				} else {
					Log.e(TAG, "Invalid position " + position + " for songsList (size: " + (songsList != null ? songsList.size() : 0) + ")");
					return;
				}
			}
			if (Objects.equals(listType, Constants.KEY_POCZEKALNIA)) {
				if (songsListPocz != null && position >= 0 && position < songsListPocz.size()) {
					o = songsListPocz.get(position);
				} else {
					Log.e(TAG, "Invalid position " + position + " for songsListPocz (size: " + (songsListPocz != null ? songsListPocz.size() : 0) + ")");
					return;
				}
			}
			if (Objects.equals(listType, Constants.KEY_NOWOSCI)) {
				if (songsListNowosci != null && position >= 0 && position < songsListNowosci.size()) {
					o = songsListNowosci.get(position);
				} else {
					Log.e(TAG, "Invalid position " + position + " for songsListNowosci (size: " + (songsListNowosci != null ? songsListNowosci.size() : 0) + ")");
					return;
				}
			}
			if (Objects.equals(listType, Constants.KEY_MOJALISTA)) {
				if (songsListMojalista != null && position >= 0 && position < songsListMojalista.size()) {
					o = songsListMojalista.get(position);
				} else {
					Log.e(TAG, "Invalid position " + position + " for songsListMojalista (size: " + (songsListMojalista != null ? songsListMojalista.size() : 0) + ")");
					return;
				}
			}
			if (Objects.equals(listType, Constants.KEY_UTW_WYKONAWCY)) {
				if (wykUtwory != null && position >= 0 && position < wykUtwory.getCount()) {
					o = (HashMap<String, String>) wykUtwory.getItemAtPosition(position);
				} else {
					Log.e(TAG, "Invalid position " + position + " for wykUtwory (count: " + (wykUtwory != null ? wykUtwory.getCount() : 0) + ")");
					return;
				}
			}
			if (Objects.equals(listType, Constants.KEY_LISTA_NOTOWANIA)) {
				if (notowaniaList != null && position >= 0 && position < notowaniaList.size()) {
					o = notowaniaList.get(position);
				} else {
					Log.e(TAG, "Invalid position " + position + " for notowaniaList (size: " + (notowaniaList != null ? notowaniaList.size() : 0) + ")");
					return;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error accessing list at position " + position + " for listType " + listType + ": " + e.getMessage());
			return;
		}

		// Additional safety check - ensure we have valid data
		if (o == null || o.isEmpty()) {
			Log.e(TAG, "No valid data found at position " + position + " for listType " + listType);
			return;
		}


		final String idListy = o.get(Constants.KEY_ID);
		final String idWykonawcy = o.get(Constants.KEY_ARTIST_ID);
		final String idGrupy = o.get(Constants.KEY_ID_GRUPY);
		String title = o.get(Constants.KEY_TITLE);
		teledysk = o.get(Constants.KEY_VIDEO);
		spotify = o.get(Constants.KEY_SPOTIFY);
		String artist = o.get(Constants.KEY_ARTIST);

		final CharSequence[] RewardItems = {getString(R.string.zaglosuj),getString(R.string.liczba_glosow), getString(R.string.teledysk), getString(R.string.spotify), getString(R.string.utwory_wykonawcy)};
		final CharSequence[] items = {getString(R.string.zaglosuj), getString(R.string.teledysk), getString(R.string.spotify), getString(R.string.utwory_wykonawcy)};
		final CharSequence[] wykItems = {getString(R.string.zaglosuj), getString(R.string.teledysk), getString(R.string.spotify)};

		AlertDialog.Builder builder = new AlertDialog.Builder(ListaPrzebojowDiscoPolo.this);
        builder.setTitle(title);
       // builder.setIcon(R.drawable.ic_menu_more);

		if (listType.equals(Constants.KEY_LISTA) || listType.equals(Constants.KEY_POCZEKALNIA) || listType.equals(Constants.KEY_NOWOSCI) || listType.equals(Constants.KEY_MOJALISTA) ) {
			if (!adReward)  {
				builder.setItems(RewardItems, (dialog, item) -> {
					//Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
					if (RewardItems[item] == getString(R.string.zaglosuj)) {
						glosTeledysk = "0";
						zaglosuj(idListy, Constants.KEY_LISTA, null, idGrupy, glosTeledysk);
					} else if (RewardItems[item] == getString(R.string.liczba_glosow)) {
						if (!Constants.VERSION_PRO_DO_NOT_SHOW_BANNER) {
							showRewardedVideo();
						}
					} else if (RewardItems[item] == getString(R.string.utwory_wykonawcy)) {
						finish();
						showAuthSongs(idWykonawcy);
					} else if (RewardItems[item] == getString(R.string.teledysk)) {
						glosTeledysk = "1";
//						zaglosuj(idListy, Constants.KEY_LISTA, null, idGrupy, glosTeledysk);
						// Use YouTube Bottom Sheet instead of browser
						if (youTubeBottomSheetController != null) {
							youTubeBottomSheetController.showYouTubeVideo(teledysk, title, artist);
						} else {
							Log.e("YouTubeDebug", "YouTubeBottomSheetController is null");
						}
					} else if (RewardItems[item] == getString(R.string.spotify)) {
                        // Use Spotify Bottom Sheet instead of Activity
                        playSpotifyTrack(spotify, title, artist);
                    }
				});
			} else {
				builder.setItems(items, (dialog, item) -> {
					//Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_SHORT).show();
					if (items[item] == getString(R.string.zaglosuj)) {
						glosTeledysk = "0";
						zaglosuj(idListy, Constants.KEY_LISTA, null, idGrupy, glosTeledysk);
					} else if (items[item] == getString(R.string.utwory_wykonawcy)) {
						finish();
						showAuthSongs(idWykonawcy);
					} else if (items[item] == getString(R.string.teledysk)) {
						glosTeledysk = "1";
//						zaglosuj(idListy, Constants.KEY_LISTA, null, idGrupy, glosTeledysk);
						// Use YouTube Bottom Sheet instead of browser
					if (youTubeBottomSheetController != null) {
						youTubeBottomSheetController.showYouTubeVideo(teledysk, title, artist);
					} else {
						Log.e("YouTubeDebug", "YouTubeBottomSheetController is null");
						}
					}else if (items[item] == getString(R.string.spotify)) {
                        // Use Spotify Bottom Sheet instead of Activity
                        playSpotifyTrack(spotify, title, artist);
                    }
				});
			}
		}

		if (Objects.equals(listType, Constants.KEY_LISTA_NOTOWANIA)) {
			//	finish();
			showAuthSongs(idWykonawcy);
		}

		if (Objects.equals(listType, Constants.KEY_UTW_WYKONAWCY)) {
			builder.setItems(wykItems, (dialog, item) -> {
				//Toast.makeText(getApplicationContext(), wykItems[item], Toast.LENGTH_SHORT).show();
				if(wykItems[item]==getString(R.string.zaglosuj)){
					glosTeledysk = "0";
					zaglosuj(idListy, Constants.KEY_UTW_WYKONAWCY, idWykonawcy, idGrupy,glosTeledysk);
				}
				else if(wykItems[item]==getString(R.string.teledysk)){
					glosTeledysk = "1";
//					zaglosuj(idListy, Constants.KEY_UTW_WYKONAWCY, idWykonawcy, idGrupy,glosTeledysk);
				// Use YouTube Bottom Sheet instead of browser
				if (youTubeBottomSheetController != null) {
					youTubeBottomSheetController.showYouTubeVideo(teledysk, title, artist);
				} else {
					Log.e("YouTubeDebug", "YouTubeBottomSheetController is null");
				}
				}
				                else if(wykItems[item]==getString(R.string.spotify)){
                    // Use Spotify Bottom Sheet instead of Activity
                    playSpotifyTrack(spotify, title, artist);
                }
			});
		}

		if (!Objects.equals(listType, Constants.KEY_LISTA_NOTOWANIA)) {
			AlertDialog alert = builder.create();
			alert.show();
		}

	}
	private void refreshListInBackground() {
		executorService.execute(() -> {
			boolean connectionError = false;

			try {
				XMLParser parser = new XMLParser();

				String xml = parser.getXmlFromUrl(Constants.URL.replace("LANG", language)); // getting XML from URL
			// Log.e(TAG, "Root xml element: " + xml );
				Document doc = parser.getDomElement(xml); // getting DOM element
			//Log.e(TAG, "doc element: " + doc );
				NodeList nlInfo = doc.getElementsByTagName(Constants.KEY_INFO);
				Element el = (Element) nlInfo.item(0);
				info = parser.getValue(el, Constants.KEY_INFO);
				
							int votesProgress;
			int maxVotes = 0;
			int currentVotes;
			
			// Clear existing data to prevent duplicates - synchronized to prevent UI thread conflicts
			synchronized(songsList) { songsList.clear(); }
			synchronized(songsListPocz) { songsListPocz.clear(); }
			synchronized(songsListNowosci) { songsListNowosci.clear(); }
			synchronized(songsListMojalista) { songsListMojalista.clear(); }
			synchronized(wykonList) { wykonList.clear(); }
			synchronized(notowaniaList) { notowaniaList.clear(); }
			synchronized(notowPrzedzialyList) { notowPrzedzialyList.clear(); }
			synchronized(listNotowPrzedzialy) { listNotowPrzedzialy.clear(); }
			
			//lista
			NodeList nl = doc.getElementsByTagName(Constants.KEY_SONG);
				
				// looping through all song nodes <song>
				for (int i = 0; i < nl.getLength(); i++) {
					// creating new HashMap
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);
					// adding each child node to HashMap key => value
					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
					//only for PRO Version
					if (adReward) {
						map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					}
					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_POSITION, parser.getValue(e, Constants.KEY_POSITION));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));
					
					// Format the place change text with arrow and number
					String originalChangeText = parser.getValue(e, Constants.KEY_PLACE_CHANGE);
					String formattedChangeText = formatPlaceChangeText(originalChangeText);
					map.put(Constants.KEY_PLACE_CHANGE, formattedChangeText + Constants.TEXT_SEPARATOR);
					
					if (originalChangeText.contains(getString(R.string.text_awans))) {
						map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_UP);
					} else if (originalChangeText.contains(getString(R.string.text_spadek))) {
						map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_DOWN);
					} else {
						map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_NO_CHANGE);
					}

					if(i==0){
						maxVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
						if (maxVotes==0){
							maxVotes = 1;
						}
					}
					currentVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
					votesProgress = (currentVotes * 100) / maxVotes ;
					map.put(Constants.KEY_VOTES_PROGRESS, Integer.toString(votesProgress));

					//showing or not showing progress bar
					map.put(Constants.KEY_SHOW_VOTES_PROGRESS,"TRUE");

					// adding HashList to ArrayList - synchronized to prevent UI thread conflicts
					synchronized(songsList) { songsList.add(map); }
					Log.i(TAG, "songsList element: " + songsList );
				}

				//poczekalnia

				nl = doc.getElementsByTagName(Constants.KEY_SONG_POCZ);
					Log.i(TAG, "nl pocz element: " + nl );
				// looping through all song nodes <song>
				for (int i = 0; i < nl.getLength(); i++) {
					// creating new HashMap
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);
					// adding each child node to HashMap key => value
					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
					if (adReward) {
						map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					}
					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_POSITION, parser.getValue(e, Constants.KEY_POSITION));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));
					//setting votes progress
					currentVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
					votesProgress = (currentVotes * 100) / maxVotes ;
					map.put(Constants.KEY_VOTES_PROGRESS, Integer.toString(votesProgress));
					//showing or not showing progress bar
					map.put(Constants.KEY_SHOW_VOTES_PROGRESS,"TRUE");

					// adding HashList to ArrayList - synchronized to prevent UI thread conflicts
					synchronized(songsListPocz) { songsListPocz.add(map); }
					Log.i(TAG, "songsListPocz element: " + songsListPocz );
				}

				//nowości
				String xml_nowosci = parser.getXmlFromUrl2(Constants.URL_NOWOSCI.replace("LANG", language)); // getting XML from URL

				Document doc2 = parser.getDomElement2(xml_nowosci); // getting DOM element
				//	Log.i(TAG, "Root2 element: " + xml_nowosci );
				//	Log.i(TAG, "doc2 element: " + doc2 );
				NodeList nl2 = doc2.getElementsByTagName(Constants.KEY_SONG_NOWOSCI);
				//	Log.i(TAG, "nl2 element: " + nl2 );

				// looping through all song nodes <song>
				for (int i = 0; i < nl2.getLength(); i++) {
					// creating new HashMap
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl2.item(i);
					// adding each child node to HashMap key => value
					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					if (adReward) {
						map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					}
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));
					// adding HashList to ArrayList - synchronized to prevent UI thread conflicts
					synchronized(songsListNowosci) { songsListNowosci.add(map); }
					Log.i(TAG, "songsListNowosci element: " + songsListNowosci );
				}







				//	Log.i(TAG, "Root androidid element: " + androidId );
				//mojalista
				String xml_mojalista = parser.getXmlFromUrlMoja(buildSafeUrl(Constants.URL_MOJALISTA)); // getting XML from URL

				Document docmoja = parser.getDomElementMoja(xml_mojalista); // getting DOM element
				//	Log.i(TAG, "Root moja element: " + xml_mojalista );
				//	Log.i(TAG, "docmoja element: " + docmoja );
				 nl = docmoja.getElementsByTagName(Constants.KEY_SONG_MOJA);
				//	Log.i(TAG, "nl element: " + nl );

				// looping through all song nodes <song>
				for (int i = 0; i < nl.getLength(); i++) {
					// creating new HashMap
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);
					// adding each child node to HashMap key => value
					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
					map.put(Constants.KEY_POSITION, parser.getValue(e, Constants.KEY_POSITION));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					if (adReward) {
						map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					}
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));
					//setting votes progress
					currentVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
					votesProgress = (currentVotes * 100) / maxVotes ;
					map.put(Constants.KEY_VOTES_PROGRESS, Integer.toString(votesProgress));
					//showing or not showing progress bar
					map.put(Constants.KEY_SHOW_VOTES_PROGRESS,"TRUE");

					// adding HashList to ArrayList - synchronized to prevent UI thread conflicts
					synchronized(songsListMojalista) { songsListMojalista.add(map); }
						Log.i(TAG, "songsListMojalista element: " + songsListMojalista );
				}


				xml = parser.getXmlFromUrl(Constants.WYK_URL); // getting XML from URL

				Document doc1 = parser.getDomElement(xml); // getting DOM element


				//wykonawcy

				nl = doc1.getElementsByTagName(Constants.KEY_WYKON);

				// looping through all song nodes <song>
				for (int i = 0; i < nl.getLength(); i++) {
					// creating new HashMap
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);
					// adding each child node to HashMap key => value
					map.put(Constants.KEY_ID_WYKON, parser.getValue(e, Constants.KEY_ID_WYKON));
					map.put(Constants.KEY_WYKONAWCA, parser.getValue(e, Constants.KEY_WYKONAWCA));
					// adding HashList to ArrayList - synchronized to prevent UI thread conflicts
					synchronized(wykonList) { wykonList.add(map); }
					Log.i(TAG, "wykonList element: " + wykonList );

					//filteredWykonList.add(map);
				}
				//filteredWykonList=wykonList;
				filterWykonawcy(wykonList, "");

				// First, get the notowania XML to populate notowPrzedzialyList and set the correct notowanieId
				xml = parser.getXmlFromUrl(Constants.URL_NOTOWANIA.replace("LANG", language).replace("START_NOTOWANIE_ID", notowanieId)); // getting XML from URL

				doc = parser.getDomElement(xml); // getting DOM element

				nlInfo = doc.getElementsByTagName(Constants.KEY_INFO);
				el = (Element) nlInfo.item(0);
				info2012 = parser.getValue(el, Constants.KEY_INFO);

//				votesProgress =0 ;
				maxVotes = 0;
//				currentVotes = 0 ;

				//notowania_przedzialy
				nl = doc.getElementsByTagName(Constants.KEY_NOTOWANIE_PRZEDZIAL);
				// looping through all records
				for (int i = 0; i < nl.getLength(); i++) {
					// creating new HashMap
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);
					// adding each child node to HashMap key => value
					map.put(Constants.KEY_NOTOWANIE_ZA, parser.getValue(e, Constants.KEY_NOTOWANIE_ZA));
					map.put(Constants.KEY_NOTOWANIE_NAZWA, parser.getValue(e, Constants.KEY_NOTOWANIE_NAZWA));
					// adding HashList to ArrayList
					notowPrzedzialyList.add(map);
					Log.i(TAG, "notowPrzedzialyList element: " + notowPrzedzialyList );
					listNotowPrzedzialy.add(map.get(Constants.KEY_NOTOWANIE_NAZWA));
				}
				
				// Set the initial notowanieId to the first valid value if it's still the placeholder
				if (notowanieId.equals(Constants.VALUE_START_NOTOWANIE_ID) && !notowPrzedzialyList.isEmpty()) {
					String firstNotowanieId = notowPrzedzialyList.get(0).get(Constants.KEY_NOTOWANIE_ZA);
					if (firstNotowanieId != null && !firstNotowanieId.isEmpty()) {
						notowanieId = firstNotowanieId;
						Log.i(TAG, "Set initial notowanieId to: " + notowanieId);
						
						// Now fetch the notowania data with the correct notowanieId
						xml = parser.getXmlFromUrl(Constants.URL_NOTOWANIA.replace("LANG", language).replace("START_NOTOWANIE_ID", notowanieId));
						doc = parser.getDomElement(xml);
						Log.i(TAG, "Refetched notowania XML with correct notowanieId: " + notowanieId);
					}
				}



				//notowania
				nl = doc.getElementsByTagName(Constants.KEY_SONG);

				// looping through all song nodes <song>
				for (int i = 0; i < nl.getLength(); i++) {
					// creating new HashMap
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);
					// adding each child node to HashMap key => value
					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
					//only for PRO Version
					if (adReward) {
						map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					}
					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_POSITION, parser.getValue(e, Constants.KEY_POSITION));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));

					if(i==0){
						maxVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
						if (maxVotes==0){
							maxVotes = 1;
						}
					}
					currentVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
					votesProgress = (currentVotes * 100) / maxVotes ;
					map.put(Constants.KEY_VOTES_PROGRESS, Integer.toString(votesProgress));

					//showing or not showing progress bar
					map.put(Constants.KEY_SHOW_VOTES_PROGRESS,"TRUE");

					// adding HashList to ArrayList - synchronized to prevent UI thread conflicts
					synchronized(notowaniaList) { notowaniaList.add(map); }
					Log.i(TAG, "notowaniaList element: " + notowaniaList );
				}


			} catch (IOException e){
				connectionError = true;
				Log.e("DataLoading", "IOException loading data: " + e.getMessage(), e);
				
				// Ensure data structures are initialized even on error
				if (songsList == null) songsList = new ArrayList<>();
				if (songsListPocz == null) songsListPocz = new ArrayList<>();
				if (songsListNowosci == null) songsListNowosci = new ArrayList<>();
				if (songsListMojalista == null) songsListMojalista = new ArrayList<>();
			} catch (Exception e){
				connectionError = true;
				Log.e("DataLoading", "General exception loading data: " + e.getMessage(), e);
				
				// Ensure data structures are initialized even on error
				if (songsList == null) songsList = new ArrayList<>();
				if (songsListPocz == null) songsListPocz = new ArrayList<>();
				if (songsListNowosci == null) songsListNowosci = new ArrayList<>();
				if (songsListMojalista == null) songsListMojalista = new ArrayList<>();
			}

			// Update UI on main thread
			final boolean finalConnectionError = connectionError;
			mainHandler.post(() -> {
				try {
					// Safely dismiss progress dialog
					if (progressDialog != null && progressDialog.isShowing()) {
						progressDialog.dismiss();
						progressDialog = null;
					}
					
					// Log data loading results for debugging
					Log.i("DataLoading", "Data loaded - songsList: " + (songsList != null ? songsList.size() : 0) + 
						", songsListPocz: " + (songsListPocz != null ? songsListPocz.size() : 0) +
						", songsListNowosci: " + (songsListNowosci != null ? songsListNowosci.size() : 0) +
						", songsListMojalista: " + (songsListMojalista != null ? songsListMojalista.size() : 0));
					
					// Validate data before updating UI
					boolean hasData = (songsList != null && !songsList.isEmpty()) ||
									  (songsListPocz != null && !songsListPocz.isEmpty()) ||
									  (songsListNowosci != null && !songsListNowosci.isEmpty()) ||
									  (songsListMojalista != null && !songsListMojalista.isEmpty()) ||
									  (notowaniaList != null && !notowaniaList.isEmpty()) ||
									  (filteredWykonList != null && !filteredWykonList.isEmpty());
					
					Log.i("DataLoading", "Has data to display: " + hasData);
					
					// Mark data loading as completed
					dataLoadCompleted = true;
					pendingFragmentUpdate = true;
					
					// Try to update fragments if they are ready, otherwise wait for fragment callbacks
					checkAndUpdateFragments();
					
					// Schedule a force refresh as fallback in case normal update didn't work
					if (mainHandler != null) {
						mainHandler.postDelayed(() -> {
							Log.i("DataLoading", "Performing fallback force refresh of adapters");
							forceRefreshAllFragmentAdapters();
						}, 2000); // Increased delay to allow for fragment readiness
					}
					
					isDataLoading = false; // Reset loading flag
					
					if (finalConnectionError) {
						// Ensure activity is still valid before showing dialog
						if (!isFinishing() && !isDestroyed()) {
							new AlertDialog.Builder(ListaPrzebojowDiscoPolo.this)
									.setTitle(R.string.text_connection_error_title)
									.setMessage(getString(R.string.text_connection_error))
									.setNeutralButton("Ok", null).show();
						}
					}
				} catch (Exception e) {
					Log.e("DialogDismiss", "Error dismissing dialog: " + e.getMessage());
					isDataLoading = false; // Reset loading flag even on error
				}
			});
		});
	}

	private void voteInBackground(String url, String listType) {
		executorService.execute(() -> {
			boolean connectionError = false;
			String voteMessage;

			Vote vote = new Vote();
			try{
				voteMessage = vote.setVoteInUrl(url);
			} catch (IOException er){
				connectionError = true;
				voteMessage = getString(R.string.text_voting_error);
			}

			// Update UI on main thread
			final boolean finalConnectionError = connectionError;
			final String finalVoteMessage = voteMessage;
			mainHandler.post(() -> {
				// Safely dismiss vote progress dialog
				try {
					if (progressDialogVote != null && progressDialogVote.isShowing()) {
						progressDialogVote.dismiss();
						progressDialogVote = null;
					}
				} catch (Exception e) {
					Log.e("DialogDismiss", "Error dismissing vote dialog: " + e.getMessage());
				}

				if (finalConnectionError) {
					new AlertDialog.Builder(ListaPrzebojowDiscoPolo.this)
					.setTitle(R.string.text_connection_error_title)
					.setMessage(getString(R.string.text_connection_error))
					.setNeutralButton("Ok",	null).show();
				} else {
					setUserVote(votingListId);
					if(Objects.equals(glosTeledysk, "0")){
						Toast.makeText(getApplicationContext(), finalVoteMessage, Toast.LENGTH_LONG).show();
						//   Snackbar snackbar = make(ListaPrzebojowDiscoPolo.this.getCurrentFocus(), voteMessage, Snackbar.LENGTH_LONG);
						//   snackbar.show();
					}
					if (Objects.equals(myListType, Constants.KEY_LISTA) || Objects.equals(myListType, Constants.KEY_POCZEKALNIA) || Objects.equals(myListType, Constants.KEY_NOWOSCI) || Objects.equals(myListType, Constants.KEY_MOJALISTA) || Objects.equals(myListType, Constants.KEY_WYKONAWCY)) {
						// Execute refreshListBackground on main thread to prevent threading issues
						if (Looper.myLooper() == Looper.getMainLooper()) {
							refreshListBackground();
						} else {
							mainHandler.post(this::refreshListBackground);
						}
					}
					if (Objects.equals(myListType, Constants.KEY_UTW_WYKONAWCY)) {
						showAuthSongs(myIdWykonawcy);
					}
				}
			});
		});
	}

	public boolean canUserVotes(String idListy) {
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		SimpleDateFormat lastVoteDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String lastVoteDateString = settings.getString(idListy, lastVoteDate.format(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)));

		try {
			return Objects.requireNonNull(lastVoteDate.parse(lastVoteDateString)).getTime() / (1000) <= (System.currentTimeMillis() - (Constants.VOTES_INTERVAL * 60 * 1000)) / (1000);
		} catch (ParseException e) {
			return true;
		}
	}

	public void setUserVote(String idListy){
		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		SimpleDateFormat newVoteDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(idListy, newVoteDate.format(System.currentTimeMillis()));
		editor.apply();
	}

	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);

		if (progressDialog != null && progressDialog.isShowing()){
			progressDialog.dismiss();
		}
	}

	private void showLanguageMenu() {

		String localeString = getLocaleSettings();
		AlertDialog.Builder builder = new AlertDialog.Builder(ListaPrzebojowDiscoPolo.this);
        builder.setTitle(R.string.menu_jezyk);
       // builder.setIcon(R.drawable.ic_menu_more);
        
        final CharSequence[] items = {getString(R.string.jezyk_angielski),getString(R.string.jezyk_polski)};
        int checkedItem = 0;
        if (localeString.equals("en")){
        	checkedItem = 0;
        }
        else if (localeString.equals("pl")){
        	checkedItem = 1;
        }
			builder.setSingleChoiceItems(items, checkedItem, (dialog, item) -> {
				if (item == 0){
					setLocaleSettings("en");
				}
				if (item == 1){
					setLocaleSettings("pl");
				}
			});
      
        builder.setNeutralButton("OK", (dialog, item) -> Toast.makeText(getApplicationContext(), R.string.text_zmiana_jezyka, Toast.LENGTH_LONG).show());
        AlertDialog alert = builder.create();
        alert.show();		
		
	}

	public String getLocaleSettings() {
		//SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		Locale lc = Locale.getDefault();
		String localeDefault = lc != null ? lc.getLanguage() : "en";
		//String localeString = settings.getString("locale",localeDefault);
		return localeDefault != null ? localeDefault : "en";
	}

	/**
	 * Helper method to safely build URLs with null checks
	 * @param baseUrl The base URL template
	 * @return URL with safe replacements for LANG and ANDROIDID placeholders
	 */
	private String buildSafeUrl(String baseUrl) {
		if (baseUrl == null) {
			Log.w(TAG, "buildSafeUrl called with null baseUrl");
			return "";
		}
		String safeLanguage = (language != null) ? language : "pl";
		String safeAndroidId = (androidId != null) ? androidId : "unknown_device";
		
		if (language == null) {
			Log.w(TAG, "Language is null, using fallback: pl");
		}
		if (androidId == null) {
			Log.w(TAG, "AndroidId is null, using fallback: unknown_device");
		}
		
		return baseUrl.replace("LANG", safeLanguage).replace("ANDROIDID", safeAndroidId);
	}

	/**
	 * Helper method to safely build URLs with language replacement only
	 * @param baseUrl The base URL template
	 * @return URL with safe replacement for LANG placeholder
	 */
	private String buildSafeUrlWithLanguage(String baseUrl) {
		if (baseUrl == null) {
			return "";
		}
		String safeLanguage = (language != null) ? language : "pl";
		return baseUrl.replace("LANG", safeLanguage);
	}

	/**
	 * Check if Google Play Services is available and up to date
	 * @return true if Google Play Services is available, false otherwise
	 */
	private boolean isGooglePlayServicesAvailable() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			Log.e(TAG, "Google Play Services not available. Result code: " + resultCode);
			String errorMessage = getGooglePlayServicesErrorMessage(resultCode);
			Log.e(TAG, "Error details: " + errorMessage);
			if (apiAvailability.isUserResolvableError(resultCode)) {
				Log.i(TAG, "Google Play Services error is user resolvable");
				// You could show a dialog to the user here if needed
			}
			return false;
		}
		Log.i(TAG, "Google Play Services is available");
		return true;
	}

	/**
	 * Get human-readable error message for Google Play Services error codes
	 */
	private String getGooglePlayServicesErrorMessage(int errorCode) {
		switch (errorCode) {
			case ConnectionResult.SERVICE_MISSING:
				return "Google Play Services is missing";
			case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
				return "Google Play Services needs to be updated";
			case ConnectionResult.SERVICE_DISABLED:
				return "Google Play Services is disabled";
			case ConnectionResult.SERVICE_INVALID:
				return "Google Play Services version is invalid";
			case ConnectionResult.SIGN_IN_REQUIRED:
				return "Sign in to Google Play Services is required";
			case ConnectionResult.NETWORK_ERROR:
				return "Network error connecting to Google Play Services";
			case ConnectionResult.INTERNAL_ERROR:
				return "Internal error in Google Play Services";
			case ConnectionResult.API_UNAVAILABLE:
				return "API is not available on this device";
			default:
				return "Unknown error code: " + errorCode;
		}
	}

	/**
	 * Log package information for debugging Google Play Services issues
	 */
	private void logPackageInfo() {
		try {
			String packageName = getPackageName();
			Log.i(TAG, "App package name: " + packageName);
			
			// Log package manager information
			PackageManager pm = getPackageManager();
			android.content.pm.PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
			Log.i(TAG, "App version code: " + packageInfo.versionCode);
			Log.i(TAG, "App version name: " + packageInfo.versionName);
			
			// Log signing information (for debugging certificate issues)
			if (packageInfo.signatures != null && packageInfo.signatures.length > 0) {
				Log.i(TAG, "App has " + packageInfo.signatures.length + " signature(s)");
				// Don't log the actual signature for security reasons, just confirm it exists
			} else {
				Log.w(TAG, "App has no signatures - this may cause Google Play Services issues");
			}
			
		} catch (Exception e) {
			Log.e(TAG, "Error logging package info: " + e.getMessage());
		}
	}
	public void setLocaleSettings(String localeString){

		SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("locale",localeString);
		editor.apply();
	}
	public void setLocale(String localeString){
		Locale locale = new Locale(localeString);
		Resources res = getResources();
		DisplayMetrics dm = res.getDisplayMetrics();

		Configuration config = new Configuration();
		config.locale = locale;
		res.updateConfiguration(config, dm);
	}


	private void getPrefs() {
		// Get the xml/preferences.xml preferences using modern approach
		SharedPreferences prefs = androidx.preference.PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		CheckboxPreference = prefs.getBoolean("example_checkbox", false);
	}




	private void loadRewardedAd() {
		if (rewardedAd == null) {
			isLoading = true;
			AdRequest adRequest = new AdRequest.Builder().build();
			RewardedAd.load(
					this,
					Constants.KEY_VIDEO_UNIT_ID,
					adRequest,
					new RewardedAdLoadCallback() {
						@Override
						public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
							// Handle the error.
							Log.d(TAG, loadAdError.getMessage());
							rewardedAd = null;
							ListaPrzebojowDiscoPolo.this.isLoading = false;
						//	Toast.makeText(ListaPrzebojowDiscoPolo.this, "onAdFailedToLoad", Toast.LENGTH_SHORT).show();
							Log.d(TAG, "onAdFailedToLoad");
							Log.w("ListaprzebojowDiscoPolo", "Ad failed to load: " + loadAdError);

						}

						@Override
						public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
							ListaPrzebojowDiscoPolo.this.rewardedAd = rewardedAd;
							Log.d(TAG, "onAdLoaded");
							ListaPrzebojowDiscoPolo.this.isLoading = false;
							//Toast.makeText(ListaPrzebojowDiscoPolo.this, "onAdLoaded", Toast.LENGTH_SHORT).show();
						}
					});
		}
	}


	private void showRewardedVideo() {

		if (rewardedAd == null) {
			Log.d("TAG", "The rewarded ad wasn't ready yet.");
			return;
		}

		rewardedAd.setFullScreenContentCallback(
				new FullScreenContentCallback() {
					@Override
					public void onAdShowedFullScreenContent() {
						// Called when ad is shown.
						Log.d(TAG, "onAdShowedFullScreenContent");
					//	Toast.makeText(ListaPrzebojowDiscoPolo.this, "onAdShowedFullScreenContent", Toast.LENGTH_SHORT)
					//			.show();
					}

					@Override
					public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
						// Called when ad fails to show.
						Log.d(TAG, "onAdFailedToShowFullScreenContent");
						// Don't forget to set the ad reference to null so you
						// don't show the ad a second time.
						rewardedAd = null;
					//	Toast.makeText(
					//			ListaPrzebojowDiscoPolo.this, "onAdFailedToShowFullScreenContent", Toast.LENGTH_SHORT)
					//			.show();
					}

					@Override
					public void onAdDismissedFullScreenContent() {
						// Called when ad is dismissed.
						// Don't forget to set the ad reference to null so you
						// don't show the ad a second time.
						rewardedAd = null;
						Log.d(TAG, "onAdDismissedFullScreenContent");
					//	Toast.makeText(ListaPrzebojowDiscoPolo.this, "onAdDismissedFullScreenContent", Toast.LENGTH_SHORT)
					//			.show();
						// Preload the next rewarded ad.
						ListaPrzebojowDiscoPolo.this.loadRewardedAd();

					}
				});
		Activity activityContext = ListaPrzebojowDiscoPolo.this;
		rewardedAd.show(
				activityContext,
				rewardItem -> {
					// Handle the reward.
					Log.d("TAG", "The user earned the reward.");
					adReward = true;
					// Refresh all fragments with adReward = true
					refreshListaWithAdReward();
					refreshPoczekalniaWithAdReward();
					refreshMojaListaWithAdReward();
					refreshNotowaniaWithAdReward();
					refreshNowosciWithAdReward();

					// [START image_view_event]
					if (mFirebaseAnalytics != null) {
						Bundle bundlereward = new Bundle();
						bundlereward.putString(FirebaseAnalytics.Param.GROUP_ID, androidId);
						bundlereward.putString(FirebaseAnalytics.Param.ITEM_NAME, "rewardvideo");
						bundlereward.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "on_rewarded_video");
						mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundlereward);

						Bundle bundleparams = new Bundle();
						bundleparams.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "rewarded_video");
						mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundleparams);
					}
					// [END image_view_event]


				}
		);
	}

	private AlertDialog createProgressDialog(String message) {
		// Check if activity is still valid
		if (isFinishing() || isDestroyed()) {
			return null;
		}
		
		try {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			// Create a simple layout with progress bar and text
			android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
			layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
			layout.setPadding(50, 50, 50, 50);

			ProgressBar progressBar = new ProgressBar(this);
			progressBar.setIndeterminate(true);
			android.widget.LinearLayout.LayoutParams progressParams =
					new android.widget.LinearLayout.LayoutParams(
							android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
							android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
			progressParams.setMargins(0, 0, 30, 0);
			progressBar.setLayoutParams(progressParams);

			TextView textView = new TextView(this);
			textView.setText(message);
			textView.setTextSize(16);
			android.widget.LinearLayout.LayoutParams textParams =
					new android.widget.LinearLayout.LayoutParams(
							android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
							android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
			textView.setLayoutParams(textParams);

			layout.addView(progressBar);
			layout.addView(textView);

			builder.setView(layout);
			builder.setCancelable(false);
			
			AlertDialog dialog = builder.create();
			
			// Set window flags to prevent input channel issues
			if (dialog.getWindow() != null) {
				dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION);
			}

			return dialog;
		} catch (Exception e) {
			Log.e("DialogCreate", "Error creating progress dialog: " + e.getMessage());
			return null;
		}
	}

	// Refresh MojaLista with adReward = true
	public void refreshMojaListaWithAdReward() {
		executorService.execute(() -> {
			boolean connectionError = false;

			try {
				XMLParser parser = new XMLParser();

				// Build a new list first; only swap on success to avoid clearing UI on failure
				ArrayList<HashMap<String, String>> newSongsListMojalista = new ArrayList<>();

				String xml_mojalista = parser.getXmlFromUrlMoja(buildSafeUrl(Constants.URL_MOJALISTA));
				Document docmoja = parser.getDomElementMoja(xml_mojalista);
				NodeList nl = docmoja.getElementsByTagName(Constants.KEY_SONG_MOJA);

				// Calculate max votes for progress from the freshly fetched data
				int maxVotes = 1; // default to 1 to avoid division by zero
				try {
					if (nl != null && nl.getLength() > 0) {
						Element first = (Element) nl.item(0);
						maxVotes = Integer.parseInt(parser.getValue(first, Constants.KEY_VOTES));
						if (maxVotes == 0) maxVotes = 1;
					}
				} catch (Exception ignore) {
					maxVotes = 1;
				}

				for (int i = 0; i < nl.getLength(); i++) {
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);

					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
					map.put(Constants.KEY_POSITION, parser.getValue(e, Constants.KEY_POSITION));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					// Show votes since adReward is true
					map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));

					// Setting votes progress
					int currentVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
					int votesProgress = (currentVotes * 100) / maxVotes;
					map.put(Constants.KEY_VOTES_PROGRESS, Integer.toString(votesProgress));
					map.put(Constants.KEY_SHOW_VOTES_PROGRESS, "TRUE");

					newSongsListMojalista.add(map);
				}

				// Only swap the data if parsing fetched something
				if (nl != null && nl.getLength() > 0) {
					synchronized(songsListMojalista) {
						songsListMojalista.clear();
						songsListMojalista.addAll(newSongsListMojalista);
					}
					Log.i(TAG, "songsListMojalista refreshed: size=" + songsListMojalista.size());
				} else {
					Log.w(TAG, "songsListMojalista refresh returned empty; keeping existing data");
				}

			} catch (Exception e) {
				connectionError = true;
			}

			// Update UI on main thread
			final boolean finalConnectionError = connectionError;
			mainHandler.post(() -> {
				MojaListaFragment mojaListaFragment = getFragmentByPosition(TabPagerAdapter.TAB_MOJALISTA);
				if (mojaListaFragment != null) {
					mojaListaFragment.updateAdapter();
				}

				if (finalConnectionError) {
					Toast.makeText(ListaPrzebojowDiscoPolo.this, getString(R.string.text_connection_error), Toast.LENGTH_SHORT).show();
				}
			});

		});
	}

	// Refresh Nowosci with adReward = true
	public void refreshNowosciWithAdReward() {
		executorService.execute(() -> {
			boolean connectionError = false;

			try {
				XMLParser parser = new XMLParser();

				// Clear and reload nowosci data with adReward = true
				synchronized(songsListNowosci) {  songsListNowosci.clear(); }

				String xml_nowosci = parser.getXmlFromUrl2(Constants.URL_NOWOSCI.replace("LANG", language));
				Document doc2 = parser.getDomElement2(xml_nowosci);
				NodeList nl2 = doc2.getElementsByTagName(Constants.KEY_SONG_NOWOSCI);

				for (int i = 0; i < nl2.getLength(); i++) {
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl2.item(i);

					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					// Show votes since adReward is true
					map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));

					synchronized(songsListNowosci) { songsListNowosci.add(map); }
					Log.i(TAG, "songsListNowosci element: " + songsListNowosci );
				}

			} catch (Exception e) {
				connectionError = true;
			}

			// Update UI on main thread
			final boolean finalConnectionError = connectionError;
			mainHandler.post(() -> {
				NowosciFragment nowosciFragment = getFragmentByPosition(TabPagerAdapter.TAB_NOWOSCI);
				if (nowosciFragment != null) {
					nowosciFragment.updateAdapter();
				}

				if (finalConnectionError) {
					Toast.makeText(ListaPrzebojowDiscoPolo.this, getString(R.string.text_connection_error), Toast.LENGTH_SHORT).show();
				}
			});
		});
	}

	// Refresh Lista with adReward = true
	public void refreshListaWithAdReward() {
		executorService.execute(() -> {
			boolean connectionError = false;

			try {
				XMLParser parser = new XMLParser();

				// Clear and reload lista data with adReward = true
				synchronized(songsList) { songsList.clear(); }

				String xml = parser.getXmlFromUrl(Constants.URL.replace("LANG", language));
				Document doc = parser.getDomElement(xml);
				NodeList nlInfo = doc.getElementsByTagName(Constants.KEY_INFO);
				Element el = (Element) nlInfo.item(0);
				info = parser.getValue(el, Constants.KEY_INFO);

				int votesProgress;
				int maxVotes = 0;
				int currentVotes;

				NodeList nl = doc.getElementsByTagName(Constants.KEY_SONG);

				for (int i = 0; i < nl.getLength(); i++) {
									HashMap<String, String> map = new HashMap<>();
				Element e = (Element) nl.item(i);

				map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
				map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
				map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
				map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
				map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
				// Show votes since adReward is true
				map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
				map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
				map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
				map.put(Constants.KEY_POSITION, parser.getValue(e, Constants.KEY_POSITION));
				map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
				map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));
				
				// Format the place change text with arrow and number
				String originalChangeText = parser.getValue(e, Constants.KEY_PLACE_CHANGE);
				String formattedChangeText = formatPlaceChangeText(originalChangeText);
				map.put(Constants.KEY_PLACE_CHANGE, formattedChangeText + Constants.TEXT_SEPARATOR);

				if (originalChangeText.contains(getString(R.string.text_awans))) {
					map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_UP);
				} else if (originalChangeText.contains(getString(R.string.text_spadek))) {
					map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_DOWN);
				} else {
					map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_NO_CHANGE);
				}

				if (i == 0) {
					maxVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
					if (maxVotes == 0) {
						maxVotes = 1;
					}
				}
				currentVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
				votesProgress = (currentVotes * 100) / maxVotes;
				map.put(Constants.KEY_VOTES_PROGRESS, Integer.toString(votesProgress));
				map.put(Constants.KEY_SHOW_VOTES_PROGRESS, "TRUE");

				synchronized(songsList) { songsList.add(map); }
				Log.i(TAG, "songsList element: " + songsList );

			}

		} catch (Exception e) {
			connectionError = true;
		}

		// Update UI on main thread
		final boolean finalConnectionError = connectionError;
		mainHandler.post(() -> {
			ListaFragment listaFragment = getFragmentByPosition(TabPagerAdapter.TAB_LISTA);
			if (listaFragment != null) {
				listaFragment.updateAdapter();
			}

			if (finalConnectionError) {
				Toast.makeText(ListaPrzebojowDiscoPolo.this, getString(R.string.text_connection_error), Toast.LENGTH_SHORT).show();
			}
		});
	});
}

	// Refresh Poczekalnia with adReward = true
	public void refreshPoczekalniaWithAdReward() {
		executorService.execute(() -> {
			boolean connectionError = false;

			try {
				XMLParser parser = new XMLParser();

				// Clear and reload poczekalnia data with adReward = true
				synchronized(songsListPocz) { songsListPocz.clear(); }

				String xml = parser.getXmlFromUrl(Constants.URL.replace("LANG", language));
				Document doc = parser.getDomElement(xml);

				NodeList nl = doc.getElementsByTagName(Constants.KEY_SONG_POCZ);

				// Calculate max votes for progress from the freshly fetched data
				int maxVotes = 1; // default to 1 to avoid division by zero
				try {
					if (nl != null && nl.getLength() > 0) {
						Element first = (Element) nl.item(0);
						maxVotes = Integer.parseInt(parser.getValue(first, Constants.KEY_VOTES));
						if (maxVotes == 0) maxVotes = 1;
					}
				} catch (Exception ignore) {
					maxVotes = 1;
				}

				for (int i = 0; i < nl.getLength(); i++) {
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);

					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
					// Show votes since adReward is true
					map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_POSITION, parser.getValue(e, Constants.KEY_POSITION));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));

					// Setting votes progress
					int currentVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
					int votesProgress = (currentVotes * 100) / maxVotes;
					map.put(Constants.KEY_VOTES_PROGRESS, Integer.toString(votesProgress));
					map.put(Constants.KEY_SHOW_VOTES_PROGRESS, "TRUE");

					synchronized(songsListPocz) { songsListPocz.add(map); }
					Log.i(TAG, "songsListPocz element: " + songsListPocz );
				}

			} catch (Exception e) {
				connectionError = true;
			}

			// Update UI on main thread
			final boolean finalConnectionError = connectionError;
			mainHandler.post(() -> {
				PoczekalniaFragment poczekalniaFragment = getFragmentByPosition(TabPagerAdapter.TAB_POCZEKALNIA);
				if (poczekalniaFragment != null) {
					poczekalniaFragment.updateAdapter();
				} else {
					// Fragment not found, try to refresh all adapters as fallback
					updateAllFragmentAdapters();
				}

				if (finalConnectionError) {
					Toast.makeText(ListaPrzebojowDiscoPolo.this, getString(R.string.text_connection_error), Toast.LENGTH_SHORT).show();
				}
			});
		});
	}

	// Refresh Notowania with adReward = true
	public void refreshNotowaniaWithAdReward() {
		executorService.execute(() -> {
			boolean connectionError = false;

			try {
				XMLParser parser = new XMLParser();

				// Clear and reload notowania data with adReward = true
				synchronized(notowaniaList) { notowaniaList.clear(); }

				String xml = parser.getXmlFromUrl(Constants.URL_NOTOWANIA.replace("LANG", language).replace("START_NOTOWANIE_ID", notowanieId));
				Document doc = parser.getDomElement(xml);

				int votesProgress;
				int maxVotes = 0;
				int currentVotes;

				NodeList nl = doc.getElementsByTagName(Constants.KEY_SONG);

				for (int i = 0; i < nl.getLength(); i++) {
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);

					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_ID_GRUPY, parser.getValue(e, Constants.KEY_ID_GRUPY));
					map.put(Constants.KEY_TITLE, parser.getValue(e, Constants.KEY_TITLE));
					map.put(Constants.KEY_ARTIST, parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID, parser.getValue(e, Constants.KEY_ARTIST_ID));
					// Show votes since adReward is true
					map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					map.put(Constants.KEY_THUMB_URL, parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_CREATE_DATE, " " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					map.put(Constants.KEY_POSITION, parser.getValue(e, Constants.KEY_POSITION));
					map.put(Constants.KEY_VIDEO, parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY, parser.getValue(e, Constants.KEY_SPOTIFY));
					map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_NO_CHANGE);

					if (i == 0) {
						maxVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
						if (maxVotes == 0) {
							maxVotes = 1;
						}
					}
					currentVotes = Integer.parseInt(parser.getValue(e, Constants.KEY_VOTES));
					votesProgress = (currentVotes * 100) / maxVotes;
					map.put(Constants.KEY_VOTES_PROGRESS, Integer.toString(votesProgress));
					map.put(Constants.KEY_SHOW_VOTES_PROGRESS, "TRUE");

					synchronized(notowaniaList) { notowaniaList.add(map); }
					Log.i(TAG, "notowaniaList element: " + notowaniaList );
				}

			} catch (Exception e) {
				connectionError = true;
			}

			// Update UI on main thread
			final boolean finalConnectionError = connectionError;
			mainHandler.post(() -> {
				NotowaniaFragment notowaniaFragment = getFragmentByPosition(TabPagerAdapter.TAB_NOTOWANIA);
				if (notowaniaFragment != null) {
					notowaniaFragment.updateAdapter();
				}

				if (finalConnectionError) {
					Toast.makeText(ListaPrzebojowDiscoPolo.this, getString(R.string.text_connection_error), Toast.LENGTH_SHORT).show();
				}
			});
		});
	}

	// Fragment readiness tracking
	private final Set<String> readyFragments = new HashSet<>();
	private boolean dataLoadCompleted = false;
	private boolean pendingFragmentUpdate = false;

	/**
	 * Formats the place change text to display with arrow and number
	 * e.g., "Awans o 1" becomes "↑ +1", "Spadek o 3" becomes "↓ -3"
	 * @param changeText The original change text from XML
	 * @return Formatted text with arrow and number
	 */
	private String formatPlaceChangeText(String changeText) {
		if (changeText == null || changeText.isEmpty()) {
			return "";
		}
		
		// Check if it's an increase (Awans)
		if (changeText.contains("Awans")) {
			// Extract the number from text like "Awans o 1"
			String[] parts = changeText.split("\\s+");
			for (int i = 0; i < parts.length; i++) {
				if (parts[i].equals("o") && i + 1 < parts.length) {
					try {
						int changeValue = Integer.parseInt(parts[i + 1]);
						return "↑ +" + changeValue + " awans" ;
					} catch (NumberFormatException e) {
						// If parsing fails, return original text
						return changeText;
					}
				}
			}
		}
		// Check if it's a decrease (Spadek)
		else if (changeText.contains("Spadek")) {
			// Extract the number from text like "Spadek o 3"
			String[] parts = changeText.split("\\s+");
			for (int i = 0; i < parts.length; i++) {
				if (parts[i].equals("o") && i + 1 < parts.length) {
					try {
						int changeValue = Integer.parseInt(parts[i + 1]);
						return "↓ -" + changeValue  + " spadek" ;
					} catch (NumberFormatException e) {
						// If parsing fails, return original text
						return changeText;
					}
				}
			}
		}
		
		// For other cases (no change, new entry, etc.), return original text
		return changeText;
	}
	
	// Public method to play Spotify track from adapters
	public void playSpotifyTrack(String spotifyTrackId, String title, String artist) {
		// [START image_view_event]
		if (mFirebaseAnalytics != null) {
			Bundle bundle = new Bundle();
			bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, title);
//			bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "tytul");
			bundle.putString(FirebaseAnalytics.Param.ITEM_LIST_ID, spotifyTrackId);
//			bundle.putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, "spotifyTrackId");
			bundle.putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, artist);
//			bundle.putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, "wykonawca");
			bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "play_spotify_track");
			mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
		}
		// [END image_view_event]
		Log.d("SpotifyDebug", "playSpotifyTrack called - trackId: " + spotifyTrackId + ", title: " + title + ", artist: " + artist);
		
		if (spotifyBottomSheetController == null) {
			Log.e("SpotifyDebug", "SpotifyBottomSheetController is null! Attempting to reinitialize...");
			// Try to reinitialize if null
			ViewGroup rootView = findViewById(R.id.root);
					if (rootView != null) {
			spotifyBottomSheetController = new SpotifyBottomSheetController(this, rootView);
			Log.d("SpotifyDebug", "SpotifyBottomSheetController reinitialized");
		} else {
				Log.e("SpotifyDebug", "Could not reinitialize - root view is null");
				return;
			}
		}
		
		// Add a small delay to ensure the Alert Dialog is fully dismissed before showing the bottom sheet
		// This prevents UI conflicts between dialog dismissal and bottom sheet display
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			Log.d("SpotifyDebug", "Calling spotifyBottomSheetController.playTrack() after delay");
			spotifyBottomSheetController.playTrack(spotifyTrackId, title, artist);
		}, 100); // 100ms delay to allow dialog dismissal to complete
	}
	
	// Get Spotify Bottom Sheet Controller
	public SpotifyBottomSheetController getSpotifyBottomSheetController() {
		return spotifyBottomSheetController;
	}
	
	/**
	 * Start Spotify OAuth authorization flow with PKCE
	 * This method should be called from the UI thread when user interaction is needed
	 */
	public void launchSpotifyForAuthorization() {
		try {
			Log.d(TAG, "Starting Spotify OAuth PKCE authorization flow");
			SpotifyAuthManager authManager = SpotifyAuthManager.getInstance(this);
			SpotifyService spotifyService = SpotifyService.getInstance(this);
			
			// Start the proper OAuth PKCE authorization flow
			authManager.startAuthorization(this, new SpotifyAuthManager.AuthorizationListener() {
				@Override
				public void onAuthorizationComplete(String accessToken) {
					Log.d(TAG, "Spotify OAuth PKCE authorization completed successfully");
					runOnUiThread(() -> {
						Toast.makeText(ListaPrzebojowDiscoPolo.this, "Autoryzacja Spotify zakończona pomyślnie", Toast.LENGTH_SHORT).show();
						
						// Now try to connect to Spotify App Remote
						if (spotifyBottomSheetController != null && spotifyBottomSheetController.isBottomSheetVisible()) {
							// Add a connection listener to retry the track after successful connection
							spotifyService.addConnectionListener(new SpotifyService.SpotifyConnectionListener() {
								@Override
								public void onConnected() {
									Log.d(TAG, "Connected after OAuth authorization, retrying track");
									// Retry the current track
									if (spotifyBottomSheetController != null) {
										spotifyBottomSheetController.retryCurrentTrack();
									}
									// Remove this listener
									spotifyService.removeConnectionListener(this);
								}
								
								@Override
								public void onConnectionFailed(Throwable error) {
									Log.e(TAG, "Connection failed after OAuth authorization", error);
									// Remove this listener
									spotifyService.removeConnectionListener(this);
								}
								
								@Override
								public void onDisconnected() {
									// Remove this listener
									spotifyService.removeConnectionListener(this);
								}
							});
							
							// Force reconnect now that we have proper authorization
							spotifyService.forceReconnect();
						}
					});
				}
				
				@Override
				public void onAuthorizationFailed(String error) {
					Log.e(TAG, "Spotify authorization failed: " + error);
					runOnUiThread(() -> {
						String userMessage;
						if (error != null && error.contains("Network error")) {
							userMessage = "Błąd sieci. Sprawdź połączenie internetowe i spróbuj ponownie.";
						} else if (error != null && error.contains("cancelled")) {
							userMessage = "Autoryzacja została anulowana";
						} else {
							userMessage = "Błąd autoryzacji Spotify. Spróbuj ponownie.";
						}
						Toast.makeText(ListaPrzebojowDiscoPolo.this, userMessage, Toast.LENGTH_LONG).show();
						
						// If authorization failed, ensure bottom sheet shows retry option
						if (spotifyBottomSheetController != null && spotifyBottomSheetController.isBottomSheetVisible()) {
							// Let the bottom sheet handle the error display
							Log.d(TAG, "Authorization failed, bottom sheet should show retry option");
						}
					});
				}
			});
			
		} catch (Exception e) {
			Log.e(TAG, "Error starting Spotify authorization", e);
			Toast.makeText(this, "Błąd podczas uruchamiania autoryzacji Spotify", Toast.LENGTH_SHORT).show();
		}
	}


}
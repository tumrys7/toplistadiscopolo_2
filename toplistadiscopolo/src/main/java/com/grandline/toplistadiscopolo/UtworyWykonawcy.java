package com.grandline.toplistadiscopolo;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.grandline.toplistadiscopolo.adapters.LazyAdapter;
import com.grandline.toplistadiscopolo.adapters.NativeAdAdapterWrapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UtworyWykonawcy extends AppCompatActivity {

	boolean adReward;
	AdView adView;
	ListView wykUtwory;
	LazyAdapter adapter;
	NativeAdAdapterWrapper adapterWrapper;
	boolean voted = false;
	Bundle bun;
	AlertDialog progressDialog;
	AlertDialog progressDialogVote;
	String myListType;
	String myIdWykonawcy;
	String votingListId;
	String url;
	String language;
	String glosTeledysk;
	ArrayList<HashMap<String, String>> wykSongsList;

	// ExecutorService and Handler for background tasks
	private ExecutorService executorService;
	private Handler mainHandler;

	// Activity result launcher
	private ActivityResultLauncher<Intent> activityResultLauncher;

	// [START declare_analytics]
	private FirebaseAnalytics mFirebaseAnalytics;
	// [END declare_analytics]
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		// Initialize ExecutorService and Handler
		executorService = Executors.newFixedThreadPool(3);
		mainHandler = new Handler(Looper.getMainLooper());

		// Initialize ActivityResultLauncher
		activityResultLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result -> {
					// Handle result if needed
				}
		);

		EdgeToEdge.enable(this);

		language = getLocaleSettings();
		setContentView(R.layout.utwory_wykonawcy);

		// Wywołanie metody, ustawiającej czarne ikony i przezroczyste paski systemowe
		getWindow().getDecorView().post(new Runnable() {
			@Override
			public void run() {
				setLightSystemBars(getWindow(), true, true);
			}
		});

		final View root = findViewById(R.id.root);
		if (root != null) {
			ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
				Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
				v.setPadding(v.getPaddingLeft(), sysBars.top, v.getPaddingRight(), sysBars.bottom);
				return WindowInsetsCompat.CONSUMED;
			});
		}

		// Setup toolbar similar to PlayActivity
		MaterialToolbar toolbar = findViewById(R.id.utwory_toolbar);

		toolbar.setTitle(getString(R.string.utwory_wykonawcy));
// jeśli dostępne (w nowoczesnych wersjach) ustaw:
		toolbar.setTitleCentered(true);

		if (toolbar != null) {
			setSupportActionBar(toolbar);
			if (getSupportActionBar() != null) {
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				getSupportActionBar().setDisplayShowHomeEnabled(true);
				getSupportActionBar().setDisplayShowTitleEnabled(false);
			}
			toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
		}

		// [START shared_app_measurement]
		// Obtain the FirebaseAnalytics instance.
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
		// [END shared_app_measurement]
		bun = getIntent().getExtras();
		assert bun != null;
		String authId = bun.getString("param_auth_id");
		adReward = bun.getBoolean("param_ad_reward", false);

		refreshAuthSong(authId);
		// Show only native ads inside ListView in this activity; disable banners
		// by not calling createAd() or createNativeAd() banner loaders here.

		// Setup OnBackPressedCallback
		OnBackPressedCallback callback = new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				Bundle conData = new Bundle();
				conData.putBoolean("param_return", voted);
				Intent intent = new Intent();
				intent.putExtras(conData);
				setResult(RESULT_OK, intent);

				if (adapterWrapper != null) {
					adapterWrapper.destroyAds();
				}
				finish();
				showAdFullscreen();
			}
		};
		getOnBackPressedDispatcher().addCallback(this, callback);
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


	@Override
	protected void onDestroy() {
		// Clean up ExecutorService to prevent memory leaks
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown();
		}
		if (adapterWrapper != null) {
			adapterWrapper.destroyAds();
		}
		super.onDestroy();
	}

	public void refreshAuthSong(String authId) {

		url = Constants.UTWORY_WYK_URL.replace("AUTH_ID", authId);
		wykUtwory = findViewById(R.id.wykUtwory);
		
		// Clear existing data if list exists, otherwise create new list
		if (wykSongsList == null) {
			wykSongsList = new ArrayList<>();
		} else {
			synchronized(wykSongsList) {
				wykSongsList.clear();
			}
		}
		
		adapter = new LazyAdapter(this, wykSongsList);
		adapterWrapper = new NativeAdAdapterWrapper(this, adapter);
		wykUtwory.setAdapter(adapterWrapper);
		wykUtwory.setOnItemClickListener((parent, view, position, id) -> {
			// Skip clicks on the ad row
			if (adapterWrapper != null && adapterWrapper.isAdPosition(position)) {
				return;
			}
			int contentPosition = adapterWrapper != null ? adapterWrapper.toContentPosition(position) : position;
			// Validate position before calling showSongMenu
			if (wykUtwory != null && contentPosition >= 0 && contentPosition < adapter.getCount()) {
				showSongMenu(contentPosition, Constants.KEY_UTW_WYKONAWCY);
			} else {
				Log.e("UtworyWykonawcy", "Invalid click position " + position +
					  " for adapter count " + (adapter != null ? adapter.getCount() : 0));
			}
		});
		progressDialog = createProgressDialog(getString(R.string.text_auth_refresh_list));
		progressDialog.show();
		refreshAuthSongsInBackground(url);
		loadNativeAds();
	}


	@SuppressWarnings({ "unchecked" })
	public void showSongMenu(int position, String listType){
		HashMap<String, String> o;

		if (Objects.equals(listType, Constants.KEY_UTW_WYKONAWCY)) {

			o = (HashMap<String, String>) adapter.getItem(position);
		} else {
            o = new HashMap<>();
        }

        final String idListy = o.get(Constants.KEY_ID);
		final String idWykonawcy = o.get(Constants.KEY_ARTIST_ID);
		String title = o.get(Constants.KEY_TITLE);
		String artist = o.get(Constants.KEY_ARTIST);

		final String teledysk = o.get(Constants.KEY_VIDEO);
		final String spotify = o.get(Constants.KEY_SPOTIFY);

		//final CharSequence[] RewardWykItems = {getString(R.string.zaglosuj),getString(R.string.liczba_glosow), getString(R.string.teledysk)};
		final CharSequence[] wykItems = {getString(R.string.zaglosuj), getString(R.string.teledysk),getString(R.string.spotify)};

		AlertDialog.Builder builder = new AlertDialog.Builder(UtworyWykonawcy.this);
		builder.setTitle(title);
		//  builder.setIcon(R.drawable.ic_menu_more);

		if (!adReward) {
			if (Objects.equals(listType, Constants.KEY_UTW_WYKONAWCY)) {
				builder.setItems(wykItems, (dialog, item) -> {
					//Toast.makeText(getApplicationContext(), wykItems[item], Toast.LENGTH_SHORT).show();
					if (wykItems[item] == getString(R.string.zaglosuj)) {
						glosTeledysk = "0";
						zaglosuj(idListy, Constants.KEY_UTW_WYKONAWCY, idWykonawcy, glosTeledysk);
					} else if (wykItems[item] == getString(R.string.teledysk)) {
						glosTeledysk = "1";
						zaglosuj(idListy, Constants.KEY_UTW_WYKONAWCY, idWykonawcy, glosTeledysk);
						try {
							Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(teledysk));
						//	browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						//	if (browserIntent.resolveActivity(getPackageManager()) != null) {
							startActivity(browserIntent);
						//	}
						} catch (Exception e) {
							Log.e("VideoPlayback", "Error launching video: " + e.getMessage());
						}
					                    } else if(wykItems[item]==getString(R.string.spotify)){
                        Intent intent = new Intent(UtworyWykonawcy.this, PlayActivity.class);
                        intent.putExtra("spotify_url", spotify);
                        intent.putExtra("title", title);
                        intent.putExtra("artist", artist);
                        startActivity(intent);
                    }
				});
			}
		}

		AlertDialog alert = builder.create();
		alert.show();
	}

	private void zaglosuj(String idUtworu, String listType, String idWykonawcy, String teledysk) {
		if (canUserVotes(idUtworu)){
			// Using a more privacy-friendly approach instead of device identifier
			SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
			String androidId = prefs.getString("user_id", null);
			if (androidId == null) {
				androidId = java.util.UUID.randomUUID().toString();
				prefs.edit().putString("user_id", androidId).apply();
			}
			String url;
			try{
				url = Constants.VOTE_URL.replace("ID_LISTY", idUtworu).replace("DEV_ID", androidId).replace("LANG", language).replace("TEL_PARAM", teledysk);
			} catch (NullPointerException e) {
				url = Constants.VOTE_URL.replace("ID_LISTY", idUtworu).replace("DEV_ID", "UNKNOWN").replace("LANG", language).replace("TEL_PARAM", teledysk);
			}
			progressDialogVote = createProgressDialog(getString(R.string.text_voting));
			progressDialogVote.show();
			myListType = listType;
			myIdWykonawcy = idWykonawcy;
			votingListId = idUtworu;
			voteInBackground(url, listType);
			// [START glos_utw_wykon_event]
			Bundle bundle = new Bundle();
			bundle.putString(FirebaseAnalytics.Param.ITEM_ID, myIdWykonawcy);
			bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, votingListId);
			bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "glos_utw_wykonawcy");
			mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
			// [END glos_utw_wykon_event]
		} else {
			if (!Objects.equals(teledysk, "1")){
				Toast.makeText(getApplicationContext(), getString(R.string.text_too_many_votes).replace("VOTES_INTERVAL",Integer.toString(Constants.VOTES_INTERVAL)), Toast.LENGTH_LONG).show();
			}
		}

	}

	// Replace RefreshAuthSongs AsyncTask with ExecutorService + Handler
	private void refreshAuthSongsInBackground(String url) {
		executorService.execute(() -> {
			boolean connectionError = false;

			XMLParser parser = new XMLParser();

			try {

				String xml = parser.getXmlFromUrl(url); // getting XML from URL

				Document doc = parser.getDomElement(xml); // getting DOM element

				// utwory
				NodeList nl = doc.getElementsByTagName(Constants.KEY_SONG);

				// looping through all song nodes <song>
				for (int i = 0; i < nl.getLength(); i++) {
					// creating new HashMap
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nl.item(i);
					// adding each child node to HashMap key => value
					map.put(Constants.KEY_ID, parser.getValue(e, Constants.KEY_ID));
					map.put(Constants.KEY_TITLE,parser.getValue(e, Constants.KEY_TITLE));
					map.put(Constants.KEY_ARTIST,parser.getValue(e, Constants.KEY_ARTIST));
					map.put(Constants.KEY_ARTIST_ID,parser.getValue(e, Constants.KEY_ARTIST_ID));
					map.put(Constants.KEY_THUMB_URL,parser.getValue(e, Constants.KEY_THUMB_URL));
					map.put(Constants.KEY_VIDEO,parser.getValue(e, Constants.KEY_VIDEO));
					map.put(Constants.KEY_SPOTIFY,parser.getValue(e, Constants.KEY_SPOTIFY));
					map.put(Constants.KEY_CREATE_DATE," " + parser.getValue(e, Constants.KEY_CREATE_DATE));
					//only for PRO Version
					if (adReward) {
						map.put(Constants.KEY_VOTES, " | " + getString(R.string.text_glosow) + " " + parser.getValue(e, Constants.KEY_VOTES));
					}
					//showing or not showing progress bar
					map.put(Constants.KEY_SHOW_VOTES_PROGRESS,"FALSE");
					// adding HashList to ArrayList - synchronized to prevent UI thread conflicts
					synchronized(wykSongsList) {
						wykSongsList.add(map);
					}
				}
			}
			catch (IOException e){
				connectionError = true;
			}

			// Update UI on main thread
			final boolean finalConnectionError = connectionError;
			mainHandler.post(() -> {
				// Notify adapter that data has changed
				if (adapter != null) {
					adapter.safeNotifyDataSetChanged();
					if (adapterWrapper != null) {
						adapterWrapper.notifyDataSetChanged();
					}
				}
				
				if (progressDialog != null && progressDialog.isShowing()) {
					progressDialog.dismiss();
				}
				if (finalConnectionError) {
					new AlertDialog.Builder(UtworyWykonawcy.this)
							.setTitle(R.string.text_connection_error_title)
							.setMessage(getString(R.string.text_connection_error))
							.setNeutralButton("Ok",	null).show();
				}
			});
		});
	}

	// Replace Zaglosuj AsyncTask with ExecutorService + Handler
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
				if (progressDialogVote != null && progressDialogVote.isShowing()) {
					progressDialogVote.dismiss();
				}

				if (finalConnectionError) {
					new AlertDialog.Builder(UtworyWykonawcy.this)
							.setTitle(R.string.text_connection_error_title)
							.setMessage(getString(R.string.text_connection_error))
							.setNeutralButton("Ok",	null).show();
				} else {
					setUserVote(votingListId);
					if(Objects.equals(glosTeledysk, "0")){
						//	Snackbar snackbar = make(UtworyWykonawcy.this.getCurrentFocus(), voteMessage, Snackbar.LENGTH_LONG);
						//	snackbar.show();
						Toast.makeText(getApplicationContext(), finalVoteMessage, Toast.LENGTH_LONG).show();
					}
					if (Objects.equals(myListType, Constants.KEY_UTW_WYKONAWCY)) {
						voted = true;
						if (adReward) {
							refreshAuthSong(myIdWykonawcy);
						}
					}
				}
			});
		});
	}

	// Load native ads to be inserted into the list
	private void loadNativeAds() {
		AdLoader adLoader = new AdLoader.Builder(this, getString(R.string.native_ad_unit_id))
				.forNativeAd(loadedAd -> {
					// If activity is destroyed before ad loaded, destroy ad and return
					if (isDestroyed() || isFinishing()) {
						loadedAd.destroy();
						return;
					}
					if (adapterWrapper != null) {
						adapterWrapper.addNativeAd(loadedAd);
					}
				})
				.withAdListener(new com.google.android.gms.ads.AdListener() {
					@Override
					public void onAdFailedToLoad(@NonNull LoadAdError adError) {
						Log.w("UtworyWykonawcy", "Native ad failed to load: " + adError);
					}
				})
				.withNativeAdOptions(new NativeAdOptions.Builder()
						.setRequestCustomMuteThisAd(true)
						.build())
				.build();

		// Request multiple ads so different ones can be shown on screen
		adLoader.loadAds(new AdRequest.Builder().build(), 10);
	}


	private void showAdFullscreen() {
		Intent intent = new Intent();
		intent.setClass(this,AdFullscreenActivity.class);
		activityResultLauncher.launch(intent);
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
		SimpleDateFormat newVoteDate;
        newVoteDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SharedPreferences.Editor editor = settings.edit();
		editor.putString(idListy, newVoteDate.format(System.currentTimeMillis()));
		editor.apply();

	}

	public String getLocaleSettings() {
		//SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		Locale lc = Locale.getDefault();

		//String localeString = settings.getString("locale",localeDefault);
		return lc.getLanguage();
	}

	private AlertDialog createProgressDialog(String message) {
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

		return builder.create();
	}


}

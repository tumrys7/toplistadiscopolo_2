package com.grandline.toplistadiscopolo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.constraintlayout.solver.ArrayLinkedVariables;
import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;
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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.analytics.FirebaseAnalytics;

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

import com.grandline.toplistadiscopolo.adapters.TabPagerAdapter;
import com.grandline.toplistadiscopolo.fragments.ListaFragment;
import com.grandline.toplistadiscopolo.fragments.PoczekalniaFragment;
import com.grandline.toplistadiscopolo.fragments.NowosciFragment;
import com.grandline.toplistadiscopolo.fragments.MojaListaFragment;
import com.grandline.toplistadiscopolo.fragments.WykonawcyFragment;
import com.grandline.toplistadiscopolo.fragments.NotowaniaFragment;


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
	
	// New ViewPager2 and TabLayout
	private ViewPager2 viewPager;
	private TabLayout tabLayout;
	private TabPagerAdapter tabPagerAdapter;
	private TabLayoutMediator tabLayoutMediator;
	
	// Fragment references
	private ListaFragment listaFragment;
	private PoczekalniaFragment poczekalniaFragment;
	private NowosciFragment nowosciFragment;
	private MojaListaFragment mojaListaFragment;
	private WykonawcyFragment wykonawcyFragment;
	private NotowaniaFragment notowaniaFragment;
	
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
    ProgressDialog progressDialog;
    ProgressDialog progressDialogVote;
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
    public String voteMessage;
    public boolean CheckboxPreference;
    
    private Toast toast;
    private long lastBackPressTime = 0;
    boolean connectionError = false;
    boolean isSpinnerClicked = false;

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
		
		// Initialize ExecutorService and Handler
		executorService = Executors.newFixedThreadPool(3);
		mainHandler = new Handler(Looper.getMainLooper());
		
		language = getLocaleSettings();
		notowanieId = Constants.VALUE_START_NOTOWANIE_ID; 
		//setLocale(language);
		setContentView(R.layout.main);

		// Włączenie na android >=7 Dangerous permission: zapis okładek w pamięci urządzenia
        verifyStoragePermissions(this);



		androidId = Settings.Secure.getString(getContentResolver(),
		         Settings.Secure.ANDROID_ID);
		// [START shared_app_measurement]
		// Obtain the FirebaseAnalytics instance.
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
		// [END shared_app_measurement]

		// [START user_property]
		mFirebaseAnalytics.setUserProperty("android_id", androidId);
		// [END user_property]


		setupTabLayoutWithViewPager();

		//only for Free Version
		if (!Constants.VERSION_PRO_DO_NOT_SHOW_BANNER) {
			// reklama
			createAd();
		}
		isSpinnerClicked = false;
		refreshListBackground();
		getPrefs();
		setupBackPressedCallback();

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
	    if (adView != null) {
	      adView.destroy();
	    }
	    // Clean up TabLayoutMediator
	    if (tabLayoutMediator != null) {
	    	tabLayoutMediator.detach();
	    }
	    // Clean up ExecutorService to prevent memory leaks
	    if (executorService != null && !executorService.isShutdown()) {
	    	executorService.shutdown();
	    }
	    super.onDestroy();
	  }

	@Override
	public void onResume() {
		//rewardedAd.resume(this);
		super.onResume();
	}

	@Override
	public void onPause() {
		//rewardedAd.pause(this);
		super.onPause();
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

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
			String androidId = Settings.Secure.getString(getContentResolver(),
			         Settings.Secure.ANDROID_ID);
			String url;
			try {
				url = Constants.VOTE_URL.replace("ID_LISTY", idUtworu).replace("DEV_ID", androidId).replace("LANG", language).replace("ID_GRUPY", idGrupy).replace("TEL_PARAM", teledysk);
			} catch (NullPointerException e) {
				url = Constants.VOTE_URL.replace("ID_LISTY", idUtworu).replace("DEV_ID", "UNKNOWN").replace("LANG", language).replace("ID_GRUPY", idGrupy).replace("TEL_PARAM", teledysk);
			}
	        progressDialogVote = ProgressDialog.show(ListaPrzebojowDiscoPolo.this, "", getString(R.string.text_voting));
			myListType = listType;
			myIdWykonawcy = idWykonawcy;
	        votingListId = idUtworu;
	        voteInBackground(url, listType);
			// [START image_view_event]
			Bundle bundle = new Bundle();
			bundle.putString(FirebaseAnalytics.Param.ITEM_ID, idUtworu);
			bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "id_utworu");
			bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "glos_lista");
			mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
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
		MobileAds.initialize(this, initializationStatus -> {
		});

		loadRewardedAd();

		// Initialize data lists
		songsList = new ArrayList<>();
		songsListPocz = new ArrayList<>();
		songsListNowosci = new ArrayList<>();
		songsListMojalista = new ArrayList<>();
        wykonList = new ArrayList<>();
        filteredWykonList = new ArrayList<>();
        notowaniaList = new ArrayList<>();
        notowPrzedzialyList = new ArrayList<>();
        listNotowPrzedzialy = new ArrayList<>();

        progressDialog = ProgressDialog.show(ListaPrzebojowDiscoPolo.this, "", getString(R.string.text_refresh_list));
        refreshListInBackground();
		if (!Constants.VERSION_PRO_DO_NOT_SHOW_BANNER) {
	    	if (adError){
	    		adView.loadAd(adRequest);
	    	}
		}
        
	}
	
	// Update all fragment adapters when data changes (replaces direct adapter updates)
	public void updateAllFragmentAdapters() {
		// Get fragment references from ViewPager2 adapter
		listaFragment = getFragmentByPosition(TabPagerAdapter.TAB_LISTA);
		poczekalniaFragment = getFragmentByPosition(TabPagerAdapter.TAB_POCZEKALNIA);
		nowosciFragment = getFragmentByPosition(TabPagerAdapter.TAB_NOWOSCI);
		mojaListaFragment = getFragmentByPosition(TabPagerAdapter.TAB_MOJALISTA);
		wykonawcyFragment = getFragmentByPosition(TabPagerAdapter.TAB_WYKONAWCY);
		notowaniaFragment = getFragmentByPosition(TabPagerAdapter.TAB_LISTA_2012);
		
		// Update fragment adapters
		if (listaFragment != null) {
			listaFragment.updateAdapter(songsList);
		}
		if (poczekalniaFragment != null) {
			poczekalniaFragment.updateAdapter(songsListPocz);
		}
		if (nowosciFragment != null) {
			nowosciFragment.updateAdapter(songsListNowosci);
		}
		if (mojaListaFragment != null) {
			mojaListaFragment.updateAdapter(songsListMojalista);
		}
		if (wykonawcyFragment != null) {
			wykonawcyFragment.updateAdapter(wykonList);
		}
		if (notowaniaFragment != null) {
			notowaniaFragment.updateAdapter(notowaniaList);
			// Handle spinner for NotowaniaFragment
			ArrayList<String> listNotowPrzedzialy = new ArrayList<>();
			for (HashMap<String, String> item : notowPrzedzialyList) {
				listNotowPrzedzialy.add(item.get(Constants.KEY_NOTOWANIE_NAZWA));
			}
			notowaniaFragment.updateSpinnerAdapter(notowPrzedzialyList, listNotowPrzedzialy);
			if (spinnerPosition != -1) {
				notowaniaFragment.setSpinnerSelection(spinnerPosition);
			}
		}
	}
	
	// Helper method to get fragment by position
	@SuppressWarnings("unchecked")
	private <T extends Fragment> T getFragmentByPosition(int position) {
		if (tabPagerAdapter != null) {
			String fragmentTag = "f" + position;
			Fragment fragment = getSupportFragmentManager().findFragmentByTag(fragmentTag);
			return (T) fragment;
		}
		return null;
	}
	
			// Handle spinner selection for NotowaniaFragment
	public void handleSpinnerSelection(String notowanieId, int position) {
		this.notowanieId = notowanieId;
		this.spinnerPosition = position;
		refreshListBackground();
	}
	
	// Filter wykonawcy method (moved from old implementation)
	public void filterWykonawcy(ArrayList<HashMap<String, String>> wykonList, CharSequence searchText) {
		filteredWykonList.clear();
		if (searchText.length() == 0) {
			filteredWykonList.addAll(wykonList);
		} else {
			String searchString = searchText.toString().toLowerCase();
			for (HashMap<String, String> item : wykonList) {
				String wykonawca = item.get(Constants.KEY_WYKONAWCA);
				if (wykonawca != null && wykonawca.toLowerCase().contains(searchString)) {
					filteredWykonList.add(item);
				}
			}
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.odswiez:
	        	refreshListBackground();
	            return true;
            case R.id.policy:
                Intent browserIntent2 = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.POLICY_URL));
                startActivity(browserIntent2);
                return true;
	        case R.id.kontakt:
				Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Constants.EMAIL_TO + "?subject=" +
						Uri.encode(getString(R.string.email_subject)) + "&body="));
				startActivity(email);
				return true;
	        case R.id.jezyk:
	        	showLanguageMenu();
	        	return true;	        	
	        case R.id.oProgramie:
	        	Intent intentAbout = new Intent(Intent.ACTION_VIEW);
	        	intentAbout.setClassName(this, OProgramie.class.getName());
	        	startActivity(intentAbout);
	        	return true;
	        case R.id.facebook:
	        	Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.FACEBOOK_URL));
	        	startActivity(browserIntent);
	        	return true;
	        case R.id.ustawienia:
	        	Intent settingsIntent = new Intent(getBaseContext(),Preferences.class);
	        	startActivity(settingsIntent);
	        	return true;	        	
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	

	// Setup TabLayout with ViewPager2 (replaced createTabs)
	public void setupTabLayoutWithViewPager(){
		tabLayout = findViewById(R.id.tabLayout);
		viewPager = findViewById(R.id.viewPager);
		
		// Initialize TabPagerAdapter
		tabPagerAdapter = new TabPagerAdapter(this);
		viewPager.setAdapter(tabPagerAdapter);
		
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
				case TabPagerAdapter.TAB_LISTA_2012:
					tab.setText(getString(R.string.tab_2012));
					break;
			}
		});
		tabLayoutMediator.attach();
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

		intent.setClass(this, UtworyWykonawcy.class);
		intent.putExtras(bun);
		final int result = 1;
		startActivityForResult(intent, result); 
	}

	
	
	@SuppressWarnings({ "unchecked" })
	public void showSongMenu(int position, String listType){
        HashMap<String, String> o = new HashMap<>();

		if (Objects.equals(listType, Constants.KEY_LISTA)) {
			o = songsList.get(position);
		}
		if (Objects.equals(listType, Constants.KEY_POCZEKALNIA)) {
			o = songsListPocz.get(position);
		}
		if (Objects.equals(listType, Constants.KEY_NOWOSCI)) {
			o = songsListNowosci.get(position);
		}
		if (Objects.equals(listType, Constants.KEY_MOJALISTA)) {
			o = songsListMojalista.get(position);
		}
		if (Objects.equals(listType, Constants.KEY_UTW_WYKONAWCY)) {
			o = (HashMap<String, String>) wykUtwory.getItemAtPosition(position);
		}
		if (Objects.equals(listType, Constants.KEY_LISTA_2012)) {
			o = notowaniaList.get(position);
		}


		final String idListy = o.get(Constants.KEY_ID);
        final String idWykonawcy = o.get(Constants.KEY_ARTIST_ID);
        final String idGrupy = o.get(Constants.KEY_ID_GRUPY);
        String title = o.get(Constants.KEY_TITLE);
		teledysk = o.get(Constants.KEY_VIDEO);
		spotify = o.get(Constants.KEY_SPOTIFY);

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
						zaglosuj(idListy, Constants.KEY_LISTA, null, idGrupy, glosTeledysk);
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(teledysk));
						startActivity(browserIntent);
					}else if (RewardItems[item] == getString(R.string.spotify)) {
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(spotify));
						startActivity(browserIntent);
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
						zaglosuj(idListy, Constants.KEY_LISTA, null, idGrupy, glosTeledysk);
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(teledysk));
						startActivity(browserIntent);
					}else if (items[item] == getString(R.string.spotify)) {
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(spotify));
						startActivity(browserIntent);
					}
				});
			}
        }

		if (Objects.equals(listType, Constants.KEY_LISTA_2012)) {
			//	finish();
			showAuthSongs(idWykonawcy);
		}

		if (Objects.equals(listType, Constants.KEY_UTW_WYKONAWCY)) {
			builder.setItems(wykItems, (dialog, item) -> {
				//Toast.makeText(getApplicationContext(), wykItems[item], Toast.LENGTH_SHORT).show();
				if(wykItems[item]==getString(R.string.zaglosuj)){
					zaglosuj(idListy, Constants.KEY_UTW_WYKONAWCY, idWykonawcy, idGrupy,"0");
				}
				else if(wykItems[item]==getString(R.string.teledysk)){
					zaglosuj(idListy, Constants.KEY_UTW_WYKONAWCY, idWykonawcy, idGrupy,"1");
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(teledysk));
					startActivity(browserIntent);
				}
				else if(wykItems[item]==getString(R.string.spotify)){
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(spotify));
					startActivity(browserIntent);
				}
			});
        }

		if (!Objects.equals(listType, Constants.KEY_LISTA_2012)) {
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
					map.put(Constants.KEY_PLACE_CHANGE, parser.getValue(e, Constants.KEY_PLACE_CHANGE) + Constants.TEXT_SEPARATOR);
					if (parser.getValue(e, Constants.KEY_PLACE_CHANGE).contains(getString(R.string.text_awans))) {
						map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_UP);
					} else if (parser.getValue(e, Constants.KEY_PLACE_CHANGE).contains(getString(R.string.text_spadek))) {
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
		
					// adding HashList to ArrayList
					songsList.add(map);
				}
	        
	        //poczekalnia
	        
	        nl = doc.getElementsByTagName(Constants.KEY_SONG_POCZ);
				//	Log.i(TAG, "nl element: " + nl );
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

				// adding HashList to ArrayList
				songsListPocz.add(map);
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
					// adding HashList to ArrayList
					songsListNowosci.add(map);
				}







				//	Log.i(TAG, "Root androidid element: " + androidId );
				//mojalista
				String xml_mojalista = parser.getXmlFromUrlMoja(Constants.URL_MOJALISTA.replace("LANG", language).replace("ANDROIDID", androidId)); // getting XML from URL

				Document docmoja = parser.getDomElementMoja(xml_mojalista); // getting DOM element
				//	Log.i(TAG, "Root moja element: " + xml_mojalista );
				//	Log.i(TAG, "docmoja element: " + docmoja );
				NodeList nlmoja = docmoja.getElementsByTagName(Constants.KEY_SONG_MOJA);
				//	Log.i(TAG, "nlmoja element: " + nlmoja );

				// looping through all song nodes <song>
				for (int i = 0; i < nlmoja.getLength(); i++) {
					// creating new HashMap
					HashMap<String, String> map = new HashMap<>();
					Element e = (Element) nlmoja.item(i);
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

					// adding HashList to ArrayList
					songsListMojalista.add(map);
					//	Log.i(TAG, "songsListMojalista element: " + songsListMojalista );
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
				// adding HashList to ArrayList
				wykonList.add(map);
				//filteredWykonList.add(map);
			}    
			//filteredWykonList=wykonList;
			filterWykonawcy(wykonList, "");
			
			xml = parser.getXmlFromUrl(Constants.URL_NOTOWANIA.replace("LANG", language).replace("START_NOTOWANIE_ID", notowanieId)); // getting XML from URL
			
			doc = parser.getDomElement(xml); // getting DOM element
			
			nlInfo = doc.getElementsByTagName(Constants.KEY_INFO);
			el = (Element) nlInfo.item(0);
			info2012 = parser.getValue(el, Constants.KEY_INFO);

			votesProgress = 0 ;
			maxVotes = 0;
			currentVotes = 0 ;
			
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
				listNotowPrzedzialy.add(map.get(Constants.KEY_NOTOWANIE_NAZWA));
			}    
			
			
			
			//lista2012
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
				//map.put(Constants.KEY_PLACE_CHANGE, parser.getValue(e, Constants.KEY_PLACE_CHANGE) + Constants.TEXT_SEPARATOR);
				//if ((parser.getValue(e, Constants.KEY_PLACE_CHANGE).indexOf(getString(R.string.text_awans)))>=0) {
				//	map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_UP);
				//} else if ((parser.getValue(e, Constants.KEY_PLACE_CHANGE).indexOf(getString(R.string.text_spadek)))>=0) {
				//	map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_DOWN);
				//} else {
					map.put(Constants.KEY_ARROW_TYPE, Constants.KEY_ARROW_NO_CHANGE);
				//}
				
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
	
				// adding HashList to ArrayList
				notowaniaList.add(map);
			}

			
			} catch (IOException e){
				connectionError = true;
			}
			
			// Update UI on main thread
			final boolean finalConnectionError = connectionError;
			mainHandler.post(() -> {
				// Update all fragment adapters with new data
				updateAllFragmentAdapters();
				isSpinnerClicked = false;
				progressDialog.dismiss();
				if (finalConnectionError) {
					new AlertDialog.Builder(ListaPrzebojowDiscoPolo.this)
					.setTitle(R.string.text_connection_error_title)
					.setMessage(getString(R.string.text_connection_error))
					.setNeutralButton("Ok",	null).show();
				}
			});
		});
	}
	
	private void voteInBackground(String url, String listType) {
		executorService.execute(() -> {
			boolean connectionError = false;
			String voteMessage = "";
			
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
				progressDialogVote.dismiss();

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
					if (Objects.equals(myListType, Constants.KEY_LISTA) || Objects.equals(myListType, Constants.KEY_POCZEKALNIA) || Objects.equals(myListType, Constants.KEY_NOWOSCI) || Objects.equals(myListType, Constants.KEY_MOJALISTA)|| Objects.equals(myListType, Constants.KEY_WYKONAWCY)) {
						if (CheckboxPreference){
							refreshListBackground();
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

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
    	
    	if (progressDialog.isShowing()){
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
		String localeDefault = lc.getLanguage();
		//String localeString = settings.getString("locale",localeDefault);
		return localeDefault;
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
        // Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager
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
							Toast.makeText(ListaPrzebojowDiscoPolo.this, "onAdFailedToLoad", Toast.LENGTH_SHORT).show();
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
					refreshListBackground();
					// [START image_view_event]
					Bundle bundlereward = new Bundle();
					bundlereward.putString(FirebaseAnalytics.Param.ITEM_ID, androidId);
					bundlereward.putString(FirebaseAnalytics.Param.ITEM_NAME, "rewardvideo");
					bundlereward.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "on_rewarded_video");
					mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundlereward);

					Bundle bundleparams = new Bundle();
					bundleparams.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "rewarded_video");
					mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundleparams);
					// [END image_view_event]


				});
	}


}	

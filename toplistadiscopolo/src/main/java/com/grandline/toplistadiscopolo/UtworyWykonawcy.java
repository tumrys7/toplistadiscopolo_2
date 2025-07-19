package com.grandline.toplistadiscopolo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
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

public class UtworyWykonawcy extends Activity {

	boolean adReward;
	AdView adView;
	AdView mAdView;
	AdView adViewNative;
	ListView wykUtwory;
	LazyAdapter adapter;
	boolean voted = false;
	Bundle bun;
	ProgressDialog progressDialog;
    ProgressDialog progressDialogVote;
    String myListType;
    String myIdWykonawcy;
    String voteMessage;
    String votingListId;
    String url;
    String language;
    String glosTeledysk;
    boolean connectionError = false;
    ArrayList<HashMap<String, String>> wykSongsList;
	// [START declare_analytics]
	private FirebaseAnalytics mFirebaseAnalytics;
	// [END declare_analytics]
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		language = getLocaleSettings();
		setContentView(R.layout.utwory_wykonawcy);
		// [START shared_app_measurement]
		// Obtain the FirebaseAnalytics instance.
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
		// [END shared_app_measurement]
		bun = getIntent().getExtras();
		assert bun != null;
		String authId = bun.getString("param_auth_id");

		refreshAuthSong(authId);
		if (!Constants.VERSION_PRO_DO_NOT_SHOW_BANNER) {
			createAd();
			createNativeAd();
		}


	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBackPressed() {
		Bundle conData = new Bundle();
		conData.putBoolean("param_return", voted);
		Intent intent = new Intent();
		intent.putExtras(conData);
		setResult(RESULT_OK, intent);

		if (adView != null) {
			adView.destroy();
		}
		super.onBackPressed();
		showAdFullscreen();
	}



	public void refreshAuthSong(String authId) {
		
		url = Constants.UTWORY_WYK_URL.replace("AUTH_ID", authId);
		wykUtwory = findViewById(R.id.wykUtwory);
		wykSongsList = new ArrayList<>();
		adapter = new LazyAdapter(this, wykSongsList);		
		wykUtwory.setOnItemClickListener((parent, view, position, id) -> showSongMenu(position, Constants.KEY_UTW_WYKONAWCY));
        progressDialog = ProgressDialog.show(UtworyWykonawcy.this, "", getString(R.string.text_auth_refresh_list));
        new RefreshAuthSongs().execute(url);		
		
	}
	
	
	@SuppressWarnings({ "unchecked" })
	public void showSongMenu(int position, String listType){
        HashMap<String, String> o = new HashMap<>();
        
        if (Objects.equals(listType, Constants.KEY_UTW_WYKONAWCY)) {
        	
			o = (HashMap<String, String>) wykUtwory.getItemAtPosition(position);
        }
        
        final String idListy = o.get(Constants.KEY_ID);
        final String idWykonawcy = o.get(Constants.KEY_ARTIST_ID);
        String title = o.get(Constants.KEY_TITLE);

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
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(teledysk));
						startActivity(browserIntent);
					} else if(wykItems[item]==getString(R.string.spotify)){
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(spotify));
						startActivity(browserIntent);
					}
				});
			}
		}
        
        AlertDialog alert = builder.create();
        alert.show();
	}

	private void zaglosuj(String idUtworu, String listType, String idWykonawcy, String teledysk) {
		if (canUserVotes(idUtworu)){
			String androidId = Settings.Secure.getString(getContentResolver(),
			         Settings.Secure.ANDROID_ID);
			String url;
			try{
				url = Constants.VOTE_URL.replace("ID_LISTY", idUtworu).replace("DEV_ID", androidId).replace("LANG", language).replace("TEL_PARAM", teledysk);
			} catch (NullPointerException e) {
				url = Constants.VOTE_URL.replace("ID_LISTY", idUtworu).replace("DEV_ID", "UNKNOWN").replace("LANG", language).replace("TEL_PARAM", teledysk);
			}
	        progressDialogVote = ProgressDialog.show(UtworyWykonawcy.this, "", getString(R.string.text_voting));
	        myListType = listType;
	        myIdWykonawcy = idWykonawcy;
	        votingListId = idUtworu;
	        new Zaglosuj().execute(url, listType);
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

	private class RefreshAuthSongs extends AsyncTask<String, Void, Integer> {

		protected Integer doInBackground(String... urls) {
			connectionError = false;

			XMLParser parser = new XMLParser();

			try {
				
				String xml = parser.getXmlFromUrl(urls[0]); // getting XML from URL

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
					// adding HashList to ArrayList
					wykSongsList.add(map);
				}
			}
			catch (IOException e){
				
				connectionError = true;
			}
			
			return 1;
        }
		
       protected void onPostExecute(Integer result) {
    	   wykUtwory.setAdapter(adapter);
    	   progressDialog.dismiss();
           if (connectionError) {
   			new AlertDialog.Builder(UtworyWykonawcy.this)
   			.setTitle(R.string.text_connection_error_title)
   			.setMessage(getString(R.string.text_connection_error))
   			.setNeutralButton("Ok",	null).show();
           }
    	   super.onPostExecute(result);

        }
        
        protected void onPreExecute() {

        }

    }
	
	
	private class Zaglosuj extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... urls) {
			connectionError = false;
			Vote vote = new Vote();
			try{
				voteMessage = vote.setVoteInUrl(urls[0]);

			} catch (IOException er){
				
				connectionError = true;
				voteMessage = getString(R.string.text_voting_error);
				
			}

			return voteMessage;
		}
		
		protected void onPostExecute(String result) {
			
    	   progressDialogVote.dismiss();

           if (connectionError) {
   			new AlertDialog.Builder(UtworyWykonawcy.this)
   			.setTitle(R.string.text_connection_error_title)
   			.setMessage(getString(R.string.text_connection_error))
   			.setNeutralButton("Ok",	null).show();
           } else {
        	setUserVote(votingListId);
        	if(Objects.equals(glosTeledysk, "0")){
			//	Snackbar snackbar = make(UtworyWykonawcy.this.getCurrentFocus(), voteMessage, Snackbar.LENGTH_LONG);
			//	snackbar.show();
        		Toast.makeText(getApplicationContext(), voteMessage, Toast.LENGTH_LONG).show();
        	}
			if (Objects.equals(myListType, Constants.KEY_UTW_WYKONAWCY)) {
				voted = true;
				if (adReward) {
					refreshAuthSong(myIdWykonawcy);
				}
			}
           }
    	   super.onPostExecute(result);
		}
        protected void onPreExecute() {

        }
		
	}
	//reklama banner przerobiona z natywnej
	public void createNativeAd(){
		adViewNative = new AdView(this);

		AdView adViewNative = findViewById(R.id.adViewNative);
		adViewNative.loadAd(new AdRequest.Builder().build());
	}

	//reklama
	public void createAd(){
		mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
	}

	private void showAdFullscreen() {
		Intent intent = new Intent();
		intent.setClass(this,AdFullscreenActivity.class);
		final int result = 1;
		startActivityForResult(intent, result);
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

	public String getLocaleSettings() {
		//SharedPreferences settings = getSharedPreferences(Constants.PREFS_NAME, 0);
		Locale lc = Locale.getDefault();

		//String localeString = settings.getString("locale",localeDefault);
		return lc.getLanguage();
	}

	
}

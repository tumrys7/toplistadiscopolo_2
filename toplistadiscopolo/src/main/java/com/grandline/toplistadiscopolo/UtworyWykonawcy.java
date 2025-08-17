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
import android.view.ViewGroup;
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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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
	
	// Spotify Bottom Sheet Controller
	private SpotifyBottomSheetController spotifyBottomSheetController;

	// ExecutorService and Handler for background tasks
	private ExecutorService executorService;
	private Handler mainHandler;

	// Activity result launcher
	private ActivityResultLauncher<Intent> activityResultLauncher;

	// [START declare_analytics]
	private FirebaseAnalytics mFirebaseAnalytics;
	// [END declare_analytics]
// ... existing code ...
}
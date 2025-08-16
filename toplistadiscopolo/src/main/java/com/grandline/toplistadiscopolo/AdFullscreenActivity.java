package com.grandline.toplistadiscopolo;


import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import static androidx.activity.EdgeToEdge.enable;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class AdFullscreenActivity extends AppCompatActivity {

    private static final String TAG = null;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_fullscreen);

        // Wywołanie metody, ustawiającej czarne ikony i przezroczyste paski systemowe
        getWindow().getDecorView().post(() -> setLightSystemBars(getWindow(), true, true));

        enable(this);

        final View root = findViewById(R.id.root);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), sysBars.top, v.getPaddingRight(), sysBars.bottom);
                return WindowInsetsCompat.CONSUMED;
            });
        }

        MobileAds.initialize(this, initializationStatus -> {});


        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this,Constants.KEY_FULLAD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                     //   Log.i(TAG, "onAdLoaded");
                        showInterstitial();
                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback(){
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when fullscreen content is dismissed.
                            //    Log.d("TAG", "The ad was dismissed.");
                                finish();
                                showLista();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                // Called when fullscreen content failed to show.
                         //       Log.d("TAG", "The ad failed to show.");
                                mInterstitialAd = null;
                                finish();
                                showLista();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when fullscreen content is shown.
                                // Make sure to set your reference to null so you don't
                                // show it a second time.
                        //        Log.d("TAG", "The ad was shown.");
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                    //    Log.i(TAG, loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });
    }

    public void setLightSystemBars(Window window, boolean lightStatusBar, boolean lightNavigationBar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
            WindowInsetsController insetsController = window.getInsetsController();
            if (insetsController != null) {
                int appearance = 0;
                int mask = 0;

                if (lightStatusBar) {
                    appearance |= WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
                    mask |= WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
                }
                if (lightNavigationBar) {
                    appearance |= WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                    mask |= WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                }

                insetsController.setSystemBarsAppearance(appearance, mask);

                // Set transparent background for system bars if needed
                window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
                window.setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
    private void showLista() {
        Intent intent = new Intent();
        intent.setClass(this,ListaPrzebojowDiscoPolo.class);
        final int result = 1;
        startActivityForResult(intent, result);
    }


    private void showInterstitial() {
            if (mInterstitialAd != null) {
                mInterstitialAd.show(AdFullscreenActivity.this);
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.");
            }
        }

    @Override
    protected void onDestroy() {
        // Clean up interstitial ad to prevent input channel issues
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(null);
            mInterstitialAd = null;
        }
        
        // Clear focus
        if (getCurrentFocus() != null) {
            getCurrentFocus().clearFocus();
        }
        
        super.onDestroy();
    }

}


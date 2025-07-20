package com.grandline.toplistadiscopolo;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;

public class OProgramie extends Activity {
    // [END declare_analytics]
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.o_programie);
        // [START shared_app_measurement]
        // Obtain the FirebaseAnalytics instance.
        // [START declare_analytics]
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // [END shared_app_measurement]
        String versionName;
        try {
			versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			versionName = "";
		}
        TextView verName= findViewById(R.id.verName);
        verName.setText(getString(R.string.wersja_programu) + " " + versionName);

        // [START info_o_autorach_aplikacji_event]
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, versionName);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, versionName);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "info_o_autorach_aplikacji");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        // [END info_o_autorach_aplikacji_event]
    }


}
